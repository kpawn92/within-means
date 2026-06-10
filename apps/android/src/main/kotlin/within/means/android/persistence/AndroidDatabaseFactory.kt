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
 * In-place migrations: because `PRAGMA user_version` is global to the
 * shared file we can't let [AndroidSqliteDriver] drive upgrades. Instead
 * we keep a tiny `wm_schema_version(name, version)` table and, when a
 * schema's installed version is behind [SqlSchema.version], run
 * `Schema.migrate(...)` ourselves. A legacy install (sentinel present but
 * no version row) is treated as version 1 — the shipped baseline — so its
 * `.sqm` migrations replay and existing data is preserved (no wipe).
 */
class AndroidDatabaseFactory(private val context: Context) {

    init {
        // Loads libsqlcipher.so packaged with net.zetetic:sqlcipher-android.
        System.loadLibrary("sqlcipher")
    }

    fun buildShared(passphrase: ByteArray): SharedDatabase {
        val driver = openDriver(SharedDatabase.Schema, "shared", sentinelTable = "domain_events", passphrase = passphrase)
        return SharedDatabase(driver)
    }

    fun buildUsers(passphrase: ByteArray): UsersDatabase {
        val driver = openDriver(UsersDatabase.Schema, "users", sentinelTable = "user_profile", passphrase = passphrase)
        return UsersDatabase(driver)
    }

    fun buildCategories(passphrase: ByteArray): CategoriesDatabase {
        val driver = openDriver(CategoriesDatabase.Schema, "categories", sentinelTable = "category", passphrase = passphrase)
        return CategoriesDatabase(driver)
    }

    fun buildTransactions(passphrase: ByteArray): TransactionsDatabase {
        val driver = openDriver(TransactionsDatabase.Schema, "transactions", sentinelTable = "transaction_entry", passphrase = passphrase)
        return TransactionsDatabase(driver)
    }

    @Suppress("UNCHECKED_CAST")
    private fun openDriver(
        schema: SqlSchema<*>,
        schemaName: String,
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
        ensureVersionTable(driver)
        if (!tableExists(driver, sentinelTable)) {
            // Fresh install: create the latest schema directly.
            typedSchema.create(driver).value
            recordVersion(driver, schemaName, schema.version)
        } else {
            // Existing install: replay any pending migrations in place.
            // A missing row means a pre-migration install → baseline v1.
            val installed = installedVersion(driver, schemaName) ?: 1L
            if (installed < schema.version) {
                typedSchema.migrate(driver, installed, schema.version).value
            }
            recordVersion(driver, schemaName, schema.version)
        }
        return driver
    }

    private fun ensureVersionTable(driver: SqlDriver) {
        driver.execute(
            identifier = null,
            sql = "CREATE TABLE IF NOT EXISTS $VERSION_TABLE (name TEXT NOT NULL PRIMARY KEY, version INTEGER NOT NULL)",
            parameters = 0,
        )
    }

    private fun installedVersion(driver: SqlDriver, schemaName: String): Long? =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT version FROM $VERSION_TABLE WHERE name = ?",
            parameters = 1,
            binders = { bindString(0, schemaName) },
            mapper = { cursor: SqlCursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
            },
        ).value

    private fun recordVersion(driver: SqlDriver, schemaName: String, version: Long) {
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO $VERSION_TABLE(name, version) VALUES (?, ?)",
            parameters = 2,
            binders = {
                bindString(0, schemaName)
                bindLong(1, version)
            },
        )
    }

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

        /** Per-schema version tracker (we can't use the global PRAGMA user_version). */
        private const val VERSION_TABLE = "wm_schema_version"
    }
}
