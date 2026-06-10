package within.means.android.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.categories.application.CategoriesResponse
import within.means.categories.application.CategoryResponse
import within.means.categories.application.search.SearchCategoriesQuery
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.application.recurring.ListActiveRecurringRulesQuery
import within.means.transactions.application.recurring.RecurringRulesResponse
import within.means.transactions.application.recurring.UpdateRecurringRuleCommand

data class RecurringEditUiState(
    val ruleId: String? = null,
    val type: String = "EXPENSE",
    val amountText: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val frequency: String = "MONTHLY",
    val availableCategories: List<CategoryResponse> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val errorMessage: String? = null,
    val isFinished: Boolean = false,
)

/**
 * Loads an active recurring rule by id (reusing the list query — only active
 * rules are editable) and applies an [UpdateRecurringRuleCommand]. Type and
 * start date are fixed; amount / category / description / frequency are editable.
 */
class RecurringEditViewModel : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(RecurringEditUiState())
    val state: StateFlow<RecurringEditUiState> = _state.asStateFlow()

    fun load(ruleId: String) {
        _state.update { it.copy(ruleId = ruleId, loading = true) }
        viewModelScope.launch {
            runCatching {
                get<QueryBus>().ask<ListActiveRecurringRulesQuery, RecurringRulesResponse>(
                    ListActiveRecurringRulesQuery()
                ).items.firstOrNull { it.id == ruleId } ?: error("Regla no encontrada")
            }.onSuccess { r ->
                _state.update {
                    it.copy(
                        loading = false,
                        type = r.type,
                        amountText = (r.amountCents / 100.0).toString(),
                        description = r.description,
                        categoryId = r.categoryId,
                        frequency = r.frequency,
                    )
                }
                loadCategories(r.type)
            }.onFailure { e ->
                _state.update { it.copy(loading = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    fun onAmountTextChanged(value: String) { _state.update { it.copy(amountText = value) } }
    fun onDescriptionChanged(value: String) { _state.update { it.copy(description = value) } }
    fun onCategoryChanged(value: String) { _state.update { it.copy(categoryId = value) } }
    fun onFrequencyChanged(value: String) { _state.update { it.copy(frequency = value) } }

    fun clearError() { _state.update { it.copy(errorMessage = null) } }

    fun save() {
        val s = _state.value
        val ruleId = s.ruleId ?: return
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
                get<CommandBus>().dispatch(
                    UpdateRecurringRuleCommand(
                        id = ruleId,
                        amountCents = cents,
                        categoryId = categoryId,
                        description = s.description,
                        frequency = s.frequency,
                    )
                )
            }.onSuccess {
                _state.update { it.copy(saving = false, isFinished = true) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    private suspend fun loadCategories(type: String) {
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
