package within.means.concepts.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(ConceptDefaultCategoryChanged.NAME)
data class ConceptDefaultCategoryChanged(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val defaultCategoryId: String?,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "concepts.default_category_changed"
    }
}
