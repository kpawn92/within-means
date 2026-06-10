package within.means.analytics.application.find_evolution

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import within.means.analytics.application.EvolutionGranularity
import within.means.analytics.application.MonthlyEvolutionPoint
import within.means.analytics.application.MonthlyEvolutionResponse
import within.means.analytics.application.YearMonth
import within.means.shared.domain.bus.query.QueryHandler
import within.means.transactions.domain.TransactionRepository
import within.means.transactions.domain.TransactionType
import kotlin.reflect.KClass

/** One contiguous bucket of the evolution trend. */
private data class Bucket(val key: String, val label: String, val start: LocalDate, val end: LocalDate)

class FindMonthlyEvolutionQueryHandler(
    private val transactions: TransactionRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : QueryHandler<FindMonthlyEvolutionQuery, MonthlyEvolutionResponse> {

    override val queryType: KClass<FindMonthlyEvolutionQuery> = FindMonthlyEvolutionQuery::class

    override suspend fun handle(query: FindMonthlyEvolutionQuery): MonthlyEvolutionResponse {
        val today = clock.now().toLocalDateTime(timeZone).date
        val buckets = bucketsEndingAt(today, query.monthsBack, query.granularity)
        val txs = transactions.all()

        val points = buckets.map { b ->
            val inBucket = txs.filter { it.date.value in b.start..b.end }
            MonthlyEvolutionPoint(
                yearMonth = b.key,
                totalIncomeCents = inBucket.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.cents },
                totalExpenseCents = inBucket.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.cents },
                label = b.label,
            )
        }
        return MonthlyEvolutionResponse(points = points)
    }

    /**
     * [count] contiguous buckets of size [granularity], oldest first, ending at
     * the one that contains [today].
     */
    private fun bucketsEndingAt(today: LocalDate, count: Int, granularity: EvolutionGranularity): List<Bucket> =
        (count - 1 downTo 0).map { back ->
            when (granularity) {
                EvolutionGranularity.MONTH -> {
                    val ym = YearMonth.of(today).minus(back)
                    val first = LocalDate(ym.year, ym.month, 1)
                    val end = first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
                    Bucket(ym.text, MONTHS_SHORT[ym.month - 1], first, end)
                }
                EvolutionGranularity.WEEK -> {
                    val thisMonday = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
                    val monday = thisMonday.minus(DatePeriod(days = 7 * back))
                    Bucket(
                        key = monday.toString(),
                        label = "${monday.dayOfMonth}/${monday.monthNumber}",
                        start = monday,
                        end = monday.plus(DatePeriod(days = 6)),
                    )
                }
                EvolutionGranularity.YEAR -> {
                    val year = today.year - back
                    Bucket(year.toString(), year.toString(), LocalDate(year, 1, 1), LocalDate(year, 12, 31))
                }
            }
        }

    private companion object {
        val MONTHS_SHORT = listOf(
            "ene", "feb", "mar", "abr", "may", "jun",
            "jul", "ago", "sep", "oct", "nov", "dic",
        )
    }
}
