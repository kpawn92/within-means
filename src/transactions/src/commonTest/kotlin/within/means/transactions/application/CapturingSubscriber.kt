package within.means.transactions.application

import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.transactions.domain.TransactionRegistered
import kotlin.reflect.KClass

class CapturingSubscriber : DomainEventSubscriber<TransactionRegistered> {
    val events = mutableListOf<TransactionRegistered>()
    override val subscribedTo: KClass<TransactionRegistered> = TransactionRegistered::class
    override suspend fun consume(event: TransactionRegistered) {
        events.add(event)
    }
}
