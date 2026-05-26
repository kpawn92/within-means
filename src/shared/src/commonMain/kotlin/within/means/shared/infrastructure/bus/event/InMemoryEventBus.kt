package within.means.shared.infrastructure.bus.event

import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.shared.domain.bus.event.EventBus

class InMemoryEventBus(
    private val subscribers: List<DomainEventSubscriber<out DomainEvent>>,
) : EventBus {

    override suspend fun publish(events: List<DomainEvent>) {
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
