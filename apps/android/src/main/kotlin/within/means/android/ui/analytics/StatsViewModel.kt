package within.means.android.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.analytics.application.CategoryBreakdownResponse
import within.means.analytics.application.EvolutionGranularity
import within.means.analytics.application.MonthlyEvolutionResponse
import within.means.analytics.application.MonthlySummaryResponse
import within.means.analytics.application.find_breakdown.FindBreakdownInRangeQuery
import within.means.analytics.application.find_evolution.FindMonthlyEvolutionQuery
import within.means.analytics.application.find_summary.FindSummaryInRangeQuery
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.domain.TransactionRepository
import within.means.users.application.OptionalUserResponse
import within.means.users.application.find.FindDefaultUserQuery

enum class StatsTab { SUMMARY, BREAKDOWN, EVOLUTION }

enum class StatsPeriod { WEEK, MONTH, YEAR }

data class StatsUiState(
    val tab: StatsTab = StatsTab.SUMMARY,
    val period: StatsPeriod = StatsPeriod.MONTH,
    val periodLabel: String = "",
    val yearMonth: String = "",
    val baseCurrency: String = "",
    val summary: MonthlySummaryResponse? = null,
    /** Same-kind period immediately before [summary] — for the savings-rate trend. */
    val previousSummary: MonthlySummaryResponse? = null,
    /** "que la semana pasada" / "que el mes pasado" / "que el año pasado". */
    val comparisonLabel: String = "",
    val breakdown: CategoryBreakdownResponse? = null,
    val evolution: MonthlyEvolutionResponse? = null,
    val loading: Boolean = false,
    val errorMessage: String? = null,
)

class StatsViewModel(
    transactions: TransactionRepository,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(StatsUiState(yearMonth = currentYearMonth(clock, zone)))
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        // Re-fetch whenever transactions change. On-the-fly queries make
        // the analytics view reactive without projection tables.
        viewModelScope.launch {
            transactions.observeAll().collect { reload() }
        }
        viewModelScope.launch {
            runCatching {
                get<QueryBus>().ask<FindDefaultUserQuery, OptionalUserResponse>(FindDefaultUserQuery())
            }.onSuccess { resp ->
                resp.user?.let { u -> _state.update { it.copy(baseCurrency = u.baseCurrency) } }
            }
        }
    }

    fun selectTab(tab: StatsTab) {
        _state.update { it.copy(tab = tab) }
        viewModelScope.launch { reload() }
    }

    fun selectPeriod(period: StatsPeriod) {
        _state.update { it.copy(period = period) }
        viewModelScope.launch { reload() }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun reload() {
        _state.update { it.copy(loading = true) }
        val today = clock.now().toLocalDateTime(zone).date
        val period = _state.value.period
        val (start, end) = rangeFor(period, today)
        val (prevStart, prevEnd) = previousRangeFor(period, today)
        runCatching {
            val bus = get<QueryBus>()
            val summary = bus.ask<FindSummaryInRangeQuery, MonthlySummaryResponse>(
                FindSummaryInRangeQuery(start.toString(), end.toString())
            )
            val previous = bus.ask<FindSummaryInRangeQuery, MonthlySummaryResponse>(
                FindSummaryInRangeQuery(prevStart.toString(), prevEnd.toString())
            )
            val breakdown = bus.ask<FindBreakdownInRangeQuery, CategoryBreakdownResponse>(
                FindBreakdownInRangeQuery(start.toString(), end.toString(), type = "EXPENSE")
            )
            // Evolution buckets follow the selected period: 6 weeks / months / years.
            val evolution = bus.ask<FindMonthlyEvolutionQuery, MonthlyEvolutionResponse>(
                FindMonthlyEvolutionQuery(monthsBack = 6, granularity = evolutionGranularity(period))
            )
            Quad(summary, previous, breakdown, evolution)
        }.onSuccess { (summary, previous, breakdown, evolution) ->
            _state.update {
                it.copy(
                    summary = summary,
                    previousSummary = previous,
                    comparisonLabel = comparisonLabel(period),
                    breakdown = breakdown,
                    evolution = evolution,
                    periodLabel = periodLabel(it.period, today),
                    loading = false,
                )
            }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
        }
    }

    private fun rangeFor(period: StatsPeriod, today: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
        StatsPeriod.WEEK -> {
            val monday = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
            monday to monday.plus(DatePeriod(days = 6))
        }
        StatsPeriod.MONTH -> {
            val first = LocalDate(today.year, today.monthNumber, 1)
            first to first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        }
        StatsPeriod.YEAR -> LocalDate(today.year, 1, 1) to LocalDate(today.year, 12, 31)
    }

    /** The same-kind period immediately before the one containing [today]. */
    private fun previousRangeFor(period: StatsPeriod, today: LocalDate): Pair<LocalDate, LocalDate> {
        val shifted = when (period) {
            StatsPeriod.WEEK -> today.minus(DatePeriod(days = 7))
            StatsPeriod.MONTH -> today.minus(DatePeriod(months = 1))
            StatsPeriod.YEAR -> today.minus(DatePeriod(years = 1))
        }
        return rangeFor(period, shifted)
    }

    private fun comparisonLabel(period: StatsPeriod): String = when (period) {
        StatsPeriod.WEEK -> "que la semana pasada"
        StatsPeriod.MONTH -> "que el mes pasado"
        StatsPeriod.YEAR -> "que el año pasado"
    }

    private fun evolutionGranularity(period: StatsPeriod): EvolutionGranularity = when (period) {
        StatsPeriod.WEEK -> EvolutionGranularity.WEEK
        StatsPeriod.MONTH -> EvolutionGranularity.MONTH
        StatsPeriod.YEAR -> EvolutionGranularity.YEAR
    }

    private fun periodLabel(period: StatsPeriod, today: LocalDate): String = when (period) {
        StatsPeriod.WEEK -> "Esta semana"
        StatsPeriod.MONTH -> MONTHS[today.monthNumber - 1]
        StatsPeriod.YEAR -> today.year.toString()
    }

    private companion object {
        val MONTHS = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
        )
    }
}

private fun currentYearMonth(clock: Clock, zone: TimeZone): String {
    val today = clock.now().toLocalDateTime(zone).date
    return "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
}

/** Four-value tuple — the period summaries plus breakdown and evolution. */
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
