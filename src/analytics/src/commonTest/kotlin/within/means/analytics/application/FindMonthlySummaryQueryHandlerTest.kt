package within.means.analytics.application

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import within.means.analytics.AnalyticsTestEnv
import within.means.analytics.application.find_summary.FindCurrentMonthSummaryQuery
import within.means.analytics.application.find_summary.FindCurrentMonthSummaryQueryHandler
import within.means.analytics.application.find_summary.FindMonthlySummaryQuery
import within.means.analytics.application.find_summary.FindMonthlySummaryQueryHandler
import within.means.categories.domain.CategoryEssentiality
import within.means.categories.domain.CategoryNature

class FindMonthlySummaryQueryHandlerTest {

    @Test
    fun aggregates_totals_and_classifier_splits_for_the_requested_month() = runTest {
        val env = AnalyticsTestEnv()
        val rent = env.expenseCategory("Alquiler", CategoryNature.FIXED, CategoryEssentiality.ESSENTIAL)
        val food = env.expenseCategory("Comida", CategoryNature.VARIABLE, CategoryEssentiality.ESSENTIAL)
        val fun_ = env.expenseCategory("Ocio", CategoryNature.VARIABLE, CategoryEssentiality.DISCRETIONARY)
        val salary = env.incomeCategory("Nómina")

        env.income(200000L, LocalDate(2026, 5, 1), salary)
        env.expense(80000L, LocalDate(2026, 5, 1), rent)
        env.expense(15000L, LocalDate(2026, 5, 10), food)
        env.expense(5000L, LocalDate(2026, 5, 20), fun_)
        // Out-of-range — must be ignored.
        env.expense(99999L, LocalDate(2026, 4, 30), rent)

        val handler = FindMonthlySummaryQueryHandler(env.transactions, env.categories)

        val r = handler.handle(FindMonthlySummaryQuery("2026-05"))

        r.yearMonth shouldBe "2026-05"
        r.totalIncomeCents shouldBe 200000L
        r.totalExpenseCents shouldBe 100000L
        r.balanceCents shouldBe 100000L
        r.fixedExpenseCents shouldBe 80000L
        r.variableExpenseCents shouldBe 20000L
        r.essentialExpenseCents shouldBe 95000L
        r.discretionaryExpenseCents shouldBe 5000L
    }

    @Test
    fun returns_all_zeros_when_no_transactions_match_the_month() = runTest {
        val env = AnalyticsTestEnv()
        val handler = FindMonthlySummaryQueryHandler(env.transactions, env.categories)

        val r = handler.handle(FindMonthlySummaryQuery("2026-05"))

        r.totalIncomeCents shouldBe 0L
        r.totalExpenseCents shouldBe 0L
        r.balanceCents shouldBe 0L
    }

    @Test
    fun current_month_query_resolves_via_the_clock() = runTest {
        val env = AnalyticsTestEnv()
        val rent = env.expenseCategory("Alquiler", CategoryNature.FIXED, CategoryEssentiality.ESSENTIAL)
        env.expense(50000L, LocalDate(2026, 5, 15), rent)

        val handler = FindCurrentMonthSummaryQueryHandler(
            env.transactions, env.categories, env.clock, env.zone,
        )

        val r = handler.handle(FindCurrentMonthSummaryQuery())

        r.yearMonth shouldBe "2026-05"
        r.totalExpenseCents shouldBe 50000L
    }
}
