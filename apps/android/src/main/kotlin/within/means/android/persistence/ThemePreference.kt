package within.means.android.persistence

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Appearance preference: follow the system, or force light / dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Persists the user's appearance choice and exposes it as a [StateFlow] so the
 * theme recomposes the moment it changes. Stored in EncryptedSharedPreferences
 * to match the rest of the app's at-rest posture (the value is trivial, but we
 * keep a single storage idiom).
 */
class ThemePreference(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(value: ThemeMode) {
        prefs.edit().putString(KEY_MODE, value.name).apply()
        _mode.value = value
    }

    private fun read(): ThemeMode =
        prefs.getString(KEY_MODE, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM

    companion object {
        private const val FILE_NAME = "within_means_theme"
        private const val KEY_MODE = "mode"
    }
}
