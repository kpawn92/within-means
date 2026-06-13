package within.means.android.subscribers

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.concepts.application.record_usage.RecordConceptUsageCommand
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.transactions.domain.TransactionRegistered
import kotlin.reflect.KClass

/**
 * Cross-context glue wired in the composition root: when a movement is
 * registered, bump the usage of each concept it carries so the chip row stays
 * ranked most-used-first (CONCEPTS-SPEC §9). Lives in apps/android so neither
 * :transactions nor :concepts learns about the other.
 *
 * The [CommandBus] is resolved lazily via [KoinComponent.get] to break the Koin
 * cycle: EventBus → this subscriber → CommandBus → RecordConceptUsageCommandHandler
 * → EventBus. The usage timestamp is the movement's own instant ([event.occurredOn]).
 */
class RecordConceptUsageOnTransactionRegistered :
    DomainEventSubscriber<TransactionRegistered>, KoinComponent {

    override val subscribedTo: KClass<TransactionRegistered> = TransactionRegistered::class

    override suspend fun consume(event: TransactionRegistered) {
        if (event.conceptIds.isEmpty()) return
        val bus = get<CommandBus>()
        event.conceptIds.forEach { conceptId ->
            bus.dispatch(
                RecordConceptUsageCommand(conceptId = conceptId, atIso = event.occurredOn.toString())
            )
        }
    }
}
