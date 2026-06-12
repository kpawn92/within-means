package within.means.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import within.means.android.persistence.BiometricVault
import within.means.android.persistence.DatabaseUnlocker
import within.means.android.persistence.PinPolicy

data class ChangePinUiState(
    val currentPin: String = "",
    val newPin: String = "",
    val confirmPin: String = "",
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val done: Boolean = false,
)

/**
 * Changes the app PIN, which re-keys the encrypted database (see
 * [DatabaseUnlocker.changePin]). A wrong current PIN fails the re-key, which we
 * surface without touching the data.
 */
class ChangePinViewModel(
    private val unlocker: DatabaseUnlocker,
    private val biometricVault: BiometricVault,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePinUiState())
    val state: StateFlow<ChangePinUiState> = _state.asStateFlow()

    fun onCurrentPinChanged(value: String) { if (value.isPin()) _state.update { it.copy(currentPin = value, errorMessage = null) } }
    fun onNewPinChanged(value: String) { if (value.isPin()) _state.update { it.copy(newPin = value, errorMessage = null) } }
    fun onConfirmPinChanged(value: String) { if (value.isPin()) _state.update { it.copy(confirmPin = value, errorMessage = null) } }

    private fun String.isPin() = length <= PinPolicy.LENGTH && all { it.isDigit() }

    fun clearError() { _state.update { it.copy(errorMessage = null) } }

    fun submit() {
        val s = _state.value
        when {
            s.currentPin.length != PinPolicy.LENGTH || s.newPin.length != PinPolicy.LENGTH ->
                _state.update { it.copy(errorMessage = "El PIN debe tener ${PinPolicy.LENGTH} dígitos") }
            s.newPin != s.confirmPin ->
                _state.update { it.copy(errorMessage = "Los PIN nuevos no coinciden") }
            s.newPin == s.currentPin ->
                _state.update { it.copy(errorMessage = "El nuevo PIN debe ser distinto" ) }
            else -> viewModelScope.launch {
                _state.update { it.copy(isWorking = true, errorMessage = null) }
                runCatching {
                    withContext(Dispatchers.IO) { unlocker.changePin(s.currentPin, s.newPin) }
                }.onSuccess {
                    // The wrapped PIN is now stale; drop biometric so it can't
                    // unlock with the old PIN. User re-enables it from Settings.
                    biometricVault.clear()
                    _state.update { it.copy(isWorking = false, done = true) }
                }.onFailure {
                    // The re-key validates the current PIN by touching the file;
                    // a failure here is almost always a wrong current PIN.
                    _state.update {
                        it.copy(isWorking = false, currentPin = "",
                            errorMessage = "No se pudo cambiar el PIN. ¿El PIN actual es correcto?")
                    }
                }
            }
        }
    }
}
