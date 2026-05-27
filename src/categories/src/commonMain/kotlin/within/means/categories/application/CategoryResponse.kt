package within.means.categories.application

import within.means.shared.domain.bus.query.Response

data class CategoryResponse(
    val id: String,
    val kind: String,
    val name: String,
    val color: String,
    val icon: String,
    val nature: String?,
    val essentiality: String?,
    val productive: Boolean,
    val engelGroup: String?,
    val parentId: String?,
    val createdAt: String,
) : Response

data class CategoriesResponse(val items: List<CategoryResponse>) : Response

data class OptionalCategoryResponse(val category: CategoryResponse?) : Response
