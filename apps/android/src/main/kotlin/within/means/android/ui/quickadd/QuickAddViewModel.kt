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
import kotlin.math.roundToLong
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.android.capture.MovementCaptureService
import within.means.android.ui.calculator.AmountCalculator
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.android.ui.transactions.ConceptChipUi
import within.means.categories.application.CategoriesResponse
import within.means.categories.application.CategoryResponse
import within.means.categories.application.search.SearchCategoriesQuery
import within.means.concepts.application.ConceptsResponse
import within.means.concepts.application.suggest.SuggestConceptsQuery
import within.means.shared.domain.bus.query.QueryBus

/** When the movement happened — QuickAdd keeps it to one tap. */
enum class QuickWhen { TODAY, YESTERDAY }

/** Single movement (calculator) vs. emptying a basket as a list (CONCEPTS-SPEC §4.4). */
enum class QuickAddMode { SINGLE, LIST }

/** One parsed row of the basket: concept + amount, with the inferred category preview. */
data class BatchLineUi(
    val id: Long,
    val label: String,
    val amountCents: Long,
    /** Inferred category name (best-effort preview); null while resolving. */
    val categoryName: String? = null,
)

data class QuickAddUiState(
    val type: String = "EXPENSE",
    /** Canonical calculator expression, e.g. "12.5" or "12+3"; empty means 0. */
    val expression: String = "",
    /** Live result of [expression] in cents; 0 when empty/invalid. */
    val amountCents: Long = 0L,
    val categoryId: String? = null,
    /** Concepts the user picked/typed ("en qué fue"); order = relevance. */
    val selectedConcepts: List<String> = emptyList(),
    /** Frequent concepts of the current type, most-used first (chip row). */
    val conceptSuggestions: List<ConceptChipUi> = emptyList(),
    /** Free text of the "¿En qué?" field, not yet committed to a chip. */
    val conceptInput: String = "",
    val note: String = "",
    /** Resolved movement date (ISO `yyyy-MM-dd`); blank until [reset] seeds today. */
    val date: String = "",
    /** Movement time (ISO `HH:mm`); blank until [reset] seeds now. */
    val time: String = "",
    /** Which quick chip is lit, or null when a custom date was picked. */
    val whenChoice: QuickWhen? = QuickWhen.TODAY,
    val availableCategories: List<CategoryResponse> = emptyList(),
    val baseCurrency: String = "",
    val saving: Boolean = false,
    val errorMessage: String? = null,
    val savedAmountCents: Long? = null,
    /** SINGLE = calculator; LIST = "vaciar la cesta" (batch, §4.4). */
    val mode: QuickAddMode = QuickAddMode.SINGLE,
    /** Free text of the batch field ("concepto monto"), not yet committed to a row. */
    val batchInput: String = "",
    /** Rows added in list mode; each becomes its own movement sharing one batchRef. */
    val batchLines: List<BatchLineUi> = emptyList(),
) {
    // No category requirement: it's inferred from the concept / falls back to "Otros".
    val canSave: Boolean get() = amountCents > 0L && !saving

    /** Concepts apply to spend/income only; transfers carry none. */
    val conceptsApply: Boolean get() = type == "EXPENSE" || type == "INCOME"

    /** Running total of the basket. */
    val batchTotalCents: Long get() = batchLines.sumOf { it.amountCents }
    val canSaveBatch: Boolean get() = batchLines.isNotEmpty() && !saving
}

class QuickAddViewModel(
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(QuickAddUiState())
    val state: StateFlow<QuickAddUiState> = _state.asStateFlow()

    private val calc = AmountCalculator()
    private var nextLineId = 0L

    private companion object {
        /** `<concepto> <monto>` — captures the trailing number (`90`, `90.5`, `90,50`). */
        val BATCH_LINE = Regex("""^(.*\S)\s+(\d+(?:[.,]\d{1,2})?)$""")
    }

    init {
        viewModelScope.launch { loadCategoriesForCurrentType() }
        viewModelScope.launch { loadConceptSuggestions() }
        viewModelScope.launch { loadCurrency() }
    }

    fun onTypeChanged(value: String) {
        _state.update {
            it.copy(
                type = value,
                categoryId = null,
                // Concepts (and inferred categories) are per-kind: switching type
                // drops the picked concepts and any basket built for the old kind.
                selectedConcepts = emptyList(),
                conceptSuggestions = emptyList(),
                conceptInput = "",
                batchInput = "",
                batchLines = emptyList(),
            )
        }
        viewModelScope.launch { loadCategoriesForCurrentType() }
        viewModelScope.launch { loadConceptSuggestions() }
    }

    fun onCategoryChanged(value: String) { _state.update { it.copy(categoryId = value, errorMessage = null) } }
    fun onNoteChanged(value: String) { _state.update { it.copy(note = value) } }

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
            )
        }
    }

    fun onToggleConcept(label: String) {
        _state.update { s ->
            val present = s.selectedConcepts.any { it.equals(label, ignoreCase = true) }
            s.copy(
                selectedConcepts = if (present) {
                    s.selectedConcepts.filterNot { it.equals(label, ignoreCase = true) }
                } else {
                    s.selectedConcepts + label
                },
            )
        }
    }

    fun onRemoveConcept(label: String) {
        _state.update { s ->
            s.copy(selectedConcepts = s.selectedConcepts.filterNot { it.equals(label, ignoreCase = true) })
        }
    }
    fun onWhenChanged(value: QuickWhen) {
        _state.update { it.copy(whenChoice = value, date = resolveDate(value)) }
    }
    /** Custom date from the picker; lights no quick chip. */
    fun onDateChanged(value: String) { _state.update { it.copy(date = value, whenChoice = null) } }
    fun onTimeChanged(value: String) { _state.update { it.copy(time = value) } }

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

    // ---- List mode ("vaciar la cesta", §4.4) ------------------------------

    fun onModeChanged(mode: QuickAddMode) { _state.update { it.copy(mode = mode, errorMessage = null) } }

    fun onBatchInputChanged(value: String) { _state.update { it.copy(batchInput = value, errorMessage = null) } }

    fun onRemoveBatchLine(id: Long) {
        _state.update { s -> s.copy(batchLines = s.batchLines.filterNot { it.id == id }) }
    }

    /**
     * Parses the batch field ("concepto monto", e.g. `carro ruta1 a ruta2 78`),
     * appends a row and clears the input — Enter keeps adding without closing the
     * keyboard. The inferred category preview resolves in the background.
     */
    fun onCommitBatchLine() {
        val parsed = parseBatchLine(_state.value.batchInput)
        if (parsed == null) {
            _state.update { it.copy(errorMessage = "Escribe concepto y monto, p. ej. \"pan 15\"") }
            return
        }
        val (label, cents) = parsed
        val id = nextLineId++
        _state.update { s ->
            s.copy(
                batchLines = s.batchLines + BatchLineUi(id = id, label = label, amountCents = cents),
                batchInput = "",
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            val name = if (label.isBlank()) "Otros" else {
                val catId = runCatching {
                    get<MovementCaptureService>().previewCategoryId(_state.value.type, label)
                }.getOrNull()
                catId?.let { cid -> _state.value.availableCategories.firstOrNull { it.id == cid }?.name } ?: "Otros"
            }
            _state.update { s ->
                s.copy(batchLines = s.batchLines.map { if (it.id == id) it.copy(categoryName = name) else it })
            }
        }
    }

    fun saveBatch() {
        val s = _state.value
        if (s.saving) return
        if (s.batchLines.isEmpty()) {
            _state.update { it.copy(errorMessage = "Añade al menos una línea") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            runCatching {
                get<MovementCaptureService>().registerBatch(
                    type = s.type,
                    date = s.date.ifBlank { resolveDate(QuickWhen.TODAY) },
                    time = s.time.ifBlank { null },
                    lines = s.batchLines.map { line ->
                        MovementCaptureService.Line(
                            amountCents = line.amountCents,
                            conceptLabels = if (line.label.isNotBlank()) listOf(line.label) else emptyList(),
                        )
                    },
                )
            }.onSuccess {
                _state.update { it.copy(saving = false, savedAmountCents = s.batchTotalCents) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    /** "carro ruta1 a ruta2 78" → ("carro ruta1 a ruta2", 7800). Trailing number is the amount. */
    private fun parseBatchLine(raw: String): Pair<String, Long>? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        val m = BATCH_LINE.matchEntire(s)
        val (label, amountStr) = when {
            m != null -> m.groupValues[1].trim() to m.groupValues[2]
            // A bare number (no concept) is allowed — it lands in "Otros".
            s.all { it.isDigit() || it == '.' || it == ',' } -> "" to s
            else -> return null
        }
        val cents = amountToCents(amountStr) ?: return null
        if (cents <= 0L) return null
        return label to cents
    }

    private fun amountToCents(s: String): Long? =
        s.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }

    fun save() {
        val s = _state.value
        if (s.saving) return
        // Only the amount is required now: the category is inferred from the
        // concept (or falls back to "Otros"), so it never blocks a save.
        if (s.amountCents <= 0L) {
            _state.update { it.copy(errorMessage = "Escribe un importe mayor que 0") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            runCatching {
                get<MovementCaptureService>().register(
                    type = s.type,
                    amountCents = s.amountCents,
                    date = s.date.ifBlank { resolveDate(QuickWhen.TODAY) },
                    time = s.time.ifBlank { null },
                    description = s.note,
                    conceptLabels = if (s.conceptsApply) s.selectedConcepts else emptyList(),
                    categoryOverride = s.categoryId,
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
        _state.update {
            QuickAddUiState(
                baseCurrency = it.baseCurrency,
                date = resolveDate(QuickWhen.TODAY),
                time = nowTime(),
            )
        }
        viewModelScope.launch { loadCategoriesForCurrentType() }
        viewModelScope.launch { loadConceptSuggestions() }
    }

    private fun resolveDate(choice: QuickWhen): String {
        val today = clock.now().toLocalDateTime(zone).date
        val date = when (choice) {
            QuickWhen.TODAY -> today
            QuickWhen.YESTERDAY -> today.minus(DatePeriod(days = 1))
        }
        return date.toString()
    }

    /** Current local time as `HH:mm`. */
    private fun nowTime(): String {
        val t = clock.now().toLocalDateTime(zone).time
        return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
    }

    private suspend fun loadCategoriesForCurrentType() {
        runCatching {
            get<QueryBus>().ask<SearchCategoriesQuery, CategoriesResponse>(
                SearchCategoriesQuery(kind = _state.value.type)
            )
        }.onSuccess { resp -> _state.update { it.copy(availableCategories = resp.items) } }
    }

    /** Most-used concepts of the current type, for the QuickAdd chip row. */
    private suspend fun loadConceptSuggestions() {
        val type = _state.value.type
        if (type != "EXPENSE" && type != "INCOME") {
            _state.update { it.copy(conceptSuggestions = emptyList()) }
            return
        }
        runCatching {
            get<QueryBus>().ask<SuggestConceptsQuery, ConceptsResponse>(SuggestConceptsQuery(kind = type))
        }.onSuccess { resp ->
            _state.update { s ->
                s.copy(conceptSuggestions = resp.items.map { ConceptChipUi(id = it.id, label = it.label) })
            }
        }
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
