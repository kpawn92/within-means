package within.means.analytics.application.find_breakdown

import within.means.analytics.application.CategoryBreakdownItem
import within.means.analytics.application.CategoryBreakdownResponse
import within.means.analytics.application.YearMonth
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.bus.query.QueryHandler
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
        val categoriesById = categories.all().associateBy { it.id.value }

        val totalsByCategory: Map<String, Long> = transactions.all()
            .asSequence()
            .filter { it.type == type && YearMonth.of(it.date.value) == ym }
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
            yearMonth = ym.text,
            type = type.name,
            totalCents = total,
            items = items,
        )
    }
}
