package within.means.analytics.application

import within.means.shared.domain.bus.query.Response

data class ConceptBreakdownItem(
    val conceptId: String,
    val conceptLabel: String,
    val totalCents: Long,
    /** Fraction of the period total (which is partitioned by *category*), 0.0..1.0+. */
    val share: Double,
)

/**
 * Per-concept spend over a period. **Not a partition**: a movement with several
 * concepts counts its full amount under each, so the item totals can exceed
 * [totalCents] (the real period total) and the shares can sum past 1.0. The UI
 * must say so (CONCEPTS-SPEC §5.2). The category breakdown stays the 100% view.
 */
data class ConceptBreakdownResponse(
    val periodLabel: String,
    val type: String,
    val totalCents: Long,
    val items: List<ConceptBreakdownItem>,
) : Response
