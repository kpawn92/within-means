package within.means.transactions.infrastructure

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
import within.means.transactions.domain.BatchRef
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.ConceptRefs
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionType
import within.means.transactions.infrastructure.persistence.SqlDelightTransactionRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SqlDelightTransactionConceptsTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: TransactionsDatabase
    private lateinit var repo: SqlDelightTransactionRepository

    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 5, 27)
    private val clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z"))
    private val uuids = SequentialUuidGenerator()
    private val market = CategoryRef("00000000-0000-4000-8000-000000000900")

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

    private fun expense(
        id: String,
        concepts: ConceptRefs = ConceptRefs.EMPTY,
        batch: BatchRef? = null,
    ): Transaction = Transaction.register(
        id = TransactionId(id),
        type = TransactionType.EXPENSE,
        amount = Amount(100L),
        date = TransactionDate(today),
        description = TransactionDescription("x"),
        categoryRef = market,
        conceptRefs = concepts,
        batchRef = batch,
        uuids = uuids,
        clock = clock,
        timeZone = zone,
    )

    private fun conceptIdFilter(conceptId: String) = Criteria(
        filters = Filters.of(Filter(FilterField("conceptId"), FilterOperator.EQUALS, FilterValue(conceptId))),
        order = Order.desc("date"),
    )

    @Test
    fun `round-trips concept refs and batch ref through the bridge table`() = runTest {
        val tx = expense(
            id = "00000000-0000-4000-8000-000000000001",
            concepts = ConceptRefs.of("concept-cerveza", "concept-papa"),
            batch = BatchRef("batch-1"),
        )
        repo.save(tx)

        val found = repo.search(tx.id)!!
        found.conceptRefs shouldBe ConceptRefs.of("concept-cerveza", "concept-papa")
        found.batchRef?.value shouldBe "batch-1"
    }

    @Test
    fun `editing concepts replaces the bridge rows, not appends`() = runTest {
        val tx = expense(
            id = "00000000-0000-4000-8000-000000000001",
            concepts = ConceptRefs.of("concept-a", "concept-b"),
        )
        repo.save(tx)

        tx.edit(
            newAmount = Amount(100L),
            newDate = TransactionDate(today),
            newTime = null,
            newDescription = TransactionDescription("x"),
            newCategoryRef = market,
            newIncomeSource = null,
            newConceptRefs = ConceptRefs.of("concept-c"),
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )
        repo.save(tx)

        repo.search(tx.id)!!.conceptRefs shouldBe ConceptRefs.of("concept-c")
    }

    @Test
    fun `matching filters movements by concept id`() = runTest {
        repo.save(expense("00000000-0000-4000-8000-000000000001", ConceptRefs.of("concept-cerveza")))
        repo.save(expense("00000000-0000-4000-8000-000000000002", ConceptRefs.of("concept-cerveza", "concept-papa")))
        repo.save(expense("00000000-0000-4000-8000-000000000003", ConceptRefs.of("concept-pan")))

        val cerveza = repo.matching(conceptIdFilter("concept-cerveza"))
        cerveza shouldHaveSize 2
        cerveza.map { it.id.value }.sorted() shouldContainExactly listOf(
            "00000000-0000-4000-8000-000000000001",
            "00000000-0000-4000-8000-000000000002",
        )
    }

    @Test
    fun `delete removes the concept links too`() = runTest {
        val tx = expense("00000000-0000-4000-8000-000000000001", ConceptRefs.of("concept-cerveza"))
        repo.save(tx)
        repo.delete(tx.id)

        repo.search(tx.id) shouldBe null
        repo.matching(conceptIdFilter("concept-cerveza")) shouldHaveSize 0
    }
}
