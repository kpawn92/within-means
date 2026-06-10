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
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, errorMessage = null) }
            runCatching {
                unlocker.unlock(current.pin)
                // Force-touch the database to surface a wrong-pin error
                // (SQLCipher only fails at the first real query).
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
