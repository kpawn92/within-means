package within.means.transactions.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(RecurringRuleDeactivated.NAME)
data class RecurringRuleDeactivated(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "transactions.recurring_rule_deactivated"
    }
}
