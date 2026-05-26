package within.means.shared.domain.bus.event

import kotlin.reflect.KClass

interface DomainEventSubscriber<E : DomainEvent> {
    val subscribedTo: KClass<E>
    suspend fun consume(event: E)
}
