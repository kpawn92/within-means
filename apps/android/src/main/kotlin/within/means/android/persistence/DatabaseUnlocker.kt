package within.means.android.persistence

import within.means.shared.db.SharedDatabase
import within.means.users.db.UsersDatabase

/**
 * Lazily holds the unlocked SQLCipher-backed databases. Access before
 * calling [unlock] fails fast — this is intentional and surfaces wiring
 * mistakes early.
 */
class DatabaseUnlocker(
    private val factory: AndroidDatabaseFactory,
    private val passphraseProvider: PassphraseProvider,
) {

    @Volatile
    private var sharedDb: SharedDatabase? = null

    @Volatile
    private var usersDb: UsersDatabase? = null

    val isUnlocked: Boolean get() = sharedDb != null && usersDb != null

    fun unlock(pin: String) {
        val passphrase = passphraseProvider.derive(pin)
        sharedDb = factory.buildShared(passphrase)
        usersDb = factory.buildUsers(passphrase)
    }

    val shared: SharedDatabase
        get() = sharedDb ?: error("SharedDatabase requested before unlock()")

    val users: UsersDatabase
        get() = usersDb ?: error("UsersDatabase requested before unlock()")
}
