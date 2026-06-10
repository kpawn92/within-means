package within.means.transactions.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import within.means.transactions.FixedClock
import within.means.transactions.SequentialUuidGenerator
import within.means.transactions.application.recurring.UpdateRecurringRuleCommand
import within.means.transactions.application.recurring.UpdateRecurringRuleCommandHandler
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.RecurrenceFrequency
import within.means.transactions.domain.RecurringRule
import within.means.transactions.domain.RecurringRuleId
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionType
import within.means.transactions.infrastructure.persistence.InMemoryRecurringRuleRepository

class UpdateRecurringRuleCommandHandlerTest {

    private val clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z"))
    private val ruleId = "00000000-0000-4000-8000-00000000aaaa"
    private val otherCategory = "00000000-0000-4000-8000-000000000888"

    private fun handlerWith(repo: InMemoryRecurringRuleRepository) =
        UpdateRecurringRuleCommandHandler(
            repo, SequentialUuidGenerator(), InMemoryEventBus(emptyList()), clock,
        )

    private fun expenseRule() = RecurringRule.create(
        id = RecurringRuleId(ruleId),
        type = TransactionType.EXPENSE,
        amount = Amount(5000L),
        categoryRef = CategoryRef("00000000-0000-4000-8000-000000000999"),
        frequency = RecurrenceFrequency.MONTHLY,
        startDate = TransactionDate(LocalDate(2026, 5, 1)),
        uuids = SequentialUuidGenerator(),
        clock = clock,
    )

    @Test
    fun updating_changes_amount_category_and_frequency_keeping_the_cursor() = runTest {
        val repo = InMemoryRecurringRuleRepository()
        val rule = expenseRule()
        repo.save(rule)
        val cursorBefore = rule.nextOccurrence.value

        handlerWith(repo).handle(
            UpdateRecurringRuleCommand(
                id = ruleId,
                amountCents = 7500L,
                categoryId = otherCategory,
                description = "Gimnasio",
                frequency = "WEEKLY",
            )
        )

        val saved = repo.search(RecurringRuleId(ruleId))!!
        saved.amount.cents shouldBe 7500L
        saved.categoryRef.value shouldBe otherCategory
        saved.description.value shouldBe "Gimnasio"
        saved.frequency shouldBe RecurrenceFrequency.WEEKLY
        // Editing details must not disturb the materialization cursor.
        saved.nextOccurrence.value shouldBe cursorBefore
    }

    @Test
    fun updating_a_missing_rule_is_a_no_op() = runTest {
        val repo = InMemoryRecurringRuleRepository()

        handlerWith(repo).handle(
            UpdateRecurringRuleCommand(
                id = "00000000-0000-4000-8000-00000000bbbb",
                amountCents = 1000L,
                categoryId = otherCategory,
                frequency = "MONTHLY",
            )
        )

        repo.all().size shouldBe 0
    }

    @Test
    fun an_income_source_on_a_non_income_rule_is_rejected() = runTest {
        val repo = InMemoryRecurringRuleRepository()
        repo.save(expenseRule())

        shouldThrow<IllegalArgumentException> {
            handlerWith(repo).handle(
                UpdateRecurringRuleCommand(
                    id = ruleId,
                    amountCents = 5000L,
                    categoryId = otherCategory,
                    incomeSource = "Nómina",
                    frequency = "MONTHLY",
                )
            )
        }
    }
}
