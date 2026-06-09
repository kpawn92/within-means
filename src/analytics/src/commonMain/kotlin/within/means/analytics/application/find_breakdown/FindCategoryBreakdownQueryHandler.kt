package within.means.analytics.application.find_breakdown

import kotlinx.datetime.LocalDate
import within.means.analytics.application.CategoryBreakdownItem
import within.means.analytics.application.CategoryBreakdownResponse
import within.means.analytics.application.YearMonth
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.bus.query.QueryHandler
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionRepository
import within.means.transactions.domain.TransactionType
import kotlin.reflect.KClass

class FindCategoryBreakdownQueryHandler(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
) : QueryHandler<FindCategoryBreakdownQuery, CategoryBreakdownResponse> {

    override val queryType: KClass<FindCategoryBreakdownQuery> = FindCategoryBreakdownQuery::class

    override suspend fun handle(query: FindCategoryBreakdownQuery): CategoryBreakdownResponse {
        val ym = YearMonth.parse(query.yearMonth)
        val type = TransactionType.valueOf(query.type)
        return breakdown(
            label = ym.text,
            type = type,
            matching = { YearMonth.of(it.date.value) == ym },
            transactions = transactions,
            categories = categories,
        )
    }
}

class FindBreakdownInRangeQueryHandler(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
) : QueryHandler<FindBreakdownInRangeQuery, CategoryBreakdownResponse> {

    override val queryType: KClass<FindBreakdownInRangeQuery> = FindBreakdownInRangeQuery::class

    override suspend fun handle(query: FindBreakdownInRangeQuery): CategoryBreakdownResponse {
        val start = LocalDate.parse(query.startDate)
        val end = LocalDate.parse(query.endDate)
        val type = TransactionType.valueOf(query.type)
        return breakdown(
            label = query.startDate,
            type = type,
            matching = { it.date.value in start..end },
            transactions = transactions,
            categories = categories,
        )
    }
}

private suspend fun breakdown(
    label: String,
    type: TransactionType,
    matching: (Transaction) -> Boolean,
    transactions: TransactionRepository,
    categories: CategoryRepository,
): CategoryBreakdownResponse {
    val categoriesById = categories.all().associateBy { it.id.value }

    val totalsByCategory: Map<String, Long> = transactions.all()
        .asSequence()
        .filter { it.type == type && matching(it) }
        .groupBy { it.categoryRef.value }
        .mapValues { (_, txs) -> txs.sumOf { it.amount.cents } }

    val total = totalsByCategory.values.sum()
    val items = totalsByCategory
        .map { (categoryId, cents) ->
            val category = categoriesById[categoryId]
            CategoryBreakdownItem(
                categoryId = categoryId,
                categoryName = category?.name?.value ?: "(desconocida)",
                color = category?.color?.value ?: "#888888",
                totalCents = cents,
                share = if (total > 0L) cents.toDouble() / total else 0.0,
            )
        }
        .sortedByDescending { it.totalCents }

    return CategoryBreakdownResponse(
        yearMonth = label,
        type = type.name,
        totalCents = total,
        items = items,
    )
}
