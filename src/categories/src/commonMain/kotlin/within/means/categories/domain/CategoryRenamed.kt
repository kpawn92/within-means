package within.means.categories.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(CategoryRenamed.NAME)
data class CategoryRenamed(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val newName: String,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "categories.renamed"
    }
}
