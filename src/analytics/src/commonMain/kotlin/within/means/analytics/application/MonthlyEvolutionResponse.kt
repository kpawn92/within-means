package within.means.analytics.application

import within.means.shared.domain.bus.query.Response

data class MonthlyEvolutionPoint(
    val yearMonth: String,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
)

data class MonthlyEvolutionResponse(
    val points: List<MonthlyEvolutionPoint>,
) : Response
