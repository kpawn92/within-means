package within.means.analytics.application.find_breakdown

import within.means.shared.domain.bus.query.Query

/**
 * Per-concept breakdown for a month (`YYYY-MM`). `type` defaults to EXPENSE —
 * it answers "¿cuánto gasté en cerveza / papá / el bus?".
 */
data class FindConceptBreakdownQuery(
    val yearMonth: String,
    val type: String = "EXPENSE",
) : Query

/** Per-concept breakdown over an inclusive `[startDate, endDate]` range. */
data class FindConceptBreakdownInRangeQuery(
    val startDate: String,
    val endDate: String,
    val type: String = "EXPENSE",
) : Query
