package within.means.shared.infrastructure

import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import kotlin.reflect.KClass
import kotlin.test.Test

class InMemoryEventBusTest {

    private data class Created(
        override val eventId: String,
        override val aggregateId: String,
        override val occurredOn: Instant,
    ) : DomainEvent {
        override val eventName: String = "test.created"
    }

    private data class Renamed(
        override val eventId: String,
        override val aggregateId: String,
        override val occurredOn: Instant,
        val newName: String,
    ) : DomainEvent {
        override val eventName: String = "test.renamed"
    }

    private class OnCreated : DomainEventSubscriber<Created> {
        val consumed = mutableListOf<String>()
        override val subscribedTo: KClass<Created> = Created::class
        override suspend fun consume(event: Created) {
            consumed.add(event.aggregateId)
        }
    }

    private class OnRenamed : DomainEventSubscriber<Renamed> {
        val consumed = mutableListOf<String>()
        override val subscribedTo: KClass<Renamed> = Renamed::class
        override suspend fun consume(event: Renamed) {
            consumed.add(event.newName)
        }
    }

    @Test
    fun `delivers each event only to its subscribers`() = runTest {
        val onCreated = OnCreated()
        val onRenamed = OnRenamed()
        val bus = InMemoryEventBus(listOf(onCreated, onRenamed))

        bus.publish(
            listOf(
                Created("evt-1", "agg-1", Instant.fromEpochSeconds(0)),
                Renamed("evt-2", "agg-1", Instant.fromEpochSeconds(1), newName = "new"),
                Created("evt-3", "agg-2", Instant.fromEpochSeconds(2)),
            )
        )

        onCreated.consumed.shouldContainExactly("agg-1", "agg-2")
        onRenamed.consumed.shouldContainExactly("new")
    }
}
