package within.means.android.ui.categories

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
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.categories.application.toResponse
import within.means.categories.application.CategoryResponse
import within.means.categories.application.delete.DeleteCategoryCommand
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.bus.command.CommandBus

enum class CategoryKindTab { EXPENSE, INCOME, TRANSFER }

data class CategoriesListUiState(
    val selectedTab: CategoryKindTab = CategoryKindTab.EXPENSE,
    val items: List<CategoryResponse> = emptyList(),
    val errorMessage: String? = null,
)

class CategoriesListViewModel(
    repository: CategoryRepository,
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(CategoriesListUiState())
    val state: StateFlow<CategoriesListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll()
                .map { list -> list.map { it.toResponse() } }
                .collect { items ->
                    _state.update { it.copy(items = items) }
                }
        }
    }

    fun selectTab(tab: CategoryKindTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun visibleItems(): List<CategoryResponse> =
        _state.value.items.filter { it.kind == _state.value.selectedTab.name }
            .sortedBy { it.name.lowercase() }

    fun delete(categoryId: String) {
        viewModelScope.launch {
            runCatching {
                get<CommandBus>().dispatch(DeleteCategoryCommand(categoryId))
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
