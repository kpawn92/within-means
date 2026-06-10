package within.means.android.ui.settings

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
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.query.QueryBus
import within.means.users.application.OptionalUserResponse
import within.means.users.application.find.FindDefaultUserQuery
import within.means.users.application.update_preferences.UpdateUserPreferencesCommand

data class SettingsUiState(
    val userId: String? = null,
    val displayName: String = "",
    val locale: String = "es",
    val baseCurrency: String = "EUR",
    /** Monthly plan as a free-text amount (major units, e.g. "1200.50"); blank = no plan. */
    val monthlyBudgetText: String = "",
    val spendingAlertsEnabled: Boolean = true,
    val monthStartDay: Int = 1,
    val hideAmounts: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val savedAck: Boolean = false,
    val errorMessage: String? = null,
)

class SettingsViewModel : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    fun onDisplayNameChanged(value: String) { _state.update { it.copy(displayName = value, savedAck = false) } }
    fun onLocaleChanged(value: String) { _state.update { it.copy(locale = value, savedAck = false) } }
    fun onBaseCurrencyChanged(value: String) { _state.update { it.copy(baseCurrency = value, savedAck = false) } }
    fun onMonthlyBudgetChanged(value: String) { _state.update { it.copy(monthlyBudgetText = value, savedAck = false) } }
    fun onSpendingAlertsChanged(value: Boolean) { _state.update { it.copy(spendingAlertsEnabled = value, savedAck = false) } }
    fun onMonthStartDayChanged(value: Int) { _state.update { it.copy(monthStartDay = value.coerceIn(1, 28), savedAck = false) } }
    fun onHideAmountsChanged(value: Boolean) { _state.update { it.copy(hideAmounts = value, savedAck = false) } }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearAck() {
        _state.update { it.copy(savedAck = false) }
    }

    fun save() {
        val s = _state.value
        val userId = s.userId ?: return
        if (s.displayName.isBlank()) {
            _state.update { it.copy(errorMessage = "El nombre no puede estar vacío") }
            return
        }
        val budgetCents = parseBudgetCents(s.monthlyBudgetText)
        if (budgetCents == null) {
            _state.update { it.copy(errorMessage = "El plan mensual no es un importe válido") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            runCatching {
                get<CommandBus>().dispatch(
                    UpdateUserPreferencesCommand(
                        userId = userId,
                        displayName = s.displayName.trim(),
                        locale = s.locale,
                        baseCurrency = s.baseCurrency,
                        monthlyBudgetCents = budgetCents,
                        spendingAlertsEnabled = s.spendingAlertsEnabled,
                        monthStartDay = s.monthStartDay,
                        hideAmounts = s.hideAmounts,
                    )
                )
            }.onSuccess {
                _state.update { it.copy(saving = false, savedAck = true) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
            }
        }
    }

    private suspend fun reload() {
        runCatching {
            get<QueryBus>().ask<FindDefaultUserQuery, OptionalUserResponse>(FindDefaultUserQuery())
        }.onSuccess { resp ->
            resp.user?.let { user ->
                _state.update {
                    it.copy(
                        loading = false,
                        userId = user.id,
                        displayName = user.displayName,
                        locale = user.locale,
                        baseCurrency = user.baseCurrency,
                        monthlyBudgetText = if (user.monthlyBudgetCents > 0L) {
                            formatBudget(user.monthlyBudgetCents)
                        } else {
                            ""
                        },
                        spendingAlertsEnabled = user.spendingAlertsEnabled,
                        monthStartDay = user.monthStartDay,
                        hideAmounts = user.hideAmounts,
                    )
                }
            } ?: _state.update { it.copy(loading = false) }
        }.onFailure { e ->
            _state.update { it.copy(loading = false, errorMessage = e.toUserMessage(ErrorContext.GENERIC)) }
        }
    }

    /** Blank → 0 (no plan). Returns null when the text isn't a valid non-negative amount. */
    private fun parseBudgetCents(input: String): Long? {
        val normalized = input.replace(',', '.').trim()
        if (normalized.isEmpty()) return 0L
        val asDouble = normalized.toDoubleOrNull() ?: return null
        if (asDouble < 0.0) return null
        return (asDouble * 100).toLong()
    }

    private fun formatBudget(cents: Long): String {
        val major = cents / 100
        val minor = (cents % 100).toInt()
        return if (minor == 0) major.toString() else "$major.${minor.toString().padStart(2, '0')}"
    }
}
