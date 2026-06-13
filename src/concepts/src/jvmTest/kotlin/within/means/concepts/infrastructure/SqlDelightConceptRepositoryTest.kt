package within.means.concepts.infrastructure

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import within.means.concepts.SequentialUuidGenerator
import within.means.concepts.db.ConceptsDatabase
import within.means.concepts.domain.Concept
import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptKey
import within.means.concepts.domain.ConceptKind
import within.means.concepts.domain.ConceptLabel
import within.means.concepts.infrastructure.persistence.SqlDelightConceptRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SqlDelightConceptRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: ConceptsDatabase
    private lateinit var repo: SqlDelightConceptRepository

    private val uuids = SequentialUuidGenerator()

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ConceptsDatabase.Schema.create(driver)
        db = ConceptsDatabase(driver)
        repo = SqlDelightConceptRepository(db, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun concept(
        label: String,
        kind: ConceptKind = ConceptKind.EXPENSE,
        category: String? = null,
    ): Concept = Concept.create(
        id = ConceptId(uuids.next()),
        kind = kind,
        label = ConceptLabel(label),
        defaultCategoryId = category,
        uuids = uuids,
    )

    @Test
    fun `saves and reads back a concept round-trip`() = runTest {
        val c = concept("Cerveza", category = "cat-food").also {
            it.recordUsage(Instant.parse("2026-06-10T10:00:00Z"), uuids)
        }
        repo.save(c)

        val found = repo.search(c.id)!!
        found.label.value shouldBe "Cerveza"
        found.key.value shouldBe "cerveza"
        found.defaultCategoryId shouldBe "cat-food"
        found.usageCount shouldBe 1
        found.lastUsedAt shouldBe Instant.parse("2026-06-10T10:00:00Z")
    }

    @Test
    fun `findByKey matches on kind and normalized key`() = runTest {
        val c = concept("Cerveza")
        repo.save(c)

        repo.findByKey(ConceptKind.EXPENSE, ConceptKey.of("cerveza"))!!.id shouldBe c.id
        repo.findByKey(ConceptKind.INCOME, ConceptKey.of("cerveza")).shouldBeNull()
    }

    @Test
    fun `byKind orders most-used first and filters by kind`() = runTest {
        val rare = concept("Pan").also { repo.save(it) }
        val frequent = concept("Cerveza").also {
            it.recordUsage(Instant.parse("2026-06-01T10:00:00Z"), uuids)
            it.recordUsage(Instant.parse("2026-06-02T10:00:00Z"), uuids)
            repo.save(it)
        }
        repo.save(concept("Nómina", kind = ConceptKind.INCOME))

        val expenses = repo.byKind(ConceptKind.EXPENSE)
        expenses shouldHaveSize 2
        expenses.first().id shouldBe frequent.id
        expenses.last().id shouldBe rare.id
    }

    @Test
    fun `delete removes the row`() = runTest {
        val c = concept("Pan").also { repo.save(it) }
        repo.delete(c.id)
        repo.search(c.id).shouldBeNull()
        repo.countAll() shouldBe 0L
    }
}
