package within.means.analytics.application.find_summary

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import within.means.analytics.application.MonthlySummaryResponse
import within.means.analytics.application.YearMonth
import within.means.categories.domain.CategoryEssentiality
import within.means.categories.domain.CategoryNature
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.bus.query.QueryHandler
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionRepository
import within.means.transactions.domain.TransactionType
import kotlin.reflect.KClass

class FindMonthlySummaryQueryHandler(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
) : QueryHandler<FindMonthlySummaryQuery, MonthlySummaryResponse> {

    override val queryType: KClass<FindMonthlySummaryQuery> = FindMonthlySummaryQuery::class

    override suspend fun handle(query: FindMonthlySummaryQuery): MonthlySummaryResponse {
        val ym = YearMonth.parse(query.yearMonth)
        return summarize(ym, transactions, categories)
    }
}

class FindCurrentMonthSummaryQueryHandler(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : QueryHandler<FindCurrentMonthSummaryQuery, MonthlySummaryResponse> {

    override val queryType: KClass<FindCurrentMonthSummaryQuery> = FindCurrentMonthSummaryQuery::class

    override suspend fun handle(query: FindCurrentMonthSummaryQuery): MonthlySummaryResponse {
        val today = clock.now().toLocalDateTime(timeZone).date
        val ym = YearMonth.of(today)
        return summarize(ym, transactions, categories)
    }
}

internal suspend fun summarize(
    ym: YearMonth,
    transactions: TransactionRepository,
    categories: CategoryRepository,
): MonthlySummaryResponse {
    val txs = transactions.all().filter { YearMonth.of(it.date.value) == ym }
    val categoriesById = categories.all().associateBy { it.id.value }

    var income = 0L
    var expense = 0L
    var fixed = 0L
    var variable = 0L
    var essential = 0L
    var discretionary = 0L

    for (tx: Transaction in txs) {
        when (tx.type) {
            TransactionType.INCOME -> income += tx.amount.cents
            TransactionType.EXPENSE -> {
                expense += tx.amount.cents
                val classifiers = categoriesById[tx.categoryRef.value]?.classifiers
                when (classifiers?.nature) {
                    CategoryNature.FIXED -> fixed += tx.amount.cents
                    CategoryNature.VARIABLE -> variable += tx.amount.cents
                    null -> Unit
                }
                when (classifiers?.essentiality) {
                    CategoryEssentiality.ESSENTIAL -> essential += tx.amount.cents
                    CategoryEssentiality.DISCRETIONARY -> discretionary += tx.amount.cents
                    null -> Unit
                }
            }
        }
    }

    return MonthlySummaryResponse(
        yearMonth = ym.text,
        totalIncomeCents = income,
        totalExpenseCents = expense,
        balanceCents = income - expense,
        fixedExpenseCents = fixed,
        variableExpenseCents = variable,
        essentialExpenseCents = essential,
        discretionaryExpenseCents = discretionary,
    )
}
