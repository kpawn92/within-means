package within.means.concepts.infrastructure.persistence

import kotlinx.datetime.Instant
import within.means.concepts.db.Concept as ConceptRow
import within.means.concepts.domain.Concept
import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptKind
import within.means.concepts.domain.ConceptLabel

internal fun ConceptRow.toAggregate(): Concept = Concept.rehydrate(
    id = ConceptId(id),
    kind = ConceptKind.valueOf(kind),
    label = ConceptLabel(label),
    defaultCategoryId = default_category_id,
    usageCount = usage_count.toInt(),
    lastUsedAt = last_used_at?.let { Instant.parse(it) },
    createdAt = Instant.parse(created_at),
)
