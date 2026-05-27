package within.means.analytics.application

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import within.means.analytics.AnalyticsTestEnv
import within.means.analytics.application.find_breakdown.FindCategoryBreakdownQuery
import within.means.analytics.application.find_breakdown.FindCategoryBreakdownQueryHandler

class FindCategoryBreakdownQueryHandlerTest {

    @Test
    fun aggregates_expenses_by_category_sorted_desc_with_share() = runTest {
        val env = AnalyticsTestEnv()
        val rent = env.expenseCategory("Alquiler")
        val food = env.expenseCategory("Comida")

        env.expense(80000L, LocalDate(2026, 5, 1), rent)
        env.expense(15000L, LocalDate(2026, 5, 10), food)
        env.expense(5000L, LocalDate(2026, 5, 12), food)

        val handler = FindCategoryBreakdownQueryHandler(env.transactions, env.categories)

        val r = handler.handle(FindCategoryBreakdownQuery(yearMonth = "2026-05"))

        r.type shouldBe "EXPENSE"
        r.totalCents shouldBe 100000L
        r.items.size shouldBe 2
        r.items.first().categoryName shouldBe "Alquiler"
        r.items.first().totalCents shouldBe 80000L
        r.items.first().share shouldBe (0.80 plusOrMinus 1e-9)
        r.items.last().categoryName shouldBe "Comida"
        r.items.last().totalCents shouldBe 20000L
    }

    @Test
    fun ignores_transactions_of_other_type_or_other_month() = runTest {
        val env = AnalyticsTestEnv()
        val food = env.expenseCategory("Comida")
        val salary = env.incomeCategory("Nómina")
        env.income(200000L, LocalDate(2026, 5, 1), salary)
        env.expense(100L, LocalDate(2026, 4, 1), food)
        env.expense(500L, LocalDate(2026, 5, 1), food)

        val r = FindCategoryBreakdownQueryHandler(env.transactions, env.categories)
            .handle(FindCategoryBreakdownQuery(yearMonth = "2026-05"))

        r.totalCents shouldBe 500L
        r.items.single().categoryId shouldBe food
    }

    @Test
    fun empty_when_no_transactions_in_the_month() = runTest {
        val env = AnalyticsTestEnv()
        val r = FindCategoryBreakdownQueryHandler(env.transactions, env.categories)
            .handle(FindCategoryBreakdownQuery(yearMonth = "2026-05"))

        r.totalCents shouldBe 0L
        r.items.shouldBeEmpty()
    }
}
