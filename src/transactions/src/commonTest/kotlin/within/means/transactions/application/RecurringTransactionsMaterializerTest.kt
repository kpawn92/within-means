package within.means.transactions.application

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import within.means.transactions.FixedClock
import within.means.transactions.SequentialUuidGenerator
import within.means.transactions.application.recurring.CreateRecurringRuleCommand
import within.means.transactions.application.recurring.RecurringRuleRegistrar
import within.means.transactions.application.recurring.RecurringTransactionsMaterializer
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.RecurrenceFrequency
import within.means.transactions.domain.RecurringRule
import within.means.transactions.domain.RecurringRuleId
import within.means.transactions.domain.TransactionDate
import within.means.transactions.infrastructure.persistence.InMemoryRecurringRuleRepository
import within.means.transactions.infrastructure.persistence.InMemoryTransactionRepository

class RecurringTransactionsMaterializerTest {

    private val zone = TimeZone.UTC
    // Fixed "today" = 2026-05-27.
    private val clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z"))
    private val category = CategoryRef("00000000-0000-4000-8000-000000000999")

    private fun env(): Triple<InMemoryTransactionRepository, InMemoryRecurringRuleRepository, RecurringTransactionsMaterializer> {
        val txRepo = InMemoryTransactionRepository()
        val ruleRepo = InMemoryRecurringRuleRepository()
        val materializer = RecurringTransactionsMaterializer(
            txRepo, ruleRepo, SequentialUuidGenerator(), InMemoryEventBus(emptyList()), clock, zone,
        )
        return Triple(txRepo, ruleRepo, materializer)
    }

    private fun rule(
        frequency: RecurrenceFrequency,
        start: LocalDate,
    ): RecurringRule = RecurringRule.create(
        id = RecurringRuleId("00000000-0000-4000-8000-00000000aaaa"),
        type = within.means.transactions.domain.TransactionType.EXPENSE,
        amount = Amount(5000L),
        categoryRef = category,
        frequency = frequency,
        startDate = TransactionDate(start),
        uuids = SequentialUuidGenerator(),
        clock = clock,
    )

    @Test
    fun monthly_rule_catches_up_every_due_month_and_advances_the_cursor() = runTest {
        val (txRepo, ruleRepo, materializer) = env()
        val r = rule(RecurrenceFrequency.MONTHLY, LocalDate(2026, 3, 15))
        ruleRepo.save(r)

        materializer.materializeAllActive()

        // 2026-03-15, 04-15, 05-15 are due; 06-15 is not.
        txRepo.all() shouldHaveSize 3
        txRepo.all().map { it.date.value.toString() }.sorted() shouldBe
            listOf("2026-03-15", "2026-04-15", "2026-05-15")
        ruleRepo.search(r.id)!!.nextOccurrence.value shouldBe LocalDate(2026, 6, 15)
        txRepo.all().all { it.recurringRef?.value == r.id.value } shouldBe true
    }

    @Test
    fun weekly_rule_materializes_each_due_week() = runTest {
        val (txRepo, ruleRepo, materializer) = env()
        val r = rule(RecurrenceFrequency.WEEKLY, LocalDate(2026, 5, 6))
        ruleRepo.save(r)

        materializer.materializeAllActive()

        // 05-06, 05-13, 05-20, 05-27.
        txRepo.all() shouldHaveSize 4
        ruleRepo.search(r.id)!!.nextOccurrence.value shouldBe LocalDate(2026, 6, 3)
    }

    @Test
    fun running_twice_is_idempotent() = runTest {
        val (txRepo, ruleRepo, materializer) = env()
        ruleRepo.save(rule(RecurrenceFrequency.MONTHLY, LocalDate(2026, 5, 1)))

        materializer.materializeAllActive()
        val afterFirst = txRepo.all().size
        materializer.materializeAllActive()

        txRepo.all().size shouldBe afterFirst
    }

    @Test
    fun future_start_produces_nothing_yet() = runTest {
        val (txRepo, ruleRepo, materializer) = env()
        val r = rule(RecurrenceFrequency.MONTHLY, LocalDate(2026, 6, 10))
        ruleRepo.save(r)

        materializer.materializeAllActive()

        txRepo.all() shouldHaveSize 0
        ruleRepo.search(r.id)!!.nextOccurrence.value shouldBe LocalDate(2026, 6, 10)
    }

    @Test
    fun registrar_creates_rule_and_materializes_the_first_due_occurrence() = runTest {
        val (txRepo, ruleRepo, materializer) = env()
        val registrar = RecurringRuleRegistrar(
            ruleRepo, materializer, SequentialUuidGenerator(), InMemoryEventBus(emptyList()),
        )

        registrar.register(
            CreateRecurringRuleCommand(
                type = "EXPENSE",
                amountCents = 5000L,
                categoryId = category.value,
                frequency = "MONTHLY",
                startDate = "2026-05-01",
            )
        )

        txRepo.all() shouldHaveSize 1
        ruleRepo.countActive() shouldBe 1L
    }
}
