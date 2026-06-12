package within.means.android.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import within.means.android.persistence.DatabaseUnlocker
import within.means.android.persistence.PinPolicy
import within.means.android.ui.error.ErrorContext
import within.means.android.ui.error.toUserMessage

data class UnlockUiState(
    val pin: String = "",
    val errorMessage: String? = null,
    val isWorking: Boolean = false,
)

class UnlockViewModel(
    private val unlocker: DatabaseUnlocker,
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockUiState())
    val state: StateFlow<UnlockUiState> = _state.asStateFlow()

    fun updatePin(value: String) {
        if (value.length <= PIN_LENGTH && value.all { it.isDigit() }) {
            _state.update { it.copy(pin = value, errorMessage = null) }
        }
    }

    fun submit(onUnlocked: () -> Unit) {
        val current = _state.value
        if (current.pin.length != PIN_LENGTH) {
            _state.update { it.copy(errorMessage = "El PIN debe tener $PIN_LENGTH dígitos") }
            return
        }
        unlockWith(current.pin, onUnlocked)
    }

    /**
     * Unlocks with a PIN obtained out-of-band (e.g. recovered from the biometric
     * vault). Shares the wrong-PIN detection with [submit]: SQLCipher only fails
     * at the first real query, so we force-touch the DB.
     */
    fun unlockWith(pin: String, onUnlocked: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, errorMessage = null) }
            runCatching {
                unlocker.unlock(pin)
                unlocker.users.userProfileQueries.findDefault().executeAsOneOrNull()
            }.onSuccess {
                _state.update { it.copy(isWorking = false) }
                onUnlocked()
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isWorking = false,
                        pin = "",
                        errorMessage = e.toUserMessage(ErrorContext.UNLOCK_DATABASE),
                    )
                }
            }
        }
    }

    companion object {
        const val PIN_LENGTH = PinPolicy.LENGTH
    }
}
