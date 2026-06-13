package within.means.concepts.application.find

import within.means.concepts.application.OptionalConceptResponse
import within.means.concepts.application.toResponse
import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptRepository
import within.means.shared.domain.bus.query.QueryHandler
import kotlin.reflect.KClass

class FindConceptQueryHandler(
    private val repository: ConceptRepository,
) : QueryHandler<FindConceptQuery, OptionalConceptResponse> {

    override val queryType: KClass<FindConceptQuery> = FindConceptQuery::class

    override suspend fun handle(query: FindConceptQuery): OptionalConceptResponse {
        val concept = repository.search(ConceptId(query.conceptId))
        return OptionalConceptResponse(concept = concept?.toResponse())
    }
}
