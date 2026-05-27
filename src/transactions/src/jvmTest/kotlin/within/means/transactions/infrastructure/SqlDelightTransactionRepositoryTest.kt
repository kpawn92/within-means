package within.means.transactions.infrastructure

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import within.means.shared.domain.criteria.Criteria
import within.means.shared.domain.criteria.Filter
import within.means.shared.domain.criteria.FilterField
import within.means.shared.domain.criteria.FilterOperator
import within.means.shared.domain.criteria.FilterValue
import within.means.shared.domain.criteria.Filters
import within.means.shared.domain.criteria.Order
import within.means.transactions.FixedClock
import within.means.transactions.SequentialUuidGenerator
import within.means.transactions.db.TransactionsDatabase
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.IncomeSource
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionType
import within.means.transactions.infrastructure.persistence.SqlDelightTransactionRepository

class SqlDelightTransactionRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: TransactionsDatabase
    private lateinit var repo: SqlDelightTransactionRepository

    private val zone = TimeZone.UTC
    private val clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z"))
    private val uuids = SequentialUuidGenerator()
    private val food = CategoryRef("00000000-0000-4000-8000-000000000900")
    private val salary = CategoryRef("00000000-0000-4000-8000-000000000901")

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TransactionsDatabase.Schema.create(driver)
        db = TransactionsDatabase(driver)
        repo = SqlDelightTransactionRepository(db, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun save_then_findById_round_trips_all_fields() = runTest {
        val tx = Transaction.register(
            id = TransactionId(uuids.next()),
            type = TransactionType.INCOME,
            amount = Amount(200000L),
            date = TransactionDate(LocalDate(2026, 5, 25)),
            description = TransactionDescription("Sueldo"),
            categoryRef = salary,
            incomeSource = IncomeSource("Empresa S.A."),
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )

        repo.save(tx)
        val loaded = repo.search(tx.id)!!

        loaded.id shouldBe tx.id
        loaded.type shouldBe TransactionType.INCOME
        loaded.amount shouldBe Amount(200000L)
        loaded.date.value shouldBe LocalDate(2026, 5, 25)
        loaded.description.value shouldBe "Sueldo"
        loaded.categoryRef shouldBe salary
        loaded.incomeSource?.value shouldBe "Empresa S.A."
    }

    @Test
    fun delete_removes_the_row() = runTest {
        val tx = expense(100L, LocalDate(2026, 5, 20))
        repo.save(tx)
        repo.delete(tx.id)
        repo.search(tx.id) shouldBe null
        repo.countAll() shouldBe 0L
    }

    @Test
    fun observeAll_emits_after_save_and_delete() = runTest {
        val tx = expense(100L, LocalDate(2026, 5, 20))
        repo.save(tx)
        repo.observeAll().first() shouldHaveSize 1
        repo.delete(tx.id)
        repo.observeAll().first() shouldHaveSize 0
    }

    @Test
    fun matching_filters_and_sorts_by_date_desc() = runTest {
        repo.save(expense(100L, LocalDate(2026, 5, 20)))
        repo.save(expense(200L, LocalDate(2026, 5, 25)))
        repo.save(expense(300L, LocalDate(2026, 5, 27)))

        val recent = repo.matching(
            Criteria(
                filters = Filters(
                    listOf(
                        Filter(FilterField("date"), FilterOperator.GTE, FilterValue("2026-05-25"))
                    )
                ),
                order = Order.desc("date"),
                limit = 2,
            )
        )

        recent.map { it.amount.cents } shouldContainExactly listOf(300L, 200L)
    }

    private fun expense(cents: Long, on: LocalDate): Transaction = Transaction.register(
        id = TransactionId(uuids.next()),
        type = TransactionType.EXPENSE,
        amount = Amount(cents),
        date = TransactionDate(on),
        description = TransactionDescription.EMPTY,
        categoryRef = food,
        uuids = uuids,
        clock = clock,
        timeZone = zone,
    )
}
