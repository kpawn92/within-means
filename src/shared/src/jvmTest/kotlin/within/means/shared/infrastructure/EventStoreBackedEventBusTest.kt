package within.means.shared.infrastructure

import app.cash.sqldelight.db.SqlDriver
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import within.means.shared.db.SharedDatabase
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.shared.inMemorySharedDatabase
import within.means.shared.infrastructure.bus.event.EventStoreBackedEventBus
import within.means.shared.infrastructure.persistence.SqlDelightDomainEventStore
import within.means.shared.infrastructure.serialization.DomainEventJsonSerializer
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class EventStoreBackedEventBusTest {

    @Serializable
    private data class SampleCreated(
        override val eventId: String,
        override val aggregateId: String,
        override val occurredOn: Instant,
        val payload: String,
    ) : DomainEvent {
        override val eventName: String = NAME

        companion object {
            const val NAME = "sample.created"
        }
    }

    private class OnSampleCreated : DomainEventSubscriber<SampleCreated> {
        val consumed = mutableListOf<String>()
        override val subscribedTo: KClass<SampleCreated> = SampleCreated::class
        override suspend fun consume(event: SampleCreated) {
            consumed.add(event.payload)
        }
    }

    private lateinit var db: SharedDatabase
    private lateinit var driver: SqlDriver
    private lateinit var store: SqlDelightDomainEventStore
    private lateinit var serializer: DomainEventJsonSerializer

    @BeforeTest
    fun setUp() {
        val (database, sqlDriver) = inMemorySharedDatabase()
        db = database
        driver = sqlDriver
        store = SqlDelightDomainEventStore(db, Dispatchers.Unconfined)
        serializer = DomainEventJsonSerializer(
            serializers = mapOf(SampleCreated.NAME to SampleCreated.serializer())
        )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `persists events to the store and dispatches them to subscribers`() = runTest {
        val subscriber = OnSampleCreated()
        val bus = EventStoreBackedEventBus(store, serializer, listOf(subscriber))

        val event = SampleCreated(
            eventId = "evt-1",
            aggregateId = "agg-1",
            occurredOn = Instant.fromEpochSeconds(123),
            payload = "hello",
        )

        bus.publish(listOf(event))

        subscriber.consumed shouldContainExactly listOf("hello")
        val persisted = store.findById("evt-1")
            ?: error("Expected event to be persisted to the store")
        persisted.eventName shouldBe SampleCreated.NAME
        persisted.aggregateId shouldBe "agg-1"
        persisted.aggregateType shouldBe "SampleCreated"
    }

    @Test
    fun `empty publish is a no-op`() = runTest {
        val subscriber = OnSampleCreated()
        val bus = EventStoreBackedEventBus(store, serializer, listOf(subscriber))

        bus.publish(emptyList())

        subscriber.consumed.isEmpty() shouldBe true
        store.findAll().isEmpty() shouldBe true
    }

    @Test
    fun `serializer round-trips a domain event`() = runTest {
        val original = SampleCreated(
            eventId = "evt-9",
            aggregateId = "agg-9",
            occurredOn = Instant.fromEpochSeconds(999),
            payload = "round-trip",
        )

        val record = serializer.toRecord(original)
        val decoded = serializer.fromRecord(record) as SampleCreated

        decoded shouldBe original
    }
}
