package within.means.analytics.application.find_summary

import within.means.shared.domain.bus.query.Query

/** `yearMonth` formatted as `YYYY-MM`. */
data class FindMonthlySummaryQuery(val yearMonth: String) : Query

/** Convenience: resolves the current calendar month at handle time. */
class FindCurrentMonthSummaryQuery : Query
