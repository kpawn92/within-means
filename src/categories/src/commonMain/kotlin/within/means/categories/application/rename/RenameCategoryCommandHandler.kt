package within.means.categories.application.rename

import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryName
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import kotlin.reflect.KClass

class RenameCategoryCommandHandler(
    private val repository: CategoryRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) : CommandHandler<RenameCategoryCommand> {

    override val commandType: KClass<RenameCategoryCommand> = RenameCategoryCommand::class

    override suspend fun handle(command: RenameCategoryCommand) {
        val id = CategoryId(command.categoryId)
        val category = repository.search(id) ?: error("Category not found: ${command.categoryId}")
        category.rename(CategoryName(command.newName), uuids)
        repository.save(category)
        eventBus.publish(category.pullDomainEvents())
    }
}
