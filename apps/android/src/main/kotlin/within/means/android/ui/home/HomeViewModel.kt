package within.means.android.ui.home

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
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.analytics.application.MonthlySummaryResponse
import within.means.analytics.application.CategoryBreakdownResponse
import within.means.analytics.application.find_breakdown.FindCategoryBreakdownQuery
import within.means.analytics.application.find_summary.FindCurrentMonthSummaryQuery
import within.means.analytics.application.find_summary.FindSummaryInRangeQuery
import within.means.android.ui.CategoryView
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.categories.application.CategoriesResponse
import within.means.categories.application.search.ListAllCategoriesQuery
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.application.TransactionResponse
import within.means.transactions.application.TransactionsResponse
import within.means.transactions.application.recurring.RecurringTransactionsMaterializer
import within.means.transactions.application.search.SearchTransactionsQuery
import within.means.transactions.domain.TransactionRepository
import within.means.users.application.OptionalUserResponse
import within.means.users.application.find.FindDefaultUserQuery

/**
 * Snapshot of the monthly spending plan, shown in the Home "Disponible" hero.
 * Only present when the user has set a plan (`planCents > 0`).
 */
data class BudgetView(
    val planCents: Long,
    val spentCents: Long,
    val availableCents: Long,
    val perDayCents: Long,
    val daysRemaining: Int,
    val withinPlan: Boolean,
)

data class HomeUiState(
    val displayName: String = "...",
    val baseCurrency: String = "",
    val summary: MonthlySummaryResponse? = null,
    val breakdown: CategoryBreakdownResponse? = null,
    val recentTransactions: List<TransactionResponse> = emptyList(),
    val categoryNames: Map<String, String> = emptyMap(),
    val categories: Map<String, CategoryView> = emptyMap(),
    val monthlyBudgetCents: Long = 0L,
    val monthStartDay: Int = 1,
    val hideAmounts: Boolean = false,
    val budget: BudgetView? = null,
    val loading: Boolean = true,
    val errorMessage: String? = null,
)

class HomeViewModel(
    transactions: TransactionRepository,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // Catch up any due recurring transactions on entry; observeAll below
        // picks up whatever this materializes and refreshes the screen.
        viewModelScope.launch {
            runCatching { get<RecurringTransactionsMaterializer>().materializeAllActive() }
        }
        viewModelScope.launch { loadProfile() }
        viewModelScope.launch { loadCategoryNames() }
        // React to any transaction change: refresh summary + recent list.
        viewModelScope.launch {
            transactions.observeAll().collect { reloadTransactions() }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private suspend fun loadProfile() {
        runCatching {
            get<QueryBus>().ask<FindDefaultUserQuery, OptionalUserResponse>(FindDefaultUserQuery())
        }.onSuccess { resp ->
            resp.user?.let { user ->
                _state.update {
                    it.copy(
                        displayName = user.displayName,
                        baseCurrency = user.baseCurrency,
                        monthlyBudgetCents = user.monthlyBudgetCents,
                        monthStartDay = user.monthStartDay,
                        hideAmounts = user.hideAmounts,
                    )
                }
                recomputeBudget()
            }
        }.onFailure { e ->
            _state.update { it.copy(errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
        }
    }

    private suspend fun loadCategoryNames() {
        runCatching {
            get<QueryBus>().ask<ListAllCategoriesQuery, CategoriesResponse>(ListAllCategoriesQuery())
        }.onSuccess { resp ->
            _state.update {
                it.copy(
                    categoryNames = resp.items.associate { c -> c.id to c.name },
                    categories = resp.items.associate { c -> c.id to CategoryView(c.name, c.icon, c.color) },
                )
            }
        }
    }

    private suspend fun reloadTransactions() {
        _state.update { it.copy(loading = true) }
        runCatching {
            val bus = get<QueryBus>()
            val summary = bus.ask<FindCurrentMonthSummaryQuery, MonthlySummaryResponse>(
                FindCurrentMonthSummaryQuery()
            )
            val breakdown = bus.ask<FindCategoryBreakdownQuery, CategoryBreakdownResponse>(
                FindCategoryBreakdownQuery(yearMonth = summary.yearMonth)
            )
            val recent = bus.ask<SearchTransactionsQuery, TransactionsResponse>(
                SearchTransactionsQuery(limit = 5)
            )
            Triple(summary, breakdown, recent)
        }.onSuccess { (summary, breakdown, recent) ->
            _state.update {
                it.copy(
                    summary = summary,
                    breakdown = breakdown,
                    recentTransactions = recent.items,
                    loading = false,
                )
            }
            recomputeBudget()
        }.onFailure { e ->
            _state.update { it.copy(loading = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
        }
    }

    /**
     * Derives the "Disponible" hero from the user's plan and the budget
     * cycle's expenses: available = plan − spent, daily pace = available ÷
     * days left in the cycle. The cycle runs from [HomeUiState.monthStartDay]
     * to the day before the next occurrence of that day. When the start day is
     * 1 the cycle is the calendar month and we reuse the month summary;
     * otherwise we query the exact cycle range. Pure presentation arithmetic —
     * keeps :analytics free of a :users dep.
     */
    private suspend fun recomputeBudget() {
        val s = _state.value
        val plan = s.monthlyBudgetCents
        if (plan <= 0L) {
            _state.update { it.copy(budget = null) }
            return
        }
        val today = clock.now().toLocalDateTime(zone).date
        val (cycleStart, cycleEnd) = budgetCycle(today, s.monthStartDay)

        val spent = if (s.monthStartDay == 1) {
            s.summary?.totalExpenseCents ?: 0L
        } else {
            runCatching {
                get<QueryBus>().ask<FindSummaryInRangeQuery, MonthlySummaryResponse>(
                    FindSummaryInRangeQuery(cycleStart.toString(), cycleEnd.toString())
                ).totalExpenseCents
            }.getOrDefault(s.summary?.totalExpenseCents ?: 0L)
        }

        val available = plan - spent
        val days = cycleEnd.toEpochDays() - today.toEpochDays() + 1
        val perDay = if (available > 0L && days > 0) available / days else 0L
        _state.update {
            it.copy(
                budget = BudgetView(
                    planCents = plan,
                    spentCents = spent,
                    availableCents = available,
                    perDayCents = perDay,
                    daysRemaining = days.toInt(),
                    withinPlan = available >= 0L,
                ),
            )
        }
    }

    /**
     * The [start, end] inclusive dates of the budget cycle containing [today]
     * for a cycle that resets on [startDay] (1..28). For startDay 1 this is
     * the calendar month.
     */
    private fun budgetCycle(today: LocalDate, startDay: Int): Pair<LocalDate, LocalDate> {
        val day = startDay.coerceIn(1, 28)
        val cycleStart = if (today.dayOfMonth >= day) {
            LocalDate(today.year, today.monthNumber, day)
        } else {
            LocalDate(today.year, today.monthNumber, day).minus(DatePeriod(months = 1))
        }
        val cycleEnd = cycleStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        return cycleStart to cycleEnd
    }
}

