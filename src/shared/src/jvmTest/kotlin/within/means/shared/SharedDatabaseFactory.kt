package within.means.shared

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import within.means.shared.db.SharedDatabase

fun inMemorySharedDatabase(): Pair<SharedDatabase, SqlDriver> {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    SharedDatabase.Schema.create(driver)
    return SharedDatabase(driver) to driver
}
