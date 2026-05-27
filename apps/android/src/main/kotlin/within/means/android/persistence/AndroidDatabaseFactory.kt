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
        return UsersDatabase(driver)
    }

    fun buildCategories(passphrase: ByteArray): CategoriesDatabase {
        val driver = openDriver(CategoriesDatabase.Schema, sentinelTable = "category", passphrase = passphrase)
        return CategoriesDatabase(driver)
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
