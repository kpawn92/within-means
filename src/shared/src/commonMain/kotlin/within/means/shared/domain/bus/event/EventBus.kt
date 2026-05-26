package within.means.shared.domain.bus.event

interface EventBus {
    suspend fun publish(events: List<DomainEvent>)
}
