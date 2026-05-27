package within.means.categories.application.find

import within.means.categories.application.OptionalCategoryResponse
import within.means.categories.application.toResponse
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.bus.query.QueryHandler
import kotlin.reflect.KClass

class FindCategoryQueryHandler(
    private val repository: CategoryRepository,
) : QueryHandler<FindCategoryQuery, OptionalCategoryResponse> {

    override val queryType: KClass<FindCategoryQuery> = FindCategoryQuery::class

    override suspend fun handle(query: FindCategoryQuery): OptionalCategoryResponse {
        val category = repository.search(CategoryId(query.categoryId))
        return OptionalCategoryResponse(category = category?.toResponse())
    }
}
