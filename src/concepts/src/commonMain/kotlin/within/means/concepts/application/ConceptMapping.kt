package within.means.concepts.application

import within.means.concepts.domain.Concept

fun Concept.toResponse(): ConceptResponse = ConceptResponse(
    id = id.value,
    kind = kind.name,
    label = label.value,
    key = key.value,
    defaultCategoryId = defaultCategoryId,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt?.toString(),
    createdAt = createdAt.toString(),
)
