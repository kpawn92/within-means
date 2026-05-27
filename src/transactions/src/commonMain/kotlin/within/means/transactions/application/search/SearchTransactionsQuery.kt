package within.means.transactions.application.search

import within.means.shared.domain.bus.query.Query

/**
 * High-level convenience query over the transactions read side. Builds a
 * `Criteria` internally so callers don't have to know about the criteria
 * vocabulary. All fields are optional; the handler defaults to "all
 * transactions ordered by date desc" when nothing is supplied.
 */
data class SearchTransactionsQuery(
    val type: String? = null,
    val categoryId: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val amountMinCents: Long? = null,
    val amountMaxCents: Long? = null,
    val limit: Int? = null,
    val offset: Int? = null,
) : Query
