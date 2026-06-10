package within.means.android.persistence

import within.means.categories.db.CategoriesDatabase
import within.means.shared.db.SharedDatabase
import within.means.transactions.db.TransactionsDatabase
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

    @Volatile
    private var categoriesDb: CategoriesDatabase? = null

    @Volatile
    private var transactionsDb: TransactionsDatabase? = null

    val isUnlocked: Boolean
        get() = sharedDb != null && usersDb != null && categoriesDb != null && transactionsDb != null

    fun unlock(pin: String) {
        val passphrase = passphraseProvider.derive(pin)
        sharedDb = factory.buildShared(passphrase)
        usersDb = factory.buildUsers(passphrase)
        categoriesDb = factory.buildCategories(passphrase)
        transactionsDb = factory.buildTransactions(passphrase)
    }

    /**
     * Drops the in-memory database handles so the app falls back to the PIN
     * unlock screen. The encrypted file on disk is untouched; the next
     * [unlock] re-derives the passphrase and reopens it.
     */
    fun lock() {
        sharedDb = null
        usersDb = null
        categoriesDb = null
        transactionsDb = null
    }

    val shared: SharedDatabase
        get() = sharedDb ?: error("SharedDatabase requested before unlock()")

    val users: UsersDatabase
        get() = usersDb ?: error("UsersDatabase requested before unlock()")

    val categories: CategoriesDatabase
        get() = categoriesDb ?: error("CategoriesDatabase requested before unlock()")

    val transactions: TransactionsDatabase
        get() = transactionsDb ?: error("TransactionsDatabase requested before unlock()")
}
