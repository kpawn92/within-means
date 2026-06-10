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
        val (start, end) = rangeFor(_state.value.period, today)
        runCatching {
            val bus = get<QueryBus>()
            val summary = bus.ask<FindSummaryInRangeQuery, MonthlySummaryResponse>(
                FindSummaryInRangeQuery(start.toString(), end.toString())
            )
            val breakdown = bus.ask<FindBreakdownInRangeQuery, CategoryBreakdownResponse>(
                FindBreakdownInRangeQuery(start.toString(), end.toString(), type = "EXPENSE")
            )
            // Evolution stays month-based (the 6-month bar trend is period-agnostic).
            val evolution = bus.ask<FindMonthlyEvolutionQuery, MonthlyEvolutionResponse>(
                FindMonthlyEvolutionQuery(monthsBack = 6)
            )
            Triple(summary, breakdown, evolution)
        }.onSuccess { (summary, breakdown, evolution) ->
            _state.update {
                it.copy(
                    summary = summary,
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
