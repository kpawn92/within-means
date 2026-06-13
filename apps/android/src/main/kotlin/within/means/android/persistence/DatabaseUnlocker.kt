package within.means.android.persistence

import app.cash.sqldelight.db.SqlDriver
import within.means.categories.db.CategoriesDatabase
import within.means.concepts.db.ConceptsDatabase
import within.means.shared.db.SharedDatabase
import within.means.transactions.db.TransactionsDatabase
import within.means.users.db.UsersDatabase

/**
 * Lazily holds the unlocked SQLCipher-backed databases. Access before
 * calling [unlock] fails fast — this is intentional and surfaces wiring
 * mistakes early. Keeps the backing [SqlDriver]s too so they can be closed
 * (needed before re-keying the shared file in [changePin]).
 */
class DatabaseUnlocker(
    private val factory: AndroidDatabaseFactory,
    private val passphraseProvider: PassphraseProvider,
) {

    @Volatile
    private var sharedDb: SharedDatabase? = null

    @Volatile
    private var usersDb: UsersDatabase? = null

    @Volatile
    private var categoriesDb: CategoriesDatabase? = null

    @Volatile
    private var conceptsDb: ConceptsDatabase? = null

    @Volatile
    private var transactionsDb: TransactionsDatabase? = null

    @Volatile
    private var drivers: List<SqlDriver> = emptyList()

    @Volatile
    private var lastPin: String? = null

    val isUnlocked: Boolean
        get() = sharedDb != null && usersDb != null && categoriesDb != null &&
            conceptsDb != null && transactionsDb != null

    /**
     * The PIN that unlocked the current session, kept in memory only while the
     * app is unlocked (cleared on [lock]). Biometric enrollment reads it to wrap
     * the PIN without re-prompting the user, since the SQLCipher passphrase —
     * derived from this same PIN — is already resident in memory anyway.
     */
    val sessionPin: String?
        get() = lastPin

    fun unlock(pin: String) {
        val passphrase = passphraseProvider.derive(pin)
        val shared = factory.buildShared(passphrase)
        val users = factory.buildUsers(passphrase)
        val categories = factory.buildCategories(passphrase)
        val concepts = factory.buildConcepts(passphrase)
        val transactions = factory.buildTransactions(passphrase)
        sharedDb = shared.database
        usersDb = users.database
        categoriesDb = categories.database
        conceptsDb = concepts.database
        transactionsDb = transactions.database
        drivers = listOf(
            shared.driver, users.driver, categories.driver, concepts.driver, transactions.driver
        )
        lastPin = pin
    }

    /**
     * Drops the database handles so the app falls back to the PIN unlock
     * screen, closing the underlying connections. The encrypted file on disk is
     * untouched; the next [unlock] re-derives the passphrase and reopens it.
     */
    fun lock() {
        closeDrivers()
    }

    /**
     * Changes the PIN by re-keying the shared SQLCipher file. Closes every
     * connection, re-keys old → new, then reopens with the new PIN. If the old
     * PIN is wrong (or the re-key fails) the file stays on the old key and we
     * reopen with it, rethrowing so the caller can surface the error.
     */
    fun changePin(oldPin: String, newPin: String) {
        val oldPassphrase = passphraseProvider.derive(oldPin)
        val newPassphrase = passphraseProvider.derive(newPin)
        closeDrivers()
        try {
            factory.rekey(oldPassphrase, newPassphrase)
        } catch (e: Throwable) {
            // Re-key didn't happen (wrong old PIN / error): restore the old key.
            unlock(oldPin)
            throw e
        }
        unlock(newPin)
    }

    private fun closeDrivers() {
        sharedDb = null
        usersDb = null
        categoriesDb = null
        conceptsDb = null
        transactionsDb = null
        drivers.forEach { runCatching { it.close() } }
        drivers = emptyList()
        lastPin = null
    }

    val shared: SharedDatabase
        get() = sharedDb ?: error("SharedDatabase requested before unlock()")

    val users: UsersDatabase
        get() = usersDb ?: error("UsersDatabase requested before unlock()")

    val categories: CategoriesDatabase
        get() = categoriesDb ?: error("CategoriesDatabase requested before unlock()")

    val concepts: ConceptsDatabase
        get() = conceptsDb ?: error("ConceptsDatabase requested before unlock()")

    val transactions: TransactionsDatabase
        get() = transactionsDb ?: error("TransactionsDatabase requested before unlock()")
}
