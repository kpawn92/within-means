package within.means.users.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(UserDefaultCreated.NAME)
data class UserDefaultCreated(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val displayName: String,
    val locale: String,
    val baseCurrency: String,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "users.default_created"
    }
}
