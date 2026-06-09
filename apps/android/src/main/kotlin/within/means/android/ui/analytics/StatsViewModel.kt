package within.means.android.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.analytics.application.CategoryBreakdownResponse
import within.means.analytics.application.MonthlyEvolutionResponse
import within.means.analytics.application.MonthlySummaryResponse
import within.means.analytics.application.find_breakdown.FindCategoryBreakdownQuery
import within.means.analytics.application.find_evolution.FindMonthlyEvolutionQuery
import within.means.analytics.application.find_summary.FindMonthlySummaryQuery
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.domain.TransactionRepository
import within.means.users.application.OptionalUserResponse
import within.means.users.application.find.FindDefaultUserQuery

enum class StatsTab { SUMMARY, BREAKDOWN, EVOLUTION }

data class StatsUiState(
    val tab: StatsTab = StatsTab.SUMMARY,
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
    clock: Clock = Clock.System,
    zone: TimeZone = TimeZone.currentSystemDefault(),
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

    fun selectYearMonth(value: String) {
        _state.update { it.copy(yearMonth = value) }
        viewModelScope.launch { reload() }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun reload() {
        _state.update { it.copy(loading = true) }
        val ym = _state.value.yearMonth
        runCatching {
            val bus = get<QueryBus>()
            val summary = bus.ask<FindMonthlySummaryQuery, MonthlySummaryResponse>(
                FindMonthlySummaryQuery(ym)
            )
            val breakdown = bus.ask<FindCategoryBreakdownQuery, CategoryBreakdownResponse>(
                FindCategoryBreakdownQuery(yearMonth = ym, type = "EXPENSE")
            )
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
                    loading = false,
                )
            }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
        }
    }
}

private fun currentYearMonth(clock: Clock, zone: TimeZone): String {
    val today = clock.now().toLocalDateTime(zone).date
    return "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
}
