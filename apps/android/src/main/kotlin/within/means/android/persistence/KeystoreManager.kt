package within.means.android.persistence

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Manages a non-exportable HMAC-SHA256 master key kept inside the
 * Android Keystore. The key never leaves secure hardware; we only ever
 * read its MAC output.
 */
class KeystoreManager {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun ensureMasterKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        generator.generateKey()
    }

    /**
     * Derives a 32-byte secret by computing HMAC-SHA256(masterKey, input).
     * The same input always produces the same output for the same install.
     * Wiping app data destroys the keystore key.
     */
    fun derive(input: ByteArray): ByteArray {
        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val mac = Mac.getInstance("HmacSHA256").apply { init(key) }
        return mac.doFinal(input)
    }

    fun deleteMasterKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "within_means.master_key"
    }
}
