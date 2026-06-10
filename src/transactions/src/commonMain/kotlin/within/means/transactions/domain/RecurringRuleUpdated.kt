package within.means.transactions.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(RecurringRuleUpdated.NAME)
data class RecurringRuleUpdated(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val amountCents: Long,
    val categoryId: String,
    val description: String,
    val incomeSource: String?,
    val frequency: String,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "transactions.recurring_rule_updated"
    }
}
