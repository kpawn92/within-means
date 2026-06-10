package within.means.android.persistence

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import within.means.categories.db.CategoriesDatabase
import within.means.shared.db.SharedDatabase
import within.means.transactions.db.TransactionsDatabase
import within.means.users.db.UsersDatabase

/**
 * Builds the per-module SQLDelight databases backed by a single SQLCipher
 * file (`within_means.db`). Every module gets its own database class but
 * they all read/write the same encrypted SQLite file.
 *
 * Why manual schema bootstrap:
 *  1. SQLCipher's [SupportOpenHelperFactory] does NOT reliably invoke
 *     [AndroidSqliteDriver.Callback.onCreate].
 *  2. We can't use `PRAGMA user_version` because it is GLOBAL to the
 *     file and we have several schemas sharing it.
 *
 * Solution: check if the schema's *sentinel table* already exists in
 * `sqlite_master`. If it does, the schema is installed; if it doesn't,
 * run `Schema.create(driver)`. This is fully idempotent and immune to
 * stale `_schema_installed` markers.
 *
 * In-place migrations: we keep the SQLDelight schema version pinned at 1 and
 * do NOT use `.sqm` files. The reason is structural — `PRAGMA user_version`
 * is global to the shared file, so bumping one schema's version makes
 * SQLCipher's open helper fire `onUpgrade` for *every* schema (the global
 * version now trails), running a migration whose `ALTER TABLE` may target a
 * table that another schema hasn't created yet. Instead, additive migrations
 * (the only kind we need so far) are applied as idempotent, guarded
 * `ALTER TABLE ... ADD COLUMN` statements via [ensureColumn]: a fresh install
 * gets the columns from `Schema.create`, an existing install gets them added
 * in place. Existing data is preserved — no reinstall needed.
 */
class AndroidDatabaseFactory(private val context: Context) {

    init {
        // Loads libsqlcipher.so packaged with net.zetetic:sqlcipher-android.
        System.loadLibrary("sqlcipher")
    }

    fun buildShared(passphrase: ByteArray): SharedDatabase {
        val driver = openDriver(SharedDatabase.Schema, sentinelTable = "domain_events", passphrase = passphrase)
        return SharedDatabase(driver)
    }

    fun buildUsers(passphrase: ByteArray): UsersDatabase {
        val driver = openDriver(UsersDatabase.Schema, sentinelTable = "user_profile", passphrase = passphrase)
        // Additive migration for installs created before these columns existed.
        // Fresh installs already have them via Schema.create; the guard makes
        // this a no-op there.
        ensureColumn(driver, "user_profile", "month_start_day", "INTEGER NOT NULL DEFAULT 1")
        ensureColumn(driver, "user_profile", "hide_amounts", "INTEGER NOT NULL DEFAULT 0")
        return UsersDatabase(driver)
    }

    fun buildCategories(passphrase: ByteArray): CategoriesDatabase {
        val driver = openDriver(CategoriesDatabase.Schema, sentinelTable = "category", passphrase = passphrase)
        return CategoriesDatabase(driver)
    }

    fun buildTransactions(passphrase: ByteArray): TransactionsDatabase {
        val driver = openDriver(TransactionsDatabase.Schema, sentinelTable = "transaction_entry", passphrase = passphrase)
        return TransactionsDatabase(driver)
    }

    @Suppress("UNCHECKED_CAST")
    private fun openDriver(
        schema: SqlSchema<*>,
        sentinelTable: String,
        passphrase: ByteArray,
    ): SqlDriver {
        val typedSchema = schema as SqlSchema<QueryResult.Value<Unit>>
        val driver = AndroidSqliteDriver(
            schema = typedSchema,
            context = context,
            name = DB_NAME,
            factory = SupportOpenHelperFactory(passphrase.copyOf()),
        )
        if (!tableExists(driver, sentinelTable)) {
            typedSchema.create(driver).value
        }
        return driver
    }

    /** Adds [column] to [table] if it isn't already present. Idempotent. */
    private fun ensureColumn(driver: SqlDriver, table: String, column: String, definition: String) {
        if (!columnExists(driver, table, column)) {
            driver.execute(
                identifier = null,
                sql = "ALTER TABLE $table ADD COLUMN $column $definition",
                parameters = 0,
            )
        }
    }

    private fun columnExists(driver: SqlDriver, table: String, column: String): Boolean =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT COUNT(*) FROM pragma_table_info(?) WHERE name = ?",
            parameters = 2,
            binders = {
                bindString(0, table)
                bindString(1, column)
            },
            mapper = { cursor: SqlCursor ->
                cursor.next()
                QueryResult.Value((cursor.getLong(0) ?: 0L) > 0L)
            },
        ).value

    private fun tableExists(driver: SqlDriver, name: String): Boolean =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
            parameters = 1,
            binders = { bindString(0, name) },
            mapper = { cursor: SqlCursor ->
                cursor.next()
                QueryResult.Value((cursor.getLong(0) ?: 0L) > 0L)
            },
        ).value

    companion object {
        const val DB_NAME = "within_means.db"
    }
}
