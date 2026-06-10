package within.means.analytics.application

import within.means.shared.domain.bus.query.Response

data class MonthlyEvolutionPoint(
    /** Bucket key (ISO month / week-start date / year); kept for ordering & tests. */
    val yearMonth: String,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
    /** Short human label for the bar (e.g. "may", "12/5", "2026"). */
    val label: String = "",
)

/** Bucket size for the evolution trend. */
enum class EvolutionGranularity { WEEK, MONTH, YEAR }

data class MonthlyEvolutionResponse(
    val points: List<MonthlyEvolutionPoint>,
) : Response
