package within.means.transactions.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(TransactionRegistered.NAME)
data class TransactionRegistered(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val type: String,
    val amountCents: Long,
    val date: String,
    val description: String,
    val categoryId: String,
    val incomeSource: String?,
    val originRef: String?,
    val recurringRef: String?,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "transactions.registered"
    }
}
