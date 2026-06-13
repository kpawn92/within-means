package within.means.concepts.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import within.means.shared.domain.bus.event.DomainEvent

@Serializable
@SerialName(ConceptCreated.NAME)
data class ConceptCreated(
    override val eventId: String,
    override val aggregateId: String,
    override val occurredOn: Instant,
    val label: String,
    val key: String,
    val kind: String,
    val defaultCategoryId: String?,
) : DomainEvent {
    override val eventName: String = NAME

    companion object {
        const val NAME = "concepts.created"
    }
}
