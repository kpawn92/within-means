package within.means.android.persistence

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.os.Build

/** Whether the device can authenticate the user with a strong biometric. */
enum class BiometricStatus {
    /** Hardware present and at least one strong biometric enrolled. */
    AVAILABLE,

    /** Hardware present but the user hasn't enrolled a fingerprint/face. */
    NONE_ENROLLED,

    /** No biometric hardware, or it's temporarily unavailable. */
    NO_HARDWARE,

    /** Below the supported OS level, or status can't be determined. */
    UNSUPPORTED,
}

/**
 * Thin wrapper over the platform [BiometricManager] (no third-party library).
 *
 * The feature targets API 30+ (Android 11): there we can pin the Keystore key to
 * [BiometricManager.Authenticators.BIOMETRIC_STRONG] and use
 * `setUserAuthenticationParameters` uniformly. On older devices the unlock and
 * the Settings row simply don't appear — the app stays PIN-only, no regression.
 */
class BiometricAvailability(private val context: Context) {

    fun status(): BiometricStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return BiometricStatus.UNSUPPORTED
        val manager = context.getSystemService(BiometricManager::class.java)
            ?: return BiometricStatus.UNSUPPORTED
        return when (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.NO_HARDWARE
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    /** True only when a strong biometric is ready to use right now. */
    val isAvailable: Boolean
        get() = status() == BiometricStatus.AVAILABLE
}
