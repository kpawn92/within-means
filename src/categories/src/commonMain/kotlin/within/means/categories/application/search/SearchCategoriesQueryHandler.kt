package within.means.categories.application.search

import within.means.categories.application.CategoriesResponse
import within.means.categories.application.toResponse
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.bus.query.QueryHandler
import within.means.shared.domain.criteria.Criteria
import within.means.shared.domain.criteria.Filter
import within.means.shared.domain.criteria.FilterField
import within.means.shared.domain.criteria.FilterOperator
import within.means.shared.domain.criteria.FilterValue
import within.means.shared.domain.criteria.Filters
import within.means.shared.domain.criteria.Order
import kotlin.reflect.KClass

class SearchCategoriesQueryHandler(
    private val repository: CategoryRepository,
) : QueryHandler<SearchCategoriesQuery, CategoriesResponse> {

    override val queryType: KClass<SearchCategoriesQuery> = SearchCategoriesQuery::class

    override suspend fun handle(query: SearchCategoriesQuery): CategoriesResponse {
        val filters = if (query.kind != null) {
            Filters.of(Filter(FilterField("kind"), FilterOperator.EQUALS, FilterValue(query.kind)))
        } else {
            Filters.none()
        }
        val criteria = Criteria(filters = filters, order = Order.asc("name"))
        return CategoriesResponse(repository.matching(criteria).map { it.toResponse() })
    }
}

class ListAllCategoriesQueryHandler(
    private val repository: CategoryRepository,
) : QueryHandler<ListAllCategoriesQuery, CategoriesResponse> {

    override val queryType: KClass<ListAllCategoriesQuery> = ListAllCategoriesQuery::class

    override suspend fun handle(query: ListAllCategoriesQuery): CategoriesResponse =
        CategoriesResponse(repository.all().map { it.toResponse() })
}
