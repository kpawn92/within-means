package within.means.categories.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(CategoryCreated.NAME)
data class CategoryCreated(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val name: String,
    val color: String,
    val icon: String,
    val kind: String,
    val nature: String?,
    val essentiality: String?,
    val productive: Boolean,
    val engelGroup: String?,
    val parentId: String?,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "categories.created"
    }
}
