package within.means.categories.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(CategoryRestyled.NAME)
data class CategoryRestyled(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val newColor: String,
    val newIcon: String,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "categories.restyled"
    }
}
