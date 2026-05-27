package within.means.analytics.application.find_evolution

import within.means.shared.domain.bus.query.Query

/** Returns the last `monthsBack` months ending in the current month (inclusive). */
data class FindMonthlyEvolutionQuery(val monthsBack: Int = 6) : Query {
    init {
        require(monthsBack in 1..36) { "monthsBack must be 1..36 (was $monthsBack)" }
    }
}
