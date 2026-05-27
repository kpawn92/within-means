package within.means.categories.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import within.means.shared.domain.AggregateRoot
import within.means.shared.domain.UuidGenerator

class Category private constructor(
    val id: CategoryId,
    val kind: CategoryKind,
    name: CategoryName,
    color: CategoryColor,
    icon: CategoryIcon,
    classifiers: CategoryClassifiers,
    val parentId: CategoryId?,
    val createdAt: Instant,
) : AggregateRoot() {

    var name: CategoryName = name
        private set

    var color: CategoryColor = color
        private set

    var icon: CategoryIcon = icon
        private set

    var classifiers: CategoryClassifiers = classifiers
        private set

    fun rename(newName: CategoryName, uuids: UuidGenerator, clock: Clock = Clock.System) {
        if (this.name == newName) return
        this.name = newName
        record(
            CategoryRenamed(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
                newName = newName.value,
            )
        )
    }

    fun restyle(
        newColor: CategoryColor,
        newIcon: CategoryIcon,
        uuids: UuidGenerator,
        clock: Clock = Clock.System,
    ) {
        if (this.color == newColor && this.icon == newIcon) return
        this.color = newColor
        this.icon = newIcon
        record(
            CategoryRestyled(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
                newColor = newColor.value,
                newIcon = newIcon.value,
            )
        )
    }

    fun reclassify(
        newClassifiers: CategoryClassifiers,
        uuids: UuidGenerator,
        clock: Clock = Clock.System,
    ) {
        if (this.classifiers == newClassifiers) return
        this.classifiers = newClassifiers
        record(
            CategoryReclassified(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
                nature = newClassifiers.nature?.name,
                essentiality = newClassifiers.essentiality?.name,
                productive = newClassifiers.productive,
                engelGroup = newClassifiers.engelGroup?.name,
            )
        )
    }

    fun markDeleted(uuids: UuidGenerator, clock: Clock = Clock.System) {
        record(
            CategoryDeleted(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
            )
        )
    }

    companion object {

        fun create(
            id: CategoryId,
            kind: CategoryKind,
            name: CategoryName,
            color: CategoryColor,
            icon: CategoryIcon,
            classifiers: CategoryClassifiers = CategoryClassifiers.EMPTY,
            parentId: CategoryId? = null,
            uuids: UuidGenerator,
            clock: Clock = Clock.System,
        ): Category {
            requireClassifierConsistency(kind, classifiers)
            val now = clock.now()
            return Category(
                id = id,
                kind = kind,
                name = name,
                color = color,
                icon = icon,
                classifiers = classifiers,
                parentId = parentId,
                createdAt = now,
            ).apply {
                record(
                    CategoryCreated(
                        eventId = uuids.next(),
                        aggregateId = id.value,
                        occurredOn = now,
                        name = name.value,
                        color = color.value,
                        icon = icon.value,
                        kind = kind.name,
                        nature = classifiers.nature?.name,
                        essentiality = classifiers.essentiality?.name,
                        productive = classifiers.productive,
                        engelGroup = classifiers.engelGroup?.name,
                        parentId = parentId?.value,
                    )
                )
            }
        }

        fun rehydrate(
            id: CategoryId,
            kind: CategoryKind,
            name: CategoryName,
            color: CategoryColor,
            icon: CategoryIcon,
            classifiers: CategoryClassifiers,
            parentId: CategoryId?,
            createdAt: Instant,
        ): Category = Category(
            id = id,
            kind = kind,
            name = name,
            color = color,
            icon = icon,
            classifiers = classifiers,
            parentId = parentId,
            createdAt = createdAt,
        )

        private fun requireClassifierConsistency(kind: CategoryKind, c: CategoryClassifiers) {
            if (kind != CategoryKind.EXPENSE) {
                require(c.nature == null) { "$kind categories must not declare a nature" }
                require(c.essentiality == null) { "$kind categories must not declare essentiality" }
                require(c.engelGroup == null) { "$kind categories must not declare an Engel group" }
            }
        }
    }
}
