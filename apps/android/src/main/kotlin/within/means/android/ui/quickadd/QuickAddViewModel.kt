package within.means.android.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.android.ui.calculator.AmountCalculator
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.categories.application.CategoriesResponse
import within.means.categories.application.CategoryResponse
import within.means.categories.application.search.SearchCategoriesQuery
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.application.register.RegisterTransactionCommand

/** When the movement happened — QuickAdd keeps it to one tap. */
enum class QuickWhen { TODAY, YESTERDAY }

data class QuickAddUiState(
    val type: String = "EXPENSE",
    /** Canonical calculator expression, e.g. "12.5" or "12+3"; empty means 0. */
    val expression: String = "",
    /** Live result of [expression] in cents; 0 when empty/invalid. */
    val amountCents: Long = 0L,
    val categoryId: String? = null,
    val note: String = "",
    val whenChoice: QuickWhen = QuickWhen.TODAY,
    val availableCategories: List<CategoryResponse> = emptyList(),
    val baseCurrency: String = "",
    val saving: Boolean = false,
    val errorMessage: String? = null,
    val savedAmountCents: Long? = null,
) {
    val canSave: Boolean get() = amountCents > 0L && !categoryId.isNullOrBlank() && !saving
}

class QuickAddViewModel(
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(QuickAddUiState())
    val state: StateFlow<QuickAddUiState> = _state.asStateFlow()

    private val calc = AmountCalculator()

    init {
        viewModelScope.launch { loadCategoriesForCurrentType() }
        viewModelScope.launch { loadCurrency() }
    }

    fun onTypeChanged(value: String) {
        _state.update { it.copy(type = value, categoryId = null) }
        viewModelScope.launch { loadCategoriesForCurrentType() }
    }

    fun onCategoryChanged(value: String) { _state.update { it.copy(categoryId = value, errorMessage = null) } }
    fun onNoteChanged(value: String) { _state.update { it.copy(note = value) } }
    fun onWhenChanged(value: QuickWhen) { _state.update { it.copy(whenChoice = value) } }

    /** Keypad now drives a calculator: digits, decimals and `+ − × ÷`. */
    fun onDigit(d: Char) { calc.onDigit(d); syncAmount() }
    fun onDot() { calc.onDot(); syncAmount() }
    fun onOperator(op: Char) { calc.onOperator(op); syncAmount() }
    fun onBackspace() { calc.onBackspace(); syncAmount() }

    private fun syncAmount() {
        _state.update {
            it.copy(expression = calc.expression, amountCents = calc.resultCents() ?: 0L, errorMessage = null)
        }
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        // Validate with explicit feedback (like the full editor) instead of a
        // silently-disabled button: tell the user what's missing.
        if (s.amountCents <= 0L) {
            _state.update { it.copy(errorMessage = "Escribe un importe mayor que 0") }
            return
        }
        val categoryId = s.categoryId
        if (categoryId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Selecciona una categoría") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            runCatching {
                get<CommandBus>().dispatch(
                    RegisterTransactionCommand(
                        type = s.type,
                        amountCents = s.amountCents,
                        date = resolveDate(s.whenChoice),
                        description = s.note,
                        categoryId = categoryId,
                    )
                )
            }.onSuccess {
                _state.update { it.copy(saving = false, savedAmountCents = s.amountCents) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    fun clearError() { _state.update { it.copy(errorMessage = null) } }

    /** Clears transient input so a reused instance starts fresh on reopen. */
    fun reset() {
        calc.clear()
        _state.update { QuickAddUiState(baseCurrency = it.baseCurrency) }
        viewModelScope.launch { loadCategoriesForCurrentType() }
    }

    private fun resolveDate(choice: QuickWhen): String {
        val today = clock.now().toLocalDateTime(zone).date
        val date = when (choice) {
            QuickWhen.TODAY -> today
            QuickWhen.YESTERDAY -> today.minus(DatePeriod(days = 1))
        }
        return date.toString()
    }

    private suspend fun loadCategoriesForCurrentType() {
        runCatching {
            get<QueryBus>().ask<SearchCategoriesQuery, CategoriesResponse>(
                SearchCategoriesQuery(kind = _state.value.type)
            )
        }.onSuccess { resp -> _state.update { it.copy(availableCategories = resp.items) } }
    }

    private suspend fun loadCurrency() {
        runCatching {
            get<QueryBus>().ask<within.means.users.application.find.FindDefaultUserQuery,
                within.means.users.application.OptionalUserResponse>(
                within.means.users.application.find.FindDefaultUserQuery()
            )
        }.onSuccess { resp ->
            resp.user?.let { u -> _state.update { it.copy(baseCurrency = u.baseCurrency) } }
        }
    }
}
