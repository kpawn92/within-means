package within.means.concepts.application.recategorize

import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import kotlin.reflect.KClass

class SetConceptDefaultCategoryCommandHandler(
    private val repository: ConceptRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) : CommandHandler<SetConceptDefaultCategoryCommand> {

    override val commandType: KClass<SetConceptDefaultCategoryCommand> =
        SetConceptDefaultCategoryCommand::class

    override suspend fun handle(command: SetConceptDefaultCategoryCommand) {
        val concept = repository.search(ConceptId(command.conceptId)) ?: return
        concept.changeDefaultCategory(command.defaultCategoryId, uuids)
        repository.save(concept)
        eventBus.publish(concept.pullDomainEvents())
    }
}
