package within.means.categories.application

import within.means.categories.domain.Category

fun Category.toResponse(): CategoryResponse = CategoryResponse(
    id = id.value,
    kind = kind.name,
    name = name.value,
    color = color.value,
    icon = icon.value,
    nature = classifiers.nature?.name,
    essentiality = classifiers.essentiality?.name,
    productive = classifiers.productive,
    engelGroup = classifiers.engelGroup?.name,
    parentId = parentId?.value,
    createdAt = createdAt.toString(),
)
