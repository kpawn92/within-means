package within.means.analytics.application

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import within.means.analytics.AnalyticsTestEnv
import within.means.analytics.application.find_summary.FindSummaryInRangeQuery
import within.means.analytics.application.find_summary.FindSummaryInRangeQueryHandler

class FindSummaryInRangeQueryHandlerTest {

    @Test
    fun aggregates_only_the_transactions_inside_the_inclusive_range() = runTest {
        val env = AnalyticsTestEnv()
        val food = env.expenseCategory("Comida")
        val salary = env.incomeCategory("Nómina")

        env.income(200000L, LocalDate(2026, 5, 1), salary)
        env.expense(1000L, LocalDate(2026, 5, 3), food)   // inside week
        env.expense(2000L, LocalDate(2026, 5, 9), food)   // inside week (boundary)
        env.expense(9999L, LocalDate(2026, 5, 20), food)  // outside week, inside month

        val handler = FindSummaryInRangeQueryHandler(env.transactions, env.categories)

        // Week range 2026-05-03..2026-05-09.
        val week = handler.handle(FindSummaryInRangeQuery("2026-05-03", "2026-05-09"))
        week.totalExpenseCents shouldBe 3000L
        week.totalIncomeCents shouldBe 0L

        // Full month picks up everything.
        val month = handler.handle(FindSummaryInRangeQuery("2026-05-01", "2026-05-31"))
        month.totalExpenseCents shouldBe 12999L
        month.totalIncomeCents shouldBe 200000L
        month.balanceCents shouldBe 187001L
    }
}
