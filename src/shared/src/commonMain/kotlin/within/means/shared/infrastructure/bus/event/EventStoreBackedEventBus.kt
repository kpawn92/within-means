package within.means.shared.infrastructure.bus.event

import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.DomainEventStore
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.shared.domain.bus.event.EventBus
import within.means.shared.infrastructure.serialization.DomainEventJsonSerializer

/**
 * EventBus that persists every published event into the Event Store before
 * dispatching it to in-process subscribers.
 *
 * Order matters: persist first, then notify. If a subscriber throws, the
 * event is already durable and can be replayed.
 */
class EventStoreBackedEventBus(
    private val store: DomainEventStore,
    private val serializer: DomainEventJsonSerializer,
    private val subscribers: List<DomainEventSubscriber<out DomainEvent>>,
) : EventBus {

    override suspend fun publish(events: List<DomainEvent>) {
        if (events.isEmpty()) return

        val records = events.map(serializer::toRecord)
        store.appendAll(records)

        events.forEach { event -> dispatch(event) }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun dispatch(event: DomainEvent) {
        subscribers
            .filter { it.subscribedTo == event::class }
            .map { it as DomainEventSubscriber<DomainEvent> }
            .forEach { it.consume(event) }
    }
}
