package within.means.categories.application.reclassify

import within.means.categories.domain.CategoryClassifiers
import within.means.categories.domain.CategoryEssentiality
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryNature
import within.means.categories.domain.CategoryRepository
import within.means.categories.domain.EngelGroup
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import kotlin.reflect.KClass

class ReclassifyCategoryCommandHandler(
    private val repository: CategoryRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) : CommandHandler<ReclassifyCategoryCommand> {

    override val commandType: KClass<ReclassifyCategoryCommand> = ReclassifyCategoryCommand::class

    override suspend fun handle(command: ReclassifyCategoryCommand) {
        val id = CategoryId(command.categoryId)
        val category = repository.search(id) ?: error("Category not found: ${command.categoryId}")
        category.reclassify(
            CategoryClassifiers(
                nature = command.nature?.let { CategoryNature.valueOf(it) },
                essentiality = command.essentiality?.let { CategoryEssentiality.valueOf(it) },
                productive = command.productive,
                engelGroup = command.engelGroup?.let { EngelGroup.valueOf(it) },
            ),
            uuids,
        )
        repository.save(category)
        eventBus.publish(category.pullDomainEvents())
    }
}
