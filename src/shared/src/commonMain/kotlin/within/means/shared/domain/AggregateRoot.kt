package within.means.shared.domain

import within.means.shared.domain.bus.event.DomainEvent

abstract class AggregateRoot {

    private val domainEvents: MutableList<DomainEvent> = mutableListOf()

    protected fun record(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun pullDomainEvents(): List<DomainEvent> {
        val snapshot = domainEvents.toList()
        domainEvents.clear()
        return snapshot
    }
}
