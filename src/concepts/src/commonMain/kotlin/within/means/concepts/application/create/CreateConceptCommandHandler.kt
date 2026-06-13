package within.means.concepts.application.create

import within.means.shared.domain.bus.command.CommandHandler
import kotlin.reflect.KClass

class CreateConceptCommandHandler(
    private val creator: ConceptCreator,
) : CommandHandler<CreateConceptCommand> {

    override val commandType: KClass<CreateConceptCommand> = CreateConceptCommand::class

    override suspend fun handle(command: CreateConceptCommand) {
        creator.create(command)
    }
}
