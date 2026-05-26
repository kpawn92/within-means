package within.means.shared.infrastructure

import app.cash.sqldelight.db.SqlDriver
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import within.means.shared.db.SharedDatabase
import within.means.shared.domain.bus.event.DomainEventRecord
import within.means.shared.inMemorySharedDatabase
import within.means.shared.infrastructure.persistence.SqlDelightDomainEventStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SqlDelightDomainEventStoreTest {

    private lateinit var db: SharedDatabase
    private lateinit var driver: SqlDriver
    private lateinit var store: SqlDelightDomainEventStore

    @BeforeTest
    fun setUp() {
        val (database, sqlDriver) = inMemorySharedDatabase()
        db = database
        driver = sqlDriver
        store = SqlDelightDomainEventStore(db, Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun record(
        id: String,
        name: String = "test.created",
        aggregateId: String = "agg-1",
        epochSeconds: Long = 0,
    ) = DomainEventRecord(
        eventId = id,
        eventName = name,
        aggregateId = aggregateId,
        aggregateType = "TestAggregate",
        occurredOn = Instant.fromEpochSeconds(epochSeconds),
        payloadJson = "{}",
    )

    @Test
    fun `appends and reads back a single record`() = runTest {
        val r = record("evt-1")
        store.append(r)
        store.findById("evt-1") shouldBe r
    }

    @Test
    fun `findById returns null when missing`() = runTest {
        store.findById("missing") shouldBe null
    }

    @Test
    fun `appendAll persists records in a single transaction`() = runTest {
        store.appendAll(
            listOf(
                record("evt-1", epochSeconds = 1),
                record("evt-2", epochSeconds = 2),
                record("evt-3", epochSeconds = 3),
            )
        )
        store.findAll().map { it.eventId } shouldContainExactly listOf("evt-1", "evt-2", "evt-3")
    }

    @Test
    fun `findByAggregate returns ordered events for the given aggregate`() = runTest {
        store.appendAll(
            listOf(
                record("evt-1", aggregateId = "agg-1", epochSeconds = 100),
                record("evt-2", aggregateId = "agg-2", epochSeconds = 200),
                record("evt-3", aggregateId = "agg-1", epochSeconds = 300),
            )
        )
        store.findByAggregate("agg-1").map { it.eventId } shouldContainExactly listOf("evt-1", "evt-3")
    }

    @Test
    fun `findByName filters by event name`() = runTest {
        store.appendAll(
            listOf(
                record("evt-1", name = "created"),
                record("evt-2", name = "renamed"),
                record("evt-3", name = "created"),
            )
        )
        store.findByName("created").map { it.eventId } shouldContainExactly listOf("evt-1", "evt-3")
    }

    @Test
    fun `findAllSince filters by occurred_on`() = runTest {
        store.appendAll(
            listOf(
                record("evt-1", epochSeconds = 100),
                record("evt-2", epochSeconds = 200),
                record("evt-3", epochSeconds = 300),
            )
        )
        store.findAllSince(Instant.fromEpochSeconds(200)).map { it.eventId } shouldContainExactly
            listOf("evt-2", "evt-3")
    }

    @Test
    fun `empty appendAll does nothing`() = runTest {
        store.appendAll(emptyList())
        store.findAll().shouldBeEmpty()
    }
}
