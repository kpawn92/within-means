package within.means.categories.application.create

import within.means.categories.domain.Category
import within.means.categories.domain.CategoryClassifiers
import within.means.categories.domain.CategoryColor
import within.means.categories.domain.CategoryEssentiality
import within.means.categories.domain.CategoryIcon
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryKind
import within.means.categories.domain.CategoryName
import within.means.categories.domain.CategoryNature
import within.means.categories.domain.CategoryRepository
import within.means.categories.domain.EngelGroup
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.event.EventBus

class CategoryCreator(
    private val repository: CategoryRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) {
    suspend fun create(command: CreateCategoryCommand): CategoryId {
        val kind = CategoryKind.valueOf(command.kind)
        val classifiers = CategoryClassifiers(
            nature = command.nature?.let { CategoryNature.valueOf(it) },
            essentiality = command.essentiality?.let { CategoryEssentiality.valueOf(it) },
            productive = command.productive,
            engelGroup = command.engelGroup?.let { EngelGroup.valueOf(it) },
        )
        val id = command.id?.let { CategoryId(it) } ?: CategoryId(uuids.next())

        val category = Category.create(
            id = id,
            kind = kind,
            name = CategoryName(command.name),
            color = CategoryColor(command.color),
            icon = CategoryIcon(command.icon),
            classifiers = classifiers,
            parentId = command.parentId?.let { CategoryId(it) },
            uuids = uuids,
        )

        repository.save(category)
        eventBus.publish(category.pullDomainEvents())
        return id
    }
}
