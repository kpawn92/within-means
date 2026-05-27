package within.means.categories.application.create

import within.means.shared.domain.bus.command.CommandHandler
import kotlin.reflect.KClass

class CreateCategoryCommandHandler(
    private val creator: CategoryCreator,
) : CommandHandler<CreateCategoryCommand> {

    override val commandType: KClass<CreateCategoryCommand> = CreateCategoryCommand::class

    override suspend fun handle(command: CreateCategoryCommand) {
        creator.create(command)
    }
}
