package within.means.categories.infrastructure.persistence

import kotlinx.datetime.Instant
import within.means.categories.db.Category as CategoryRow
import within.means.categories.domain.Category
import within.means.categories.domain.CategoryClassifiers
import within.means.categories.domain.CategoryColor
import within.means.categories.domain.CategoryEssentiality
import within.means.categories.domain.CategoryIcon
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryKind
import within.means.categories.domain.CategoryName
import within.means.categories.domain.CategoryNature
import within.means.categories.domain.EngelGroup

internal fun CategoryRow.toAggregate(): Category = Category.rehydrate(
    id = CategoryId(id),
    kind = CategoryKind.valueOf(kind),
    name = CategoryName(name),
    color = CategoryColor(color),
    icon = CategoryIcon(icon),
    classifiers = CategoryClassifiers(
        nature = nature?.let { CategoryNature.valueOf(it) },
        essentiality = essentiality?.let { CategoryEssentiality.valueOf(it) },
        productive = productive != 0L,
        engelGroup = engel_group?.let { EngelGroup.valueOf(it) },
    ),
    parentId = parent_id?.let { CategoryId(it) },
    createdAt = Instant.parse(created_at),
)
