package within.means.categories.application.recolor

import within.means.categories.domain.CategoryColor
import within.means.categories.domain.CategoryIcon
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import kotlin.reflect.KClass

class RestyleCategoryCommandHandler(
    private val repository: CategoryRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) : CommandHandler<RestyleCategoryCommand> {

    override val commandType: KClass<RestyleCategoryCommand> = RestyleCategoryCommand::class

    override suspend fun handle(command: RestyleCategoryCommand) {
        val id = CategoryId(command.categoryId)
        val category = repository.search(id) ?: error("Category not found: ${command.categoryId}")
        category.restyle(CategoryColor(command.newColor), CategoryIcon(command.newIcon), uuids)
        repository.save(category)
        eventBus.publish(category.pullDomainEvents())
    }
}
