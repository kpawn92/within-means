package within.means.categories.application.search

import within.means.shared.domain.bus.query.Query

/**
 * Optional `kind` filter (EXPENSE / INCOME / TRANSFER). If null, all kinds.
 */
data class SearchCategoriesQuery(
    val kind: String? = null,
) : Query

class ListAllCategoriesQuery : Query
