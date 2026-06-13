package within.means.concepts.application

import within.means.shared.domain.bus.query.Response

data class ConceptResponse(
    val id: String,
    val kind: String,
    val label: String,
    val key: String,
    val defaultCategoryId: String?,
    val usageCount: Int,
    val lastUsedAt: String?,
    val createdAt: String,
) : Response

data class ConceptsResponse(val items: List<ConceptResponse>) : Response

data class OptionalConceptResponse(val concept: ConceptResponse?) : Response
