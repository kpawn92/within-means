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
import within.means.categories.application.search.SearchCategoriesQuery
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.application.recurring.DeactivateRecurringRuleCommand
import within.means.transactions.application.recurring.ListActiveRecurringRulesQuery
import within.means.transactions.application.recurring.RecurringRulesResponse
import within.means.users.application.OptionalUserResponse
import within.means.users.application.find.FindDefaultUserQuery

/** One row of the recurring-rules manager, already resolved for display. */
data class RecurringRuleRow(
    val id: String,
    val type: String,
    val amountCents: Long,
    val categoryName: String,
    val description: String,
    val frequency: String,
    val nextOccurrence: String,
)

data class RecurringRulesUiState(
    val rules: List<RecurringRuleRow> = emptyList(),
    val baseCurrency: String = "EUR",
    val loading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * Lists the active recurring rules and lets the user deactivate them.
 * Deactivation is soft (already-materialized transactions stay); the
 * list refreshes from the query after each command.
 */
class RecurringRulesViewModel : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(RecurringRulesUiState())
    val state: StateFlow<RecurringRulesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    fun deactivate(id: String) {
        viewModelScope.launch {
            runCatching {
                get<CommandBus>().dispatch(DeactivateRecurringRuleCommand(id))
            }.onSuccess {
                reload()
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    fun clearError() { _state.update { it.copy(errorMessage = null) } }

    private suspend fun reload() {
        _state.update { it.copy(loading = true) }
        runCatching {
            val currency = get<QueryBus>()
                .ask<FindDefaultUserQuery, OptionalUserResponse>(FindDefaultUserQuery())
                .user?.baseCurrency ?: "EUR"
            val names = get<QueryBus>()
                .ask<SearchCategoriesQuery, CategoriesResponse>(SearchCategoriesQuery())
                .items.associate { it.id to it.name }
            val rules = get<QueryBus>()
                .ask<ListActiveRecurringRulesQuery, RecurringRulesResponse>(ListActiveRecurringRulesQuery())
                .items
            currency to rules.map { r ->
                RecurringRuleRow(
                    id = r.id,
                    type = r.type,
                    amountCents = r.amountCents,
                    categoryName = names[r.categoryId] ?: "Categoría",
                    description = r.description,
                    frequency = r.frequency,
                    nextOccurrence = r.nextOccurrence,
                )
            }
        }.onSuccess { (currency, rows) ->
            _state.update { it.copy(loading = false, baseCurrency = currency, rules = rows) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
        }
    }
}
