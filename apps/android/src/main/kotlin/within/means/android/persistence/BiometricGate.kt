package within.means.android.persistence

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Drives the platform [BiometricPrompt] (no third-party library) and resumes a
 * coroutine with the authenticated [Cipher]. Built per screen from the current
 * [Context] — it needs the foreground activity to show the system sheet, so it
 * is not a Koin singleton.
 */
@RequiresApi(Build.VERSION_CODES.R)
class BiometricGate(private val context: Context) {

    sealed interface Result {
        /** The user authenticated; [cipher] is unlocked for one crypto operation. */
        data class Success(val cipher: Cipher) : Result

        /** The user dismissed the sheet or tapped "Usar PIN". */
        data object Cancelled : Result

        /** A non-recoverable error (lockout, no hardware, key invalidated…). */
        data class Failed(val code: Int, val message: String) : Result
    }

    /**
     * Shows the biometric sheet bound to [cipher] via a [BiometricPrompt.CryptoObject].
     * Suspends until the user authenticates, cancels, or an error occurs.
     */
    suspend fun authenticate(title: String, subtitle: String, cipher: Cipher): Result =
        suspendCancellableCoroutine { cont ->
            val cancellation = CancellationSignal()
            cont.invokeOnCancellation { cancellation.cancel() }

            val prompt = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                // Strong-biometric-only lets us keep an explicit fallback button to
                // the PIN keypad (device-credential auth would forbid it). Tapping it
                // resumes as Cancelled here; the matching error callback is deduped by
                // the isActive guard.
                .setNegativeButton(NEGATIVE_LABEL, context.mainExecutor) { _, _ ->
                    if (cont.isActive) cont.resume(Result.Cancelled)
                }
                .build()

            prompt.authenticate(
                BiometricPrompt.CryptoObject(cipher),
                cancellation,
                context.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (!cont.isActive) return
                        val authenticated = result.cryptoObject?.cipher
                        cont.resume(
                            if (authenticated != null) Result.Success(authenticated)
                            else Result.Failed(-1, "Cipher no autenticado"),
                        )
                    }

                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        if (!cont.isActive) return
                        cont.resume(
                            if (isCancellation(code)) Result.Cancelled
                            else Result.Failed(code, message.toString()),
                        )
                    }

                    // onAuthenticationFailed = a single non-match; the sheet stays
                    // open for a retry, so there's nothing to resume here.
                },
            )
        }

    private fun isCancellation(code: Int): Boolean =
        code == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
            code == BiometricPrompt.BIOMETRIC_ERROR_CANCELED

    private companion object {
        const val NEGATIVE_LABEL = "Usar PIN"
    }
}
