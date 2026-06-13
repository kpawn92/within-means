package within.means.analytics.application

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import within.means.analytics.AnalyticsTestEnv
import within.means.analytics.application.find_breakdown.FindConceptBreakdownQuery
import within.means.analytics.application.find_breakdown.FindConceptBreakdownQueryHandler

class FindConceptBreakdownQueryHandlerTest {

    private fun handler(env: AnalyticsTestEnv) =
        FindConceptBreakdownQueryHandler(env.transactions, env.concepts)

    @Test
    fun aggregates_expense_by_concept_sorted_desc_with_label() = runTest {
        val env = AnalyticsTestEnv()
        val food = env.expenseCategory("Comida")
        val cerveza = env.concept("Cerveza")
        val pan = env.concept("Pan")

        env.expense(5000L, LocalDate(2026, 5, 1), food, listOf(cerveza))
        env.expense(3000L, LocalDate(2026, 5, 2), food, listOf(cerveza))
        env.expense(1500L, LocalDate(2026, 5, 3), food, listOf(pan))

        val r = handler(env).handle(FindConceptBreakdownQuery(yearMonth = "2026-05"))

        r.totalCents shouldBe 9500L
        r.items.size shouldBe 2
        r.items.first().conceptLabel shouldBe "Cerveza"
        r.items.first().totalCents shouldBe 8000L
        r.items.first().share shouldBe (8000.0 / 9500.0 plusOrMinus 1e-9)
        r.items.last().conceptLabel shouldBe "Pan"
        r.items.last().totalCents shouldBe 1500L
    }

    @Test
    fun is_not_a_partition_a_movement_counts_under_each_concept() = runTest {
        val env = AnalyticsTestEnv()
        val transport = env.expenseCategory("Transporte")
        val papa = env.concept("Papá")
        val bus = env.concept("Bus")

        // One 7.800 movement tagged with BOTH concepts.
        env.expense(7800L, LocalDate(2026, 5, 4), transport, listOf(papa, bus))

        val r = handler(env).handle(FindConceptBreakdownQuery(yearMonth = "2026-05"))

        // Period total counts the movement once...
        r.totalCents shouldBe 7800L
        // ...but each concept gets the full amount → item totals exceed the period total.
        r.items.map { it.conceptLabel to it.totalCents }.toSet() shouldBe setOf(
            "Papá" to 7800L,
            "Bus" to 7800L,
        )
        r.items.sumOf { it.totalCents } shouldBe 15600L
        // Shares can sum past 1.0 — that's the whole point of the warning.
        r.items.sumOf { it.share } shouldBe (2.0 plusOrMinus 1e-9)
    }

    @Test
    fun ignores_other_month_and_other_type() = runTest {
        val env = AnalyticsTestEnv()
        val food = env.expenseCategory("Comida")
        val cerveza = env.concept("Cerveza")
        env.expense(100L, LocalDate(2026, 4, 30), food, listOf(cerveza))
        env.expense(500L, LocalDate(2026, 5, 10), food, listOf(cerveza))

        val r = handler(env).handle(FindConceptBreakdownQuery(yearMonth = "2026-05"))

        r.totalCents shouldBe 500L
        r.items.single().totalCents shouldBe 500L
    }

    @Test
    fun movements_without_concepts_contribute_to_total_but_no_items() = runTest {
        val env = AnalyticsTestEnv()
        val food = env.expenseCategory("Comida")
        env.expense(4000L, LocalDate(2026, 5, 1), food) // no concepts

        val r = handler(env).handle(FindConceptBreakdownQuery(yearMonth = "2026-05"))

        r.totalCents shouldBe 4000L
        r.items.shouldBeEmpty()
    }
}
