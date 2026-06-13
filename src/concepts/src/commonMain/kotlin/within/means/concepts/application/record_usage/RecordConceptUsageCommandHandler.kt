package within.means.concepts.application.record_usage

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import kotlin.reflect.KClass

class RecordConceptUsageCommandHandler(
    private val repository: ConceptRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
    private val clock: Clock = Clock.System,
) : CommandHandler<RecordConceptUsageCommand> {

    override val commandType: KClass<RecordConceptUsageCommand> = RecordConceptUsageCommand::class

    override suspend fun handle(command: RecordConceptUsageCommand) {
        val concept = repository.search(ConceptId(command.conceptId)) ?: return
        val at = command.atIso?.let { Instant.parse(it) } ?: clock.now()
        concept.recordUsage(at = at, uuids = uuids)
        repository.save(concept)
        eventBus.publish(concept.pullDomainEvents())
    }
}
