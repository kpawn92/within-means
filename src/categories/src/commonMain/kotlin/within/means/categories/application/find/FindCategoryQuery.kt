package within.means.categories.application.find

import within.means.shared.domain.bus.query.Query

data class FindCategoryQuery(val categoryId: String) : Query
