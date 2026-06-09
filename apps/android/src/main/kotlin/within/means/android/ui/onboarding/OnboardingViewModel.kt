package within.means.android.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.android.persistence.DatabaseUnlocker
import within.means.android.persistence.OnboardingState
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.query.QueryBus
import within.means.users.application.OptionalUserResponse
import within.means.users.application.ensure_default.EnsureDefaultUserCommand
import within.means.users.application.find.FindDefaultUserQuery
import within.means.users.application.update_preferences.UpdateUserPreferencesCommand

enum class OnboardingStep { Welcome, Pin, Preferences, Done }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val pin: String = "",
    val pinConfirm: String = "",
    val displayName: String = "Yo",
    val locale: String = "es",
    val baseCurrency: String = "EUR",
    val errorMessage: String? = null,
    val isWorking: Boolean = false,
)

/**
 * Buses are resolved lazily via [KoinComponent.get] AFTER calling
 * [DatabaseUnlocker.unlock]. Building the CommandBus pulls in handlers
 * that depend on the unlocked databases, so eager injection would fail.
 *
 * Type-erasure note: we DO NOT inject the buses as `() -> CommandBus` /
 * `() -> QueryBus` because both collapse to `Function0<Any>` at runtime
 * and Koin can't tell them apart.
 */
class OnboardingViewModel(
    private val unlocker: DatabaseUnlocker,
    private val onboardingState: OnboardingState,
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun goToPin() {
        _state.update { it.copy(step = OnboardingStep.Pin, errorMessage = null) }
    }

    fun updatePin(value: String) {
        if (value.length <= PIN_LENGTH && value.all { it.isDigit() }) {
            _state.update { it.copy(pin = value, errorMessage = null) }
        }
    }

    fun updatePinConfirm(value: String) {
        if (value.length <= PIN_LENGTH && value.all { it.isDigit() }) {
            _state.update { it.copy(pinConfirm = value, errorMessage = null) }
        }
    }

    fun submitPin() {
        val s = _state.value
        when {
            s.pin.length != PIN_LENGTH ->
                _state.update { it.copy(errorMessage = "El PIN debe tener $PIN_LENGTH dígitos") }
            s.pin != s.pinConfirm ->
                _state.update { it.copy(errorMessage = "Los PIN no coinciden") }
            else -> {
                viewModelScope.launch {
                    _state.update { it.copy(isWorking = true, errorMessage = null) }
                    runCatching {
                        unlocker.unlock(s.pin)
                        // Resolve AFTER unlock so handlers bind to live DBs.
                        // Default-category seeding fires from the
                        // UserDefaultCreated subscriber wired in apps/android.
                        get<CommandBus>().dispatch(EnsureDefaultUserCommand())
                    }.onSuccess {
                        _state.update { it.copy(step = OnboardingStep.Preferences, isWorking = false) }
                    }.onFailure { e ->
                        _state.update {
                            it.copy(
                                isWorking = false,
                                errorMessage = e.toUserMessage(ErrorContext.UNLOCK_DATABASE),
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateDisplayName(value: String) {
        _state.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun updateLocale(code: String) {
        _state.update { it.copy(locale = code) }
    }

    fun updateBaseCurrency(code: String) {
        _state.update { it.copy(baseCurrency = code) }
    }

    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (s.displayName.isBlank()) {
            _state.update { it.copy(errorMessage = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, errorMessage = null) }
            runCatching {
                val queryBus = get<QueryBus>()
                val commandBus = get<CommandBus>()

                val current = queryBus
                    .ask<FindDefaultUserQuery, OptionalUserResponse>(FindDefaultUserQuery())
                    .user
                    ?: error("default user missing after onboarding")

                commandBus.dispatch(
                    UpdateUserPreferencesCommand(
                        userId = current.id,
                        displayName = s.displayName,
                        locale = s.locale,
                        baseCurrency = s.baseCurrency,
                        monthlyBudgetCents = current.monthlyBudgetCents,
                        spendingAlertsEnabled = current.spendingAlertsEnabled,
                    )
                )
                onboardingState.isCompleted = true
            }.onSuccess {
                _state.update { it.copy(step = OnboardingStep.Done, isWorking = false) }
                onDone()
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isWorking = false,
                        errorMessage = e.toUserMessage(ErrorContext.UPDATE_PREFERENCES),
                    )
                }
            }
        }
    }

    companion object {
        const val PIN_LENGTH = 6
    }
}
