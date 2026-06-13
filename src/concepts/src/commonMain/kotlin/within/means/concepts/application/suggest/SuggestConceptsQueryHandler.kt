package within.means.concepts.application.suggest

import within.means.concepts.application.ConceptsResponse
import within.means.concepts.application.toResponse
import within.means.concepts.domain.ConceptKey
import within.means.concepts.domain.ConceptKind
import within.means.concepts.domain.ConceptRepository
import within.means.shared.domain.bus.query.QueryHandler
import kotlin.reflect.KClass

class SuggestConceptsQueryHandler(
    private val repository: ConceptRepository,
) : QueryHandler<SuggestConceptsQuery, ConceptsResponse> {

    override val queryType: KClass<SuggestConceptsQuery> = SuggestConceptsQuery::class

    override suspend fun handle(query: SuggestConceptsQuery): ConceptsResponse {
        val kind = ConceptKind.valueOf(query.kind)
        val all = repository.byKind(kind) // already most-used first

        val filtered = query.prefix
            ?.takeIf { it.isNotBlank() }
            ?.let { raw ->
                // Match on the same normalized form used to store the key, so
                // "cerv", "Cerv " and "çerv" all narrow to "cerveza". A prefix
                // that normalizes to nothing (e.g. only emoji) doesn't filter.
                val needle = runCatching { ConceptKey.of(raw).value }.getOrNull()
                if (needle == null) all else all.filter { it.key.value.startsWith(needle) }
            }
            ?: all

        return ConceptsResponse(filtered.take(query.limit).map { it.toResponse() })
    }
}

class ListAllConceptsQueryHandler(
    private val repository: ConceptRepository,
) : QueryHandler<ListAllConceptsQuery, ConceptsResponse> {

    override val queryType: KClass<ListAllConceptsQuery> = ListAllConceptsQuery::class

    override suspend fun handle(query: ListAllConceptsQuery): ConceptsResponse {
        val kind = ConceptKind.valueOf(query.kind)
        return ConceptsResponse(repository.byKind(kind).map { it.toResponse() })
    }
}
