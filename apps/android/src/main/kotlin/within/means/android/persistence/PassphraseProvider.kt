package within.means.android.persistence

/**
 * Combines the device-bound master key (KeystoreManager) with the user's
 * PIN to derive a 32-byte SQLCipher passphrase.
 *
 * passphrase = HMAC-SHA256( masterKey , utf8(pin) )
 *
 * Compromising the PIN alone is not enough to read the DB: the master
 * key is non-exportable and lives in secure hardware. Wiping app data
 * destroys the master key, so the DB becomes unreadable even with the
 * same PIN.
 */
class PassphraseProvider(private val keystore: KeystoreManager) {

    fun derive(pin: String): ByteArray {
        keystore.ensureMasterKey()
        return keystore.derive(pin.toByteArray(Charsets.UTF_8))
    }
}
