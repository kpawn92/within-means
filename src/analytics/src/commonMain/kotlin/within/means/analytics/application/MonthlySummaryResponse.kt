package within.means.analytics.application

import within.means.shared.domain.bus.query.Response

/**
 * Aggregate view for a single calendar month, expressed as integer cents
 * of the user's base currency. The fixed/variable and essential/discretionary
 * splits apply only to expenses; income transactions don't carry those
 * classifiers in the MVP.
 */
data class MonthlySummaryResponse(
    val yearMonth: String,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
    val balanceCents: Long,
    val fixedExpenseCents: Long,
    val variableExpenseCents: Long,
    val essentialExpenseCents: Long,
    val discretionaryExpenseCents: Long,
) : Response
