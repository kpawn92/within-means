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
import within.means.android.capture.MovementCaptureService
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.categories.application.CategoriesResponse
import within.means.categories.application.CategoryResponse
import within.means.categories.application.search.SearchCategoriesQuery
import within.means.concepts.application.ConceptsResponse
import within.means.concepts.application.OptionalConceptResponse
import within.means.concepts.application.find.FindConceptQuery
import within.means.concepts.application.recategorize.SetConceptDefaultCategoryCommand
import within.means.concepts.application.suggest.ListAllConceptsQuery
import within.means.concepts.domain.ConceptKey
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.query.QueryBus
import within.means.transactions.application.OptionalTransactionResponse
import within.means.transactions.application.delete.DeleteTransactionCommand
import within.means.transactions.application.edit.EditTransactionCommand
import within.means.transactions.application.find.FindTransactionQuery
import within.means.users.application.OptionalUserResponse
import within.means.users.application.find.FindDefaultUserQuery
import within.means.transactions.application.recurring.CreateRecurringRuleCommand
import within.means.transactions.application.recurring.ListActiveRecurringRulesQuery
import within.means.transactions.application.recurring.RecurringRulesResponse

/** A concept the user can tap to attach to the movement. [id] is null until resolved. */
data class ConceptChipUi(val id: String?, val label: String)

/** A known (already-saved) concept and the category it currently infers. */
data class ConceptUi(val id: String, val label: String, val defaultCategoryId: String?)

/** Offer to re-point a concept's inferred category after the user overrode it (§8.4). */
data class RelearnPrompt(
    val conceptId: String,
    val conceptLabel: String,
    val categoryId: String,
    val categoryName: String,
)

data class TransactionEditUiState(
    val transactionId: String? = null,
    val type: String = "EXPENSE",
    val amountText: String = "",
    val date: String = todayIso(),
    /** ISO local time `HH:mm`; blank means no time-of-day. Defaults to now on create. */
    val time: String = nowTimeIso(),
    val description: String = "",
    val categoryId: String? = null,
    val incomeSource: String = "",
    val availableCategories: List<CategoryResponse> = emptyList(),
    /** Concepts the user picked/typed ("en qué fue"); order = relevance. */
    val selectedConcepts: List<String> = emptyList(),
    /** Frequent concepts of the current type, most-used first (chip row). */
    val conceptSuggestions: List<ConceptChipUi> = emptyList(),
    /** All known concepts of the current type, keyed by normalized key. */
    val knownConcepts: Map<String, ConceptUi> = emptyMap(),
    /** Pending "re-learn this concept's category?" offer, or null. */
    val relearnPrompt: RelearnPrompt? = null,
    /** Free text of the "¿En qué?" field, not yet committed to a chip. */
    val conceptInput: String = "",
    /** Whether the advanced "Detalles" section (category, date, recurring…) is open. */
    val showDetails: Boolean = false,
    val recurring: Boolean = false,
    val frequency: String = "MONTHLY",
    val activeRecurringCount: Int = 0,
    val baseCurrency: String = "",
    val loading: Boolean = false,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val errorMessage: String? = null,
    val isFinished: Boolean = false,
) {
    /** Concepts apply to spend/income only; transfers carry none (CONCEPTS-SPEC §10-C). */
    val conceptsApply: Boolean get() = type == "EXPENSE" || type == "INCOME"
    /** Parsed amount in cents for display; 0 when blank/invalid. */
    val amountCents: Long
        get() {
            val n = amountText.replace(',', '.').trim().toDoubleOrNull() ?: return 0L
            return (n * 100).toLong()
        }
}

class TransactionEditViewModel : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(TransactionEditUiState())
    val state: StateFlow<TransactionEditUiState> = _state.asStateFlow()

    val isEditMode: Boolean get() = _state.value.transactionId != null

    init {
        viewModelScope.launch { loadCategoriesForCurrentType() }
        viewModelScope.launch { loadConceptSuggestions() }
        viewModelScope.launch { loadActiveRecurringCount() }
        viewModelScope.launch { loadCurrency() }
    }

    private suspend fun loadCurrency() {
        runCatching {
            get<QueryBus>().ask<FindDefaultUserQuery, OptionalUserResponse>(FindDefaultUserQuery())
        }.onSuccess { resp ->
            resp.user?.let { u -> _state.update { it.copy(baseCurrency = u.baseCurrency) } }
        }
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
                val conceptLabels = resolveConceptLabels(t.conceptIds)
                _state.update {
                    it.copy(
                        loading = false,
                        type = t.type,
                        amountText = (t.amountCents / 100.0).toString(),
                        date = t.date,
                        time = t.time.orEmpty(),
                        description = t.description,
                        categoryId = t.categoryId,
                        incomeSource = t.incomeSource.orEmpty(),
                        selectedConcepts = conceptLabels,
                        // Editing reveals the full form (category, date, recurring…).
                        showDetails = true,
                    )
                }
                loadCategoriesForCurrentType()
                loadConceptSuggestions()
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
                // Concepts are per-kind: switching type drops the picked ones.
                selectedConcepts = emptyList(),
                conceptSuggestions = emptyList(),
                knownConcepts = emptyMap(),
                relearnPrompt = null,
                conceptInput = "",
            )
        }
        viewModelScope.launch { loadCategoriesForCurrentType() }
        viewModelScope.launch { loadConceptSuggestions() }
    }

    fun onAmountTextChanged(value: String) { _state.update { it.copy(amountText = value) } }
    fun onDateChanged(value: String) { _state.update { it.copy(date = value) } }
    fun onTimeChanged(value: String) { _state.update { it.copy(time = value) } }
    fun onDescriptionChanged(value: String) { _state.update { it.copy(description = value) } }
    fun onCategoryChanged(value: String) {
        _state.update { it.copy(categoryId = value, relearnPrompt = relearnPromptFor(it, value)) }
    }
    fun onIncomeSourceChanged(value: String) { _state.update { it.copy(incomeSource = value) } }
    fun onRecurringChanged(value: Boolean) { _state.update { it.copy(recurring = value) } }
    fun onFrequencyChanged(value: String) { _state.update { it.copy(frequency = value) } }
    fun onToggleDetails() { _state.update { it.copy(showDetails = !it.showDetails) } }

    /**
     * Applies a deep-link preset (from a widget / app shortcut): preselect the
     * type and, optionally, a concept. Create mode only — never rewrites an edit.
     */
    fun preset(type: String?, concept: String?) {
        if (isEditMode) return
        type?.takeIf { it in TYPES && it != _state.value.type }?.let { onTypeChanged(it) }
        concept?.trim()?.takeIf { it.isNotEmpty() }?.let { label ->
            _state.update { s ->
                if (s.selectedConcepts.any { it.equals(label, ignoreCase = true) }) s
                else s.copy(selectedConcepts = s.selectedConcepts + label)
            }
        }
    }

    fun onConceptInputChanged(value: String) { _state.update { it.copy(conceptInput = value) } }

    /** Commits the typed "¿En qué?" text as a selected concept (deduped, case-insensitive). */
    fun onCommitTypedConcept() {
        val raw = _state.value.conceptInput.trim()
        if (raw.isEmpty()) return
        _state.update { s ->
            val already = s.selectedConcepts.any { it.equals(raw, ignoreCase = true) }
            s.copy(
                selectedConcepts = if (already) s.selectedConcepts else s.selectedConcepts + raw,
                conceptInput = "",
                relearnPrompt = null,
            )
        }
    }

    /** Taps a suggestion chip or a selected chip: toggles its membership. */
    fun onToggleConcept(label: String) {
        _state.update { s ->
            val present = s.selectedConcepts.any { it.equals(label, ignoreCase = true) }
            s.copy(
                selectedConcepts = if (present) {
                    s.selectedConcepts.filterNot { it.equals(label, ignoreCase = true) }
                } else {
                    s.selectedConcepts + label
                },
                relearnPrompt = null,
            )
        }
    }

    fun onRemoveConcept(label: String) {
        _state.update { s ->
            s.copy(
                selectedConcepts = s.selectedConcepts.filterNot { it.equals(label, ignoreCase = true) },
                relearnPrompt = null,
            )
        }
    }

    fun save() {
        val s = _state.value
        val cents = parseCents(s.amountText)
        if (cents == null || cents <= 0L) {
            _state.update { it.copy(errorMessage = "El monto debe ser mayor que 0") }
            return
        }
        // No category requirement: it's inferred from the first concept, or falls
        // back to "Otros". The user never has to pick one (CONCEPTS-SPEC D0.3/§10-B).

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            runCatching {
                val capture = get<MovementCaptureService>()
                val labels = if (s.conceptsApply) s.selectedConcepts else emptyList()
                val incomeSource = s.incomeSource.takeIf { it.isNotBlank() && s.type == "INCOME" }

                when {
                    s.transactionId == null && s.recurring -> {
                        // Recurring is create-only and doesn't carry concepts in the
                        // MVP; still infer the category so it's never asked for.
                        val conceptIds = capture.resolveConceptIds(s.type, labels)
                        val categoryId = capture.inferCategoryId(s.type, conceptIds, s.categoryId)
                        get<CommandBus>().dispatch(
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
                    }

                    s.transactionId == null -> {
                        capture.register(
                            type = s.type,
                            amountCents = cents,
                            date = s.date,
                            time = s.time.ifBlank { null },
                            description = s.description,
                            conceptLabels = labels,
                            categoryOverride = s.categoryId,
                            incomeSource = incomeSource,
                        )
                    }

                    else -> {
                        val conceptIds = capture.resolveConceptIds(s.type, labels)
                        val categoryId = capture.inferCategoryId(s.type, conceptIds, s.categoryId)
                        get<CommandBus>().dispatch(
                            EditTransactionCommand(
                                transactionId = s.transactionId,
                                amountCents = cents,
                                date = s.date,
                                time = s.time.ifBlank { null },
                                description = s.description,
                                categoryId = categoryId,
                                incomeSource = incomeSource,
                                conceptIds = conceptIds,
                            )
                        )
                    }
                }
            }.onSuccess {
                _state.update { it.copy(saving = false, isFinished = true) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    /**
     * Turns the loaded movement into a fresh draft (create mode): keeps type,
     * amount, category, description and source, but drops the id, sets the date to
     * today and clears the recurring flag. The user reviews and taps "Añadir" to
     * save it as a new, independent copy.
     */
    fun duplicate() {
        if (_state.value.transactionId == null) return
        _state.update {
            it.copy(
                transactionId = null,
                date = todayIso(),
                recurring = false,
                isFinished = false,
                errorMessage = null,
            )
        }
    }

    fun delete() {
        val id = _state.value.transactionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(deleting = true, errorMessage = null) }
            runCatching {
                get<CommandBus>().dispatch(DeleteTransactionCommand(transactionId = id))
            }.onSuccess {
                _state.update { it.copy(deleting = false, isFinished = true) }
            }.onFailure { e ->
                _state.update { it.copy(deleting = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
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

    /**
     * Loads the concepts of the current type: the most-used ones feed the chip
     * row, and the full set (keyed by normalized key) backs the re-learn check.
     */
    private suspend fun loadConceptSuggestions() {
        val type = _state.value.type
        if (type != "EXPENSE" && type != "INCOME") {
            _state.update { it.copy(conceptSuggestions = emptyList(), knownConcepts = emptyMap()) }
            return
        }
        runCatching {
            get<QueryBus>().ask<ListAllConceptsQuery, ConceptsResponse>(ListAllConceptsQuery(kind = type))
        }.onSuccess { resp ->
            _state.update { s ->
                s.copy(
                    conceptSuggestions = resp.items.take(CHIP_LIMIT).map { ConceptChipUi(id = it.id, label = it.label) },
                    knownConcepts = resp.items.associate { c ->
                        c.key to ConceptUi(id = c.id, label = c.label, defaultCategoryId = c.defaultCategoryId)
                    },
                )
            }
        }
    }

    /**
     * After the user overrides the category, check whether the first selected
     * concept already maps somewhere else — if so, offer to re-point it (§8.4).
     * Only fires for **existing** concepts (a brand-new one has nothing to relearn).
     */
    private fun relearnPromptFor(s: TransactionEditUiState, categoryId: String): RelearnPrompt? {
        if (!s.conceptsApply) return null
        val firstLabel = s.selectedConcepts.firstOrNull() ?: return null
        val key = runCatching { ConceptKey.of(firstLabel).value }.getOrNull() ?: return null
        val concept = s.knownConcepts[key] ?: return null
        if (concept.defaultCategoryId == categoryId) return null
        val categoryName = s.availableCategories.firstOrNull { it.id == categoryId }?.name ?: return null
        return RelearnPrompt(
            conceptId = concept.id,
            conceptLabel = concept.label,
            categoryId = categoryId,
            categoryName = categoryName,
        )
    }

    /** Accepts the offer: re-points the concept's default category from now on. */
    fun applyRelearn() {
        val p = _state.value.relearnPrompt ?: return
        viewModelScope.launch {
            runCatching {
                get<CommandBus>().dispatch(SetConceptDefaultCategoryCommand(p.conceptId, p.categoryId))
            }
            _state.update { s ->
                val key = runCatching { ConceptKey.of(p.conceptLabel).value }.getOrNull()
                val known = if (key != null && s.knownConcepts.containsKey(key)) {
                    s.knownConcepts + (key to s.knownConcepts.getValue(key).copy(defaultCategoryId = p.categoryId))
                } else {
                    s.knownConcepts
                }
                s.copy(relearnPrompt = null, knownConcepts = known)
            }
        }
    }

    fun dismissRelearn() { _state.update { it.copy(relearnPrompt = null) } }

    /** Turns the saved concept ids of an edited movement back into their labels. */
    private suspend fun resolveConceptLabels(conceptIds: List<String>): List<String> =
        conceptIds.mapNotNull { id ->
            runCatching {
                get<QueryBus>().ask<FindConceptQuery, OptionalConceptResponse>(FindConceptQuery(id))
            }.getOrNull()?.concept?.label
        }

    private fun parseCents(input: String): Long? {
        val normalized = input.replace(',', '.').trim()
        if (normalized.isEmpty()) return null
        val asDouble = normalized.toDoubleOrNull() ?: return null
        return (asDouble * 100).toLong()
    }
}

/** How many concept chips the QuickAdd row shows before the rest stay searchable. */
private const val CHIP_LIMIT = 12

/** Valid movement types a deep-link may preselect. */
private val TYPES = setOf("EXPENSE", "INCOME", "TRANSFER")

private fun todayIso(): String =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

/** Current local time as `HH:mm` (seconds dropped). */
private fun nowTimeIso(): String {
    val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    val hh = t.hour.toString().padStart(2, '0')
    val mm = t.minute.toString().padStart(2, '0')
    return "$hh:$mm"
}
