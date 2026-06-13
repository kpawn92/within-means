package within.means.concepts.application.rename

import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptLabel
import within.means.concepts.domain.ConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import kotlin.reflect.KClass

class RenameConceptCommandHandler(
    private val repository: ConceptRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) : CommandHandler<RenameConceptCommand> {

    override val commandType: KClass<RenameConceptCommand> = RenameConceptCommand::class

    override suspend fun handle(command: RenameConceptCommand) {
        val concept = repository.search(ConceptId(command.conceptId)) ?: return
        concept.rename(ConceptLabel(command.newLabel), uuids)
        repository.save(concept)
        eventBus.publish(concept.pullDomainEvents())
    }
}
