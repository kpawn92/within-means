package within.means.analytics.application.find_evolution

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import within.means.analytics.application.MonthlyEvolutionPoint
import within.means.analytics.application.MonthlyEvolutionResponse
import within.means.analytics.application.YearMonth
import within.means.shared.domain.bus.query.QueryHandler
import within.means.transactions.domain.TransactionRepository
import within.means.transactions.domain.TransactionType
import kotlin.reflect.KClass

class FindMonthlyEvolutionQueryHandler(
    private val transactions: TransactionRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : QueryHandler<FindMonthlyEvolutionQuery, MonthlyEvolutionResponse> {

    override val queryType: KClass<FindMonthlyEvolutionQuery> = FindMonthlyEvolutionQuery::class

    override suspend fun handle(query: FindMonthlyEvolutionQuery): MonthlyEvolutionResponse {
        val today = clock.now().toLocalDateTime(timeZone).date
        val currentYm = YearMonth.of(today)

        // Build the contiguous range [currentYm - (monthsBack-1) .. currentYm].
        val months: List<YearMonth> = (query.monthsBack - 1 downTo 0)
            .map { currentYm.minus(it) }

        val txByYm = transactions.all().groupBy { YearMonth.of(it.date.value) }

        val points = months.map { ym ->
            val txs = txByYm[ym].orEmpty()
            val income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.cents }
            val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.cents }
            MonthlyEvolutionPoint(
                yearMonth = ym.text,
                totalIncomeCents = income,
                totalExpenseCents = expense,
            )
        }

        return MonthlyEvolutionResponse(points = points)
    }
}
