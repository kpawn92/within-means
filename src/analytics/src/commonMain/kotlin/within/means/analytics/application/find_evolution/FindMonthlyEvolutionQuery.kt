package within.means.analytics.application.find_evolution

import within.means.analytics.application.EvolutionGranularity
import within.means.shared.domain.bus.query.Query

/**
 * Returns the last [monthsBack] buckets of size [granularity] ending in the
 * current one (inclusive). Despite the legacy `monthsBack` name it counts
 * buckets, so with [EvolutionGranularity.WEEK] it means "weeks back".
 */
data class FindMonthlyEvolutionQuery(
    val monthsBack: Int = 6,
    val granularity: EvolutionGranularity = EvolutionGranularity.MONTH,
) : Query {
    init {
        require(monthsBack in 1..36) { "monthsBack must be 1..36 (was $monthsBack)" }
    }
}
