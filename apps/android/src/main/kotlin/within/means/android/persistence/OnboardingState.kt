package within.means.android.persistence

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Tracks whether the user has finished onboarding so that on subsequent
 * launches we know whether to show the Welcome flow or jump straight to
 * the PIN unlock screen.
 *
 * Stored in EncryptedSharedPreferences (AES-GCM) so even though the
 * value is just a boolean, it cannot be tampered with off-device.
 */
class OnboardingState(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var isCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPLETED, value).apply()

    fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "within_means_onboarding"
        private const val KEY_COMPLETED = "completed"
    }
}
