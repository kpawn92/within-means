package within.means.analytics.application.find_summary

import within.means.shared.domain.bus.query.Query

/** `yearMonth` formatted as `YYYY-MM`. */
data class FindMonthlySummaryQuery(val yearMonth: String) : Query

/** Convenience: resolves the current calendar month at handle time. */
class FindCurrentMonthSummaryQuery : Query

/**
 * Aggregates the same totals as [FindMonthlySummaryQuery] but over an
 * arbitrary inclusive `[startDate, endDate]` range (ISO `YYYY-MM-DD`),
 * which lets the Analysis screen switch between week / month / year.
 */
data class FindSummaryInRangeQuery(val startDate: String, val endDate: String) : Query
