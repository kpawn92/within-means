package within.means.android.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.categories.application.CategoriesResponse
import within.means.categories.application.CategoryResponse
import within.means.categories.application.search.SearchCategoriesQuery
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.application.OptionalTransactionResponse
import within.means.transactions.application.edit.EditTransactionCommand
import within.means.transactions.application.find.FindTransactionQuery
import within.means.transactions.application.recurring.CreateRecurringRuleCommand
import within.means.transactions.application.recurring.ListActiveRecurringRulesQuery
import within.means.transactions.application.recurring.RecurringRulesResponse
import within.means.transactions.application.register.RegisterTransactionCommand

data class TransactionEditUiState(
    val transactionId: String? = null,
    val type: String = "EXPENSE",
    val amountText: String = "",
    val date: String = todayIso(),
    val description: String = "",
    val categoryId: String? = null,
    val incomeSource: String = "",
    val availableCategories: List<CategoryResponse> = emptyList(),
    val recurring: Boolean = false,
    val frequency: String = "MONTHLY",
    val activeRecurringCount: Int = 0,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val errorMessage: String? = null,
    val isFinished: Boolean = false,
)

class TransactionEditViewModel : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(TransactionEditUiState())
    val state: StateFlow<TransactionEditUiState> = _state.asStateFlow()

    val isEditMode: Boolean get() = _state.value.transactionId != null

    init {
        viewModelScope.launch { loadCategoriesForCurrentType() }
        viewModelScope.launch { loadActiveRecurringCount() }
    }

    private suspend fun loadActiveRecurringCount() {
        runCatching {
            get<QueryBus>().ask<ListActiveRecurringRulesQuery, RecurringRulesResponse>(
                ListActiveRecurringRulesQuery()
            )
        }.onSuccess { resp ->
            _state.update { it.copy(activeRecurringCount = resp.items.size) }
        }
    }

    fun loadExisting(transactionId: String) {
        _state.update { it.copy(transactionId = transactionId, loading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                get<QueryBus>().ask<FindTransactionQuery, OptionalTransactionResponse>(
                    FindTransactionQuery(transactionId)
                ).transaction ?: error("Transaction not found")
            }.onSuccess { t ->
                _state.update {
                    it.copy(
                        loading = false,
                        type = t.type,
                        amountText = (t.amountCents / 100.0).toString(),
                        date = t.date,
                        description = t.description,
                        categoryId = t.categoryId,
                        incomeSource = t.incomeSource.orEmpty(),
                    )
                }
                loadCategoriesForCurrentType()
            }.onFailure { e ->
                _state.update { it.copy(loading = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    fun onTypeChanged(value: String) {
        _state.update {
            it.copy(
                type = value,
                categoryId = null,
                incomeSource = if (value == "INCOME") it.incomeSource else "",
            )
        }
        viewModelScope.launch { loadCategoriesForCurrentType() }
    }

    fun onAmountTextChanged(value: String) { _state.update { it.copy(amountText = value) } }
    fun onDateChanged(value: String) { _state.update { it.copy(date = value) } }
    fun onDescriptionChanged(value: String) { _state.update { it.copy(description = value) } }
    fun onCategoryChanged(value: String) { _state.update { it.copy(categoryId = value) } }
    fun onIncomeSourceChanged(value: String) { _state.update { it.copy(incomeSource = value) } }
    fun onRecurringChanged(value: Boolean) { _state.update { it.copy(recurring = value) } }
    fun onFrequencyChanged(value: String) { _state.update { it.copy(frequency = value) } }

    fun save() {
        val s = _state.value
        val cents = parseCents(s.amountText)
        if (cents == null || cents <= 0L) {
            _state.update { it.copy(errorMessage = "El monto debe ser mayor que 0") }
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
                val bus = get<CommandBus>()
                val incomeSource = s.incomeSource.takeIf { it.isNotBlank() && s.type == "INCOME" }
                if (s.transactionId == null && s.recurring) {
                    // Recurring is create-only: the rule materializes its own
                    // occurrences (including the first one due today).
                    bus.dispatch(
                        CreateRecurringRuleCommand(
                            type = s.type,
                            amountCents = cents,
                            categoryId = categoryId,
                            description = s.description,
                            incomeSource = incomeSource,
                            frequency = s.frequency,
                            startDate = s.date,
                        )
                    )
                } else if (s.transactionId == null) {
                    bus.dispatch(
                        RegisterTransactionCommand(
                            type = s.type,
                            amountCents = cents,
                            date = s.date,
                            description = s.description,
                            categoryId = categoryId,
                            incomeSource = incomeSource,
                        )
                    )
                } else {
                    bus.dispatch(
                        EditTransactionCommand(
                            transactionId = s.transactionId,
                            amountCents = cents,
                            date = s.date,
                            description = s.description,
                            categoryId = categoryId,
                            incomeSource = incomeSource,
                        )
                    )
                }
            }.onSuccess {
                _state.update { it.copy(saving = false, isFinished = true) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    private suspend fun loadCategoriesForCurrentType() {
        val type = _state.value.type
        runCatching {
            get<QueryBus>().ask<SearchCategoriesQuery, CategoriesResponse>(SearchCategoriesQuery(kind = type))
        }.onSuccess { resp ->
            _state.update { it.copy(availableCategories = resp.items) }
        }
    }

    private fun parseCents(input: String): Long? {
        val normalized = input.replace(',', '.').trim()
        if (normalized.isEmpty()) return null
        val asDouble = normalized.toDoubleOrNull() ?: return null
        return (asDouble * 100).toLong()
    }
}

private fun todayIso(): String =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
