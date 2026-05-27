package within.means.categories.application.delete

import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import kotlin.reflect.KClass

class DeleteCategoryCommandHandler(
    private val repository: CategoryRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) : CommandHandler<DeleteCategoryCommand> {

    override val commandType: KClass<DeleteCategoryCommand> = DeleteCategoryCommand::class

    override suspend fun handle(command: DeleteCategoryCommand) {
        val id = CategoryId(command.categoryId)
        val category = repository.search(id) ?: return
        category.markDeleted(uuids)
        repository.delete(id)
        eventBus.publish(category.pullDomainEvents())
    }
}
