package within.means.concepts.application.delete

import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import kotlin.reflect.KClass

class DeleteConceptCommandHandler(
    private val repository: ConceptRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) : CommandHandler<DeleteConceptCommand> {

    override val commandType: KClass<DeleteConceptCommand> = DeleteConceptCommand::class

    override suspend fun handle(command: DeleteConceptCommand) {
        val id = ConceptId(command.conceptId)
        val concept = repository.search(id) ?: return
        concept.markDeleted(uuids)
        eventBus.publish(concept.pullDomainEvents())
        repository.delete(id)
    }
}
