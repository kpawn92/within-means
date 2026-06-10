package within.means.users.infrastructure

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import within.means.shared.domain.money.Currency
import within.means.users.SequentialUuidGenerator
import within.means.users.db.UsersDatabase
import within.means.users.domain.DisplayName
import within.means.users.domain.Locale
import within.means.users.domain.UserId
import within.means.users.domain.UserProfile
import within.means.users.infrastructure.persistence.SqlDelightUserProfileRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SqlDelightUserProfileRepositoryTest {

    private lateinit var db: UsersDatabase
    private lateinit var driver: SqlDriver
    private lateinit var repo: SqlDelightUserProfileRepository

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        UsersDatabase.Schema.create(driver)
        db = UsersDatabase(driver)
        repo = SqlDelightUserProfileRepository(db, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `save then findById restores the aggregate state`() = runTest {
        val uuids = SequentialUuidGenerator()
        val profile = UserProfile.bootstrap(id = UserId(uuids.next()), uuids = uuids)
        repo.save(profile)

        val loaded = repo.search(profile.id)
            ?: error("expected profile to be persisted")
        loaded.id shouldBe profile.id
        loaded.displayName shouldBe profile.displayName
        loaded.locale shouldBe profile.locale
        loaded.baseCurrency shouldBe profile.baseCurrency
    }

    @Test
    fun `searchDefault returns the saved default user`() = runTest {
        val uuids = SequentialUuidGenerator()
        val profile = UserProfile.bootstrap(id = UserId(uuids.next()), uuids = uuids)
        repo.save(profile)

        val def = repo.searchDefault()
        def shouldNotBe null
        def!!.id shouldBe profile.id
    }

    @Test
    fun `save updates an existing default profile in place`() = runTest {
        val uuids = SequentialUuidGenerator()
        val profile = UserProfile.bootstrap(id = UserId(uuids.next()), uuids = uuids)
        repo.save(profile)

        profile.updatePreferences(
            displayName = DisplayName("Alejandro"),
            locale = Locale.EN,
            baseCurrency = Currency.USD,
            monthlyBudgetCents = 200000L,
            spendingAlertsEnabled = false,
            monthStartDay = 5,
            hideAmounts = true,
            uuids = uuids,
        )
        repo.save(profile)

        val loaded = repo.searchDefault()!!
        loaded.displayName.value shouldBe "Alejandro"
        loaded.locale shouldBe Locale.EN
        loaded.baseCurrency shouldBe Currency.USD
        loaded.monthlyBudgetCents shouldBe 200000L
        loaded.spendingAlertsEnabled shouldBe false
        loaded.monthStartDay shouldBe 5
        loaded.hideAmounts shouldBe true
    }
}
