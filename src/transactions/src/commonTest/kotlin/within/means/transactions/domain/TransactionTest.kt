package within.means.transactions.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import within.means.transactions.FixedClock
import within.means.transactions.SequentialUuidGenerator

class TransactionTest {

    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 5, 27)
    private val clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z"))
    private val uuids = SequentialUuidGenerator()

    private val anyId = TransactionId("00000000-0000-4000-8000-000000000001")
    private val anyCategory = CategoryRef("00000000-0000-4000-8000-000000000999")

    @Test
    fun register_emits_TransactionRegistered_with_all_fields() {
        val tx = Transaction.register(
            id = anyId,
            type = TransactionType.EXPENSE,
            amount = Amount(1500L),
            date = TransactionDate(today),
            description = TransactionDescription("Supermercado"),
            categoryRef = anyCategory,
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )

        val events = tx.pullDomainEvents()
        events shouldHaveSize 1
        val registered = events.single().shouldBeInstanceOf<TransactionRegistered>()
        registered.aggregateId shouldBe anyId.value
        registered.type shouldBe "EXPENSE"
        registered.amountCents shouldBe 1500L
        registered.date shouldBe "2026-05-27"
        registered.description shouldBe "Supermercado"
        registered.categoryId shouldBe anyCategory.value
        registered.incomeSource shouldBe null
    }

    @Test
    fun register_rejects_future_dates() {
        val tomorrow = LocalDate(2026, 5, 28)
        shouldThrow<IllegalArgumentException> {
            Transaction.register(
                id = anyId,
                type = TransactionType.EXPENSE,
                amount = Amount(100L),
                date = TransactionDate(tomorrow),
                categoryRef = anyCategory,
                uuids = uuids,
                clock = clock,
                timeZone = zone,
            )
        }
    }

    @Test
    fun register_rejects_EXPENSE_with_incomeSource() {
        shouldThrow<IllegalArgumentException> {
            Transaction.register(
                id = anyId,
                type = TransactionType.EXPENSE,
                amount = Amount(100L),
                date = TransactionDate(today),
                categoryRef = anyCategory,
                incomeSource = IncomeSource("Empresa S.A."),
                uuids = uuids,
                clock = clock,
                timeZone = zone,
            )
        }
    }

    @Test
    fun register_rejects_TRANSFER_with_incomeSource() {
        shouldThrow<IllegalArgumentException> {
            Transaction.register(
                id = anyId,
                type = TransactionType.TRANSFER,
                amount = Amount(100L),
                date = TransactionDate(today),
                categoryRef = anyCategory,
                incomeSource = IncomeSource("Empresa S.A."),
                uuids = uuids,
                clock = clock,
                timeZone = zone,
            )
        }
    }

    @Test
    fun register_accepts_TRANSFER_as_savings() {
        val tx = Transaction.register(
            id = anyId,
            type = TransactionType.TRANSFER,
            amount = Amount(30000L),
            date = TransactionDate(today),
            categoryRef = anyCategory,
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )
        tx.type shouldBe TransactionType.TRANSFER
        tx.pullDomainEvents().single().shouldBeInstanceOf<TransactionRegistered>().type shouldBe "TRANSFER"
    }

    @Test
    fun register_accepts_INCOME_with_or_without_incomeSource() {
        val withSource = Transaction.register(
            id = anyId,
            type = TransactionType.INCOME,
            amount = Amount(200000L),
            date = TransactionDate(today),
            categoryRef = anyCategory,
            incomeSource = IncomeSource("Empresa S.A."),
            uuids = SequentialUuidGenerator(),
            clock = clock,
            timeZone = zone,
        )
        withSource.incomeSource?.value shouldBe "Empresa S.A."

        val withoutSource = Transaction.register(
            id = TransactionId("00000000-0000-4000-8000-000000000002"),
            type = TransactionType.INCOME,
            amount = Amount(50000L),
            date = TransactionDate(today),
            categoryRef = anyCategory,
            uuids = SequentialUuidGenerator(),
            clock = clock,
            timeZone = zone,
        )
        withoutSource.incomeSource shouldBe null
    }

    @Test
    fun edit_emits_TransactionEdited_and_mutates_state() {
        val tx = newExpense()
        tx.pullDomainEvents() // clear register event

        tx.edit(
            newAmount = Amount(2000L),
            newDate = TransactionDate(today),
            newTime = null,
            newDescription = TransactionDescription("Supermercado XL"),
            newCategoryRef = anyCategory,
            newIncomeSource = null,
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )

        tx.amount.cents shouldBe 2000L
        tx.description.value shouldBe "Supermercado XL"
        val events = tx.pullDomainEvents()
        events shouldHaveSize 1
        events.single().shouldBeInstanceOf<TransactionEdited>().amountCents shouldBe 2000L
    }

    @Test
    fun edit_is_a_noop_when_nothing_changes() {
        val tx = newExpense()
        tx.pullDomainEvents()

        tx.edit(
            newAmount = tx.amount,
            newDate = tx.date,
            newTime = tx.time,
            newDescription = tx.description,
            newCategoryRef = tx.categoryRef,
            newIncomeSource = tx.incomeSource,
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )

        tx.pullDomainEvents() shouldHaveSize 0
    }

    @Test
    fun edit_re_validates_invariants() {
        val tx = newExpense()
        val tomorrow = LocalDate(2026, 5, 28)
        shouldThrow<IllegalArgumentException> {
            tx.edit(
                newAmount = tx.amount,
                newDate = TransactionDate(tomorrow),
                newTime = null,
                newDescription = tx.description,
                newCategoryRef = tx.categoryRef,
                newIncomeSource = null,
                uuids = uuids,
                clock = clock,
                timeZone = zone,
            )
        }
    }

    @Test
    fun markDeleted_emits_TransactionDeleted() {
        val tx = newExpense()
        tx.pullDomainEvents()

        tx.markDeleted(uuids = uuids, clock = clock)

        val events = tx.pullDomainEvents()
        events.single().shouldBeInstanceOf<TransactionDeleted>().aggregateId shouldBe anyId.value
    }

    private fun newExpense(): Transaction = Transaction.register(
        id = anyId,
        type = TransactionType.EXPENSE,
        amount = Amount(1500L),
        date = TransactionDate(today),
        description = TransactionDescription("Comida"),
        categoryRef = anyCategory,
        uuids = uuids,
        clock = clock,
        timeZone = zone,
    )
}
