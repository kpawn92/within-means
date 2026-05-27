package within.means.analytics.application

import within.means.shared.domain.bus.query.Response

data class CategoryBreakdownItem(
    val categoryId: String,
    val categoryName: String,
    val color: String,
    val totalCents: Long,
    /** Fraction of the period total, 0.0..1.0. */
    val share: Double,
)

data class CategoryBreakdownResponse(
    val yearMonth: String,
    val type: String,
    val totalCents: Long,
    val items: List<CategoryBreakdownItem>,
) : Response
