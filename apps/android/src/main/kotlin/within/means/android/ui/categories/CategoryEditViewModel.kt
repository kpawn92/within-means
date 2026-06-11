package within.means.android.ui.categories

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
import within.means.categories.application.OptionalCategoryResponse
import within.means.categories.application.create.CreateCategoryCommand
import within.means.categories.application.delete.DeleteCategoryCommand
import within.means.categories.application.find.FindCategoryQuery
import within.means.categories.application.reclassify.ReclassifyCategoryCommand
import within.means.categories.application.recolor.RestyleCategoryCommand
import within.means.categories.application.rename.RenameCategoryCommand
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.application.TransactionsResponse
import within.means.transactions.application.search.SearchTransactionsQuery

data class CategoryEditUiState(
    val categoryId: String? = null,
    val name: String = "",
    val color: String = CategoryColorPalette.first(),
    val icon: String = "label",
    val kind: String = "EXPENSE",
    val nature: String? = "VARIABLE",
    val essentiality: String? = "DISCRETIONARY",
    val productive: Boolean = false,
    val engelGroup: String? = "OTHER",
    val loading: Boolean = false,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val errorMessage: String? = null,
    val isFinished: Boolean = false,
    /**
     * Whether this category already has transactions. `null` while it's still being
     * checked (or in create mode); deletion is only offered once this is `false`.
     */
    val isUsed: Boolean? = null,
)

class CategoryEditViewModel : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(CategoryEditUiState())
    val state: StateFlow<CategoryEditUiState> = _state.asStateFlow()

    val isEditMode: Boolean get() = _state.value.categoryId != null

    fun loadExisting(categoryId: String) {
        _state.update { it.copy(categoryId = categoryId, loading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                get<QueryBus>().ask<FindCategoryQuery, OptionalCategoryResponse>(
                    FindCategoryQuery(categoryId)
                ).category ?: error("Category not found")
            }.onSuccess { c ->
                _state.update {
                    it.copy(
                        loading = false,
                        name = c.name,
                        color = c.color,
                        icon = c.icon,
                        kind = c.kind,
                        nature = c.nature,
                        essentiality = c.essentiality,
                        productive = c.productive,
                        engelGroup = c.engelGroup,
                    )
                }
                checkUsage(categoryId)
            }.onFailure { e ->
                _state.update {
                    it.copy(loading = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC))
                }
            }
        }
    }

    /**
     * Cross-context usage check (app layer, not inside the KMP module): a category can
     * only be deleted while no transaction references it. Failures leave [isUsed] null,
     * so the delete action simply stays hidden rather than offering an unsafe delete.
     */
    private fun checkUsage(categoryId: String) {
        viewModelScope.launch {
            runCatching {
                get<QueryBus>().ask<SearchTransactionsQuery, TransactionsResponse>(
                    SearchTransactionsQuery(categoryId = categoryId, limit = 1)
                ).items.isNotEmpty()
            }.onSuccess { used -> _state.update { it.copy(isUsed = used) } }
        }
    }

    fun delete() {
        val id = _state.value.categoryId ?: return
        if (_state.value.isUsed != false) return
        viewModelScope.launch {
            _state.update { it.copy(deleting = true, errorMessage = null) }
            runCatching {
                get<CommandBus>().dispatch(DeleteCategoryCommand(id))
            }.onSuccess {
                _state.update { it.copy(deleting = false, isFinished = true) }
            }.onFailure { e ->
                _state.update {
                    it.copy(deleting = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC))
                }
            }
        }
    }

    fun onNameChanged(value: String) { _state.update { it.copy(name = value, errorMessage = null) } }
    fun onColorChanged(value: String) { _state.update { it.copy(color = value) } }
    fun onIconChanged(value: String) { _state.update { it.copy(icon = value) } }
    fun onKindChanged(value: String) {
        _state.update {
            // Drop EXPENSE-only classifiers when switching kind.
            if (value == "EXPENSE") {
                it.copy(kind = value)
            } else {
                it.copy(
                    kind = value,
                    nature = null,
                    essentiality = null,
                    engelGroup = null,
                    productive = false,
                )
            }
        }
    }
    fun onNatureChanged(value: String?) { _state.update { it.copy(nature = value) } }
    fun onEssentialityChanged(value: String?) { _state.update { it.copy(essentiality = value) } }
    fun onProductiveChanged(value: Boolean) { _state.update { it.copy(productive = value) } }
    fun onEngelGroupChanged(value: String?) { _state.update { it.copy(engelGroup = value) } }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(errorMessage = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            runCatching {
                val bus = get<CommandBus>()
                if (s.categoryId == null) {
                    bus.dispatch(
                        CreateCategoryCommand(
                            kind = s.kind,
                            name = s.name,
                            color = s.color,
                            icon = s.icon,
                            nature = s.nature.takeIf { s.kind == "EXPENSE" },
                            essentiality = s.essentiality.takeIf { s.kind == "EXPENSE" },
                            productive = s.productive && s.kind == "EXPENSE",
                            engelGroup = s.engelGroup.takeIf { s.kind == "EXPENSE" },
                        )
                    )
                } else {
                    bus.dispatch(RenameCategoryCommand(s.categoryId, s.name))
                    bus.dispatch(RestyleCategoryCommand(s.categoryId, s.color, s.icon))
                    if (s.kind == "EXPENSE") {
                        bus.dispatch(
                            ReclassifyCategoryCommand(
                                categoryId = s.categoryId,
                                nature = s.nature,
                                essentiality = s.essentiality,
                                productive = s.productive,
                                engelGroup = s.engelGroup,
                            )
                        )
                    }
                }
            }.onSuccess {
                _state.update { it.copy(saving = false, isFinished = true) }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = e.toUserMessage(ErrorContext.GENERIC),
                    )
                }
            }
        }
    }
}
