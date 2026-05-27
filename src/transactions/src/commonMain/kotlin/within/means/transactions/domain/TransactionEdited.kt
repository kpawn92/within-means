package within.means.transactions.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(TransactionEdited.NAME)
data class TransactionEdited(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val amountCents: Long,
    val date: String,
    val description: String,
    val categoryId: String,
    val incomeSource: String?,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "transactions.edited"
    }
}
