package within.means.transactions.application.search

import within.means.shared.domain.bus.query.QueryHandler
import within.means.shared.domain.criteria.Criteria
import within.means.shared.domain.criteria.Filter
import within.means.shared.domain.criteria.FilterField
import within.means.shared.domain.criteria.FilterOperator
import within.means.shared.domain.criteria.FilterValue
import within.means.shared.domain.criteria.Filters
import within.means.shared.domain.criteria.Order
import within.means.transactions.application.TransactionsResponse
import within.means.transactions.application.toResponse
import within.means.transactions.domain.TransactionRepository
import kotlin.reflect.KClass

class SearchTransactionsQueryHandler(
    private val repository: TransactionRepository,
) : QueryHandler<SearchTransactionsQuery, TransactionsResponse> {

    override val queryType: KClass<SearchTransactionsQuery> = SearchTransactionsQuery::class

    override suspend fun handle(query: SearchTransactionsQuery): TransactionsResponse {
        val filters = buildList {
            query.type?.let {
                add(Filter(FilterField("type"), FilterOperator.EQUALS, FilterValue(it)))
            }
            query.categoryId?.let {
                add(Filter(FilterField("categoryId"), FilterOperator.EQUALS, FilterValue(it)))
            }
            query.conceptId?.let {
                add(Filter(FilterField("conceptId"), FilterOperator.EQUALS, FilterValue(it)))
            }
            query.dateFrom?.let {
                add(Filter(FilterField("date"), FilterOperator.GTE, FilterValue(it)))
            }
            query.dateTo?.let {
                add(Filter(FilterField("date"), FilterOperator.LTE, FilterValue(it)))
            }
            query.amountMinCents?.let {
                add(Filter(FilterField("amount"), FilterOperator.GTE, FilterValue(it.toString())))
            }
            query.amountMaxCents?.let {
                add(Filter(FilterField("amount"), FilterOperator.LTE, FilterValue(it.toString())))
            }
        }

        val criteria = Criteria(
            filters = Filters(filters),
            order = Order.desc("date"),
            limit = query.limit,
            offset = query.offset,
        )

        return TransactionsResponse(repository.matching(criteria).map { it.toResponse() })
    }
}
