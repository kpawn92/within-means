package within.means.android.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.android.ui.CategoryView
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.categories.application.CategoriesResponse
import within.means.categories.application.search.ListAllCategoriesQuery
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.application.TransactionResponse
import within.means.transactions.application.delete.DeleteTransactionCommand
import within.means.transactions.application.toResponse
import within.means.transactions.domain.TransactionRepository
import within.means.users.application.OptionalUserResponse
import within.means.users.application.find.FindDefaultUserQuery

enum class TransactionTypeFilter { ALL, INCOME, EXPENSE, TRANSFER }

data class TransactionsListUiState(
    val items: List<TransactionResponse> = emptyList(),
    val typeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val query: String = "",
    val baseCurrency: String = "",
    val categoryNames: Map<String, String> = emptyMap(),
    val categories: Map<String, CategoryView> = emptyMap(),
    val errorMessage: String? = null,
)

class TransactionsListViewModel(
    repository: TransactionRepository,
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(TransactionsListUiState())
    val state: StateFlow<TransactionsListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll()
                .map { list -> list.map { it.toResponse() } }
                .collect { items ->
                    _state.update { it.copy(items = items) }
                }
        }
        viewModelScope.launch {
            runCatching {
                get<QueryBus>().ask<FindDefaultUserQuery, OptionalUserResponse>(FindDefaultUserQuery())
            }.onSuccess { resp ->
                resp.user?.let { u -> _state.update { it.copy(baseCurrency = u.baseCurrency) } }
            }
        }
        viewModelScope.launch {
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
    }

    fun selectTypeFilter(filter: TransactionTypeFilter) {
        _state.update { it.copy(typeFilter = filter) }
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
    }

    fun visibleItems(): List<TransactionResponse> {
        val s = _state.value
        val typeMatches: (TransactionResponse) -> Boolean = when (s.typeFilter) {
            TransactionTypeFilter.ALL -> { _ -> true }
            TransactionTypeFilter.INCOME -> { it -> it.type == "INCOME" }
            TransactionTypeFilter.EXPENSE -> { it -> it.type == "EXPENSE" }
            TransactionTypeFilter.TRANSFER -> { it -> it.type == "TRANSFER" }
        }
        val q = s.query.trim().lowercase()
        val queryMatches: (TransactionResponse) -> Boolean = { tx ->
            q.isEmpty() ||
                tx.description.lowercase().contains(q) ||
                (s.categoryNames[tx.categoryId]?.lowercase()?.contains(q) == true) ||
                (tx.incomeSource?.lowercase()?.contains(q) == true)
        }
        return s.items.filter(typeMatches).filter(queryMatches).sortedByDescending { it.date }
    }

    fun delete(transactionId: String) {
        viewModelScope.launch {
            runCatching {
                get<CommandBus>().dispatch(DeleteTransactionCommand(transactionId))
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
