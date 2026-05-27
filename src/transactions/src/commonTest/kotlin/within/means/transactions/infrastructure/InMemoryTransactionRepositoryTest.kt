package within.means.transactions.infrastructure

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import within.means.shared.domain.criteria.Criteria
import within.means.shared.domain.criteria.Filter
import within.means.shared.domain.criteria.FilterField
import within.means.shared.domain.criteria.FilterOperator
import within.means.shared.domain.criteria.FilterValue
import within.means.shared.domain.criteria.Filters
import within.means.shared.domain.criteria.Order
import within.means.shared.domain.criteria.OrderField
import within.means.shared.domain.criteria.OrderType
import within.means.transactions.FixedClock
import within.means.transactions.SequentialUuidGenerator
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionType
import within.means.transactions.infrastructure.persistence.InMemoryTransactionRepository

class InMemoryTransactionRepositoryTest {

    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 5, 27)
    private val clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z"))
    private val uuids = SequentialUuidGenerator()
    private val category = CategoryRef("00000000-0000-4000-8000-000000000999")

    @Test
    fun save_and_search_round_trip() = runTest {
        val repo = InMemoryTransactionRepository()
        val tx = expense("00000000-0000-4000-8000-000000000001", 1500L)

        repo.save(tx)

        repo.search(tx.id)!!.amount.cents shouldBe 1500L
        repo.countAll() shouldBe 1L
    }

    @Test
    fun delete_removes_the_transaction_and_observeAll_emits() = runTest {
        val repo = InMemoryTransactionRepository()
        val tx = expense("00000000-0000-4000-8000-000000000001", 1500L)
        repo.save(tx)

        repo.delete(tx.id)

        repo.search(tx.id) shouldBe null
        repo.observeAll().first() shouldHaveSize 0
    }

    @Test
    fun matching_filters_by_type_and_categoryId() = runTest {
        val repo = InMemoryTransactionRepository()
        repo.save(expense("00000000-0000-4000-8000-000000000001", 100L))
        repo.save(expense("00000000-0000-4000-8000-000000000002", 200L))
        repo.save(income("00000000-0000-4000-8000-000000000003", 999L))

        val expenses = repo.matching(
            Criteria(
                filters = Filters(
                    listOf(
                        Filter(
                            field = FilterField("type"),
                            operator = FilterOperator.EQUALS,
                            value = FilterValue("EXPENSE"),
                        )
                    )
                ),
                order = Order.none(),
            )
        )

        expenses.map { it.amount.cents } shouldContainExactly listOf(100L, 200L)
    }

    @Test
    fun matching_supports_order_by_date_desc_with_limit() = runTest {
        val repo = InMemoryTransactionRepository()
        repo.save(expense("00000000-0000-4000-8000-000000000001", 100L, on = LocalDate(2026, 5, 20)))
        repo.save(expense("00000000-0000-4000-8000-000000000002", 200L, on = LocalDate(2026, 5, 25)))
        repo.save(expense("00000000-0000-4000-8000-000000000003", 300L, on = LocalDate(2026, 5, 27)))

        val mostRecentTwo = repo.matching(
            Criteria(
                filters = Filters(emptyList()),
                order = Order(
                    field = OrderField("date"),
                    type = OrderType.DESC,
                ),
                limit = 2,
            )
        )

        mostRecentTwo.map { it.amount.cents } shouldContainExactly listOf(300L, 200L)
    }

    private fun expense(id: String, cents: Long, on: LocalDate = today): Transaction =
        Transaction.register(
            id = TransactionId(id),
            type = TransactionType.EXPENSE,
            amount = Amount(cents),
            date = TransactionDate(on),
            description = TransactionDescription.EMPTY,
            categoryRef = category,
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )

    private fun income(id: String, cents: Long): Transaction =
        Transaction.register(
            id = TransactionId(id),
            type = TransactionType.INCOME,
            amount = Amount(cents),
            date = TransactionDate(today),
            categoryRef = category,
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )
}
