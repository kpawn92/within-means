package within.means.analytics.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import within.means.analytics.AnalyticsTestEnv
import within.means.analytics.application.find_evolution.FindMonthlyEvolutionQuery
import within.means.analytics.application.find_evolution.FindMonthlyEvolutionQueryHandler

class FindMonthlyEvolutionQueryHandlerTest {

    @Test
    fun returns_contiguous_months_ending_in_the_current_one() = runTest {
        val env = AnalyticsTestEnv() // clock = 2026-05-27
        val salary = env.incomeCategory("Nómina")
        val food = env.expenseCategory("Comida")

        env.income(100000L, LocalDate(2026, 3, 1), salary)
        env.expense(40000L, LocalDate(2026, 3, 15), food)
        env.income(150000L, LocalDate(2026, 5, 1), salary)
        env.expense(60000L, LocalDate(2026, 5, 10), food)

        val handler = FindMonthlyEvolutionQueryHandler(
            env.transactions, clock = env.clock, timeZone = env.zone,
        )

        val r = handler.handle(FindMonthlyEvolutionQuery(monthsBack = 3))

        r.points shouldHaveSize 3
        r.points.map { it.yearMonth } shouldBe listOf("2026-03", "2026-04", "2026-05")
        r.points[0].totalIncomeCents shouldBe 100000L
        r.points[0].totalExpenseCents shouldBe 40000L
        r.points[1].totalIncomeCents shouldBe 0L
        r.points[1].totalExpenseCents shouldBe 0L
        r.points[2].totalIncomeCents shouldBe 150000L
        r.points[2].totalExpenseCents shouldBe 60000L
    }

    @Test
    fun handles_year_boundary_correctly() = runTest {
        val env = AnalyticsTestEnv() // clock = 2026-05
        val handler = FindMonthlyEvolutionQueryHandler(
            env.transactions, clock = env.clock, timeZone = env.zone,
        )

        val r = handler.handle(FindMonthlyEvolutionQuery(monthsBack = 12))

        r.points.first().yearMonth shouldBe "2025-06"
        r.points.last().yearMonth shouldBe "2026-05"
    }

    @Test
    fun rejects_out_of_range_monthsBack() {
        shouldThrow<IllegalArgumentException> { FindMonthlyEvolutionQuery(monthsBack = 0) }
        shouldThrow<IllegalArgumentException> { FindMonthlyEvolutionQuery(monthsBack = 37) }
    }
}
