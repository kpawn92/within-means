package within.means.transactions.application

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import within.means.transactions.FixedClock
import within.means.transactions.SequentialUuidGenerator
import within.means.transactions.application.recurring.DeactivateRecurringRuleCommand
import within.means.transactions.application.recurring.DeactivateRecurringRuleCommandHandler
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.RecurrenceFrequency
import within.means.transactions.domain.RecurringRule
import within.means.transactions.domain.RecurringRuleId
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionType
import within.means.transactions.infrastructure.persistence.InMemoryRecurringRuleRepository

class DeactivateRecurringRuleCommandHandlerTest {

    private val clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z"))
    private val ruleId = "00000000-0000-4000-8000-00000000aaaa"

    private fun handlerWith(repo: InMemoryRecurringRuleRepository) =
        DeactivateRecurringRuleCommandHandler(
            repo, SequentialUuidGenerator(), InMemoryEventBus(emptyList()), clock,
        )

    private fun activeRule() = RecurringRule.create(
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
    fun deactivating_an_active_rule_drops_it_from_active_set() = runTest {
        val repo = InMemoryRecurringRuleRepository()
        repo.save(activeRule())
        repo.countActive() shouldBe 1L

        handlerWith(repo).handle(DeactivateRecurringRuleCommand(ruleId))

        repo.countActive() shouldBe 0L
        repo.search(RecurringRuleId(ruleId))!!.active shouldBe false
    }

    @Test
    fun deactivating_a_missing_rule_is_a_no_op() = runTest {
        val repo = InMemoryRecurringRuleRepository()

        handlerWith(repo).handle(
            DeactivateRecurringRuleCommand("00000000-0000-4000-8000-00000000bbbb"),
        )

        repo.all().size shouldBe 0
    }

    @Test
    fun deactivating_twice_is_idempotent() = runTest {
        val repo = InMemoryRecurringRuleRepository()
        repo.save(activeRule())

        val handler = handlerWith(repo)
        handler.handle(DeactivateRecurringRuleCommand(ruleId))
        handler.handle(DeactivateRecurringRuleCommand(ruleId))

        repo.countActive() shouldBe 0L
    }
}
