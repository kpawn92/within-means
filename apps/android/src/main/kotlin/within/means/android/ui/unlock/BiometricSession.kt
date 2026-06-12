package within.means.android.ui.unlock

import android.content.Context
import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import within.means.android.persistence.BiometricGate
import within.means.android.persistence.BiometricVault

/** Outcome of a biometric unlock attempt, routed back to the PIN flow. */
sealed interface BiometricUnlockResult {
    /** PIN recovered from the vault — unlock the database with it. */
    data class Recovered(val pin: String) : BiometricUnlockResult

    /** User dismissed the sheet or chose "Usar PIN"; show the keypad. */
    data object Cancelled : BiometricUnlockResult

    /** Biometrics changed since enrollment; the vault was cleared. Use the PIN. */
    data object Invalidated : BiometricUnlockResult

    /** Lockout or hardware error; show the keypad and surface [message]. */
    data class Failed(val message: String) : BiometricUnlockResult
}

/**
 * Bridges the Compose layer to [BiometricGate]/[BiometricVault]. The gate needs
 * the foreground activity, so this is created per screen from [LocalContext] and
 * not held as a Koin singleton. Guards every Keystore call behind the API-30
 * floor; on older devices [unlock]/[enroll] resolve as no-ops.
 */
class BiometricSession internal constructor(
    private val context: Context,
    private val vault: BiometricVault,
    private val scope: CoroutineScope,
) {

    val isEnrolled: Boolean
        get() = supported && vault.isEnrolled

    private val supported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Prompts for a fingerprint and recovers the wrapped PIN. */
    fun unlock(onResult: (BiometricUnlockResult) -> Unit) {
        if (!isEnrolled) {
            onResult(BiometricUnlockResult.Cancelled)
            return
        }
        unlockApi30(onResult)
    }

    /** Wraps [pin] behind the biometric so future launches can unlock with it. */
    fun enroll(pin: String, onResult: (success: Boolean, message: String?) -> Unit) {
        if (!supported) {
            onResult(false, null)
            return
        }
        enrollApi30(pin, onResult)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun unlockApi30(onResult: (BiometricUnlockResult) -> Unit) {
        val cipher = try {
            vault.decryptCipher()
        } catch (_: KeyPermanentlyInvalidatedException) {
            vault.clear()
            onResult(BiometricUnlockResult.Invalidated)
            return
        } catch (_: Exception) {
            // Unreadable vault (e.g. key gone): reset and fall back to PIN.
            vault.clear()
            onResult(BiometricUnlockResult.Invalidated)
            return
        }
        scope.launch {
            when (val r = BiometricGate(context).authenticate(TITLE, UNLOCK_SUBTITLE, cipher)) {
                is BiometricGate.Result.Success -> {
                    val pin = runCatching { vault.retrievePin(r.cipher) }.getOrNull()
                    if (pin != null) {
                        onResult(BiometricUnlockResult.Recovered(pin))
                    } else {
                        vault.clear()
                        onResult(BiometricUnlockResult.Invalidated)
                    }
                }
                BiometricGate.Result.Cancelled -> onResult(BiometricUnlockResult.Cancelled)
                is BiometricGate.Result.Failed -> onResult(BiometricUnlockResult.Failed(r.message))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun enrollApi30(pin: String, onResult: (Boolean, String?) -> Unit) {
        val cipher = try {
            vault.encryptCipher()
        } catch (e: Exception) {
            onResult(false, e.message)
            return
        }
        scope.launch {
            when (val r = BiometricGate(context).authenticate(ENROLL_TITLE, ENROLL_SUBTITLE, cipher)) {
                is BiometricGate.Result.Success ->
                    runCatching { vault.store(pin, r.cipher) }
                        .onSuccess { onResult(true, null) }
                        .onFailure { onResult(false, it.message) }
                BiometricGate.Result.Cancelled -> onResult(false, null)
                is BiometricGate.Result.Failed -> onResult(false, r.message)
            }
        }
    }

    private companion object {
        const val TITLE = "Within Means"
        const val UNLOCK_SUBTITLE = "Desbloquea con tu huella"
        const val ENROLL_TITLE = "Activar desbloqueo con huella"
        const val ENROLL_SUBTITLE = "Confirma tu huella para activarlo"
    }
}

@Composable
fun rememberBiometricSession(): BiometricSession {
    val context = LocalContext.current
    val vault: BiometricVault = koinInject()
    val scope = rememberCoroutineScope()
    return remember(context) { BiometricSession(context, vault, scope) }
}
