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
    // Nullable + defaulted so events serialized before time-of-day existed still deserialize.
    val time: String? = null,
    val description: String,
    val categoryId: String,
    val incomeSource: String?,
    // Defaulted so events serialized before concepts/batch existed still deserialize.
    val conceptIds: List<String> = emptyList(),
    val originRef: String?,
    val recurringRef: String?,
    val batchRef: String? = null,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "transactions.registered"
    }
}
