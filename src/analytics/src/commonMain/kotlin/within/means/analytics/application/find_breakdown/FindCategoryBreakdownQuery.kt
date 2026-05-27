package within.means.analytics.application.find_breakdown

import within.means.shared.domain.bus.query.Query

/**
 * `yearMonth` as `YYYY-MM`. `type` defaults to EXPENSE — it's the breakdown
 * that answers "en qué se va el dinero".
 */
data class FindCategoryBreakdownQuery(
    val yearMonth: String,
    val type: String = "EXPENSE",
) : Query
