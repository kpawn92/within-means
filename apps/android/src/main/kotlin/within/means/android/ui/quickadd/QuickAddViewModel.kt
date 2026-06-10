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
    /** Raw keypad buffer, e.g. "12.5"; empty means 0. */
    val amountRaw: String = "",
    val categoryId: String? = null,
    val note: String = "",
    val whenChoice: QuickWhen = QuickWhen.TODAY,
    val availableCategories: List<CategoryResponse> = emptyList(),
    val baseCurrency: String = "",
    val saving: Boolean = false,
    val errorMessage: String? = null,
    val savedAmountCents: Long? = null,
) {
    val amountCents: Long
        get() {
            if (amountRaw.isEmpty()) return 0L
            val parts = amountRaw.split('.')
            val whole = parts[0].toLongOrNull() ?: 0L
            val frac = parts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toLongOrNull() ?: 0L
            return whole * 100 + frac
        }

    val canSave: Boolean get() = amountCents > 0L && !categoryId.isNullOrBlank() && !saving
}

class QuickAddViewModel(
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(QuickAddUiState())
    val state: StateFlow<QuickAddUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { loadCategoriesForCurrentType() }
        viewModelScope.launch { loadCurrency() }
    }

    fun onTypeChanged(value: String) {
        _state.update { it.copy(type = value, categoryId = null) }
        viewModelScope.launch { loadCategoriesForCurrentType() }
    }

    fun onCategoryChanged(value: String) { _state.update { it.copy(categoryId = value) } }
    fun onNoteChanged(value: String) { _state.update { it.copy(note = value) } }
    fun onWhenChanged(value: QuickWhen) { _state.update { it.copy(whenChoice = value) } }

    /** Appends a digit, enforcing ≤9 integer digits and ≤2 decimals. */
    fun onDigit(d: Char) {
        _state.update { s ->
            val raw = s.amountRaw
            val parts = raw.split('.')
            if (parts.size == 2) {
                if (parts[1].length >= 2) return@update s
            } else {
                // integer part
                if (raw.length >= 9) return@update s
                if (raw == "0") return@update s.copy(amountRaw = d.toString())
            }
            s.copy(amountRaw = raw + d)
        }
    }

    fun onDot() {
        _state.update { s ->
            if (s.amountRaw.contains('.')) return@update s
            val base = s.amountRaw.ifEmpty { "0" }
            s.copy(amountRaw = "$base.")
        }
    }

    fun onBackspace() {
        _state.update { s -> s.copy(amountRaw = s.amountRaw.dropLast(1)) }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        val categoryId = s.categoryId ?: return
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
