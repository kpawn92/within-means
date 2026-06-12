package within.means.android.persistence

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Custodies the user's PIN behind a biometric-gated Keystore key so the app can
 * be unlocked with a fingerprint instead of typing the PIN. See
 * `doc/within-means-app/BIOMETRIC-UNLOCK-SPEC.md`.
 *
 * The biometric **never** touches the SQLCipher passphrase: it only decrypts the
 * stored PIN, which then feeds the existing [DatabaseUnlocker.unlock] pipeline.
 *
 * The Keystore key (`biometric_key`, AES-256-GCM) requires a fresh strong
 * biometric for every use and is **invalidated automatically** if a new
 * fingerprint is enrolled on the device ([setInvalidatedByBiometricEnrollment]),
 * so a biometric added by a third party can't open the app — decrypt then throws
 * [android.security.keystore.KeyPermanentlyInvalidatedException] and the caller
 * falls back to the PIN.
 *
 * The wrapped PIN (ciphertext + IV) lives in [EncryptedSharedPreferences], the
 * same AES-GCM store used by [OnboardingState]. The key-generation and cipher
 * factories require API 30; the rest ([isEnrolled], [store], [retrievePin],
 * [clear]) are version-agnostic so the class can be constructed on any device.
 */
class BiometricVault(context: Context) {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /** True when a PIN has been wrapped and the Keystore key still exists. */
    val isEnrolled: Boolean
        get() = prefs.contains(KEY_CIPHERTEXT) && keyStore.containsAlias(KEY_ALIAS)

    /**
     * Cipher in ENCRYPT mode to be unlocked by `BiometricPrompt` before wrapping
     * the PIN. Generates the Keystore key on first use.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun encryptCipher(): Cipher {
        generateKey()
        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        return Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    /** Persists the PIN encrypted by the already biometric-authenticated cipher. */
    fun store(pin: String, authenticatedCipher: Cipher) {
        val ciphertext = authenticatedCipher.doFinal(pin.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(authenticatedCipher.iv, Base64.NO_WRAP))
            .apply()
    }

    /**
     * Cipher in DECRYPT mode initialized with the stored IV. Throws
     * [android.security.keystore.KeyPermanentlyInvalidatedException] if the
     * device's biometrics changed since enrollment — the caller must then
     * [clear] the vault and fall back to the PIN.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun decryptCipher(): Cipher {
        val iv = Base64.decode(requireStored(KEY_IV), Base64.NO_WRAP)
        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
    }

    /** Recovers the PIN using the already biometric-authenticated decrypt cipher. */
    fun retrievePin(authenticatedCipher: Cipher): String {
        val ciphertext = Base64.decode(requireStored(KEY_CIPHERTEXT), Base64.NO_WRAP)
        return String(authenticatedCipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /** Disables biometric unlock: drops the wrapped PIN and the Keystore key. */
    fun clear() {
        prefs.edit().clear().apply()
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun requireStored(key: String): String =
        prefs.getString(key, null) ?: error("Biometric vault is not enrolled")

    @RequiresApi(Build.VERSION_CODES.R)
    private fun generateKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .setInvalidatedByBiometricEnrollment(true)
                .build(),
        )
        generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "within_means.biometric_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val FILE_NAME = "within_means_biometric"
        private const val KEY_CIPHERTEXT = "pin_ciphertext"
        private const val KEY_IV = "pin_iv"
    }
}
