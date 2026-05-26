package within.means.shared.domain.bus.event

import kotlinx.datetime.Instant

interface DomainEvent {
    val eventId: String
    val aggregateId: String
    val occurredOn: Instant
    val eventName: String
}
