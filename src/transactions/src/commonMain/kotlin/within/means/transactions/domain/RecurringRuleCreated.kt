package within.means.transactions.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(RecurringRuleCreated.NAME)
data class RecurringRuleCreated(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val type: String,
    val amountCents: Long,
    val categoryId: String,
    val description: String,
    val incomeSource: String?,
    val frequency: String,
    val startDate: String,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "transactions.recurring_rule_created"
    }
}
