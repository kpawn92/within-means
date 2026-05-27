package within.means.transactions.application

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import within.means.transactions.FixedClock
import within.means.transactions.SequentialUuidGenerator
import within.means.transactions.application.search.SearchTransactionsQuery
import within.means.transactions.application.search.SearchTransactionsQueryHandler
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionType
import within.means.transactions.infrastructure.persistence.InMemoryTransactionRepository

class SearchTransactionsQueryHandlerTest {

    private val zone = TimeZone.UTC
    private val clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z"))
    private val uuids = SequentialUuidGenerator()
    private val food = CategoryRef("00000000-0000-4000-8000-000000000900")
    private val transport = CategoryRef("00000000-0000-4000-8000-000000000901")

    @Test
    fun no_filters_returns_all_ordered_by_date_desc() = runTest {
        val repo = InMemoryTransactionRepository()
        repo.save(expense(id(1), 100L, food, LocalDate(2026, 5, 20)))
        repo.save(expense(id(2), 200L, food, LocalDate(2026, 5, 25)))
        repo.save(expense(id(3), 300L, transport, LocalDate(2026, 5, 27)))
        val handler = SearchTransactionsQueryHandler(repo)

        val result = handler.handle(SearchTransactionsQuery())

        result.items.map { it.date } shouldContainExactly listOf(
            "2026-05-27",
            "2026-05-25",
            "2026-05-20",
        )
    }

    @Test
    fun filters_by_category_and_date_range() = runTest {
        val repo = InMemoryTransactionRepository()
        repo.save(expense(id(1), 100L, food, LocalDate(2026, 5, 20)))
        repo.save(expense(id(2), 200L, food, LocalDate(2026, 5, 25)))
        repo.save(expense(id(3), 300L, transport, LocalDate(2026, 5, 25)))
        val handler = SearchTransactionsQueryHandler(repo)

        val result = handler.handle(
            SearchTransactionsQuery(
                categoryId = food.value,
                dateFrom = "2026-05-21",
            )
        )

        result.items shouldHaveSize 1
        result.items.single().amountCents shouldBe 200L
    }

    @Test
    fun filters_by_amount_range() = runTest {
        val repo = InMemoryTransactionRepository()
        repo.save(expense(id(1), 50L, food, LocalDate(2026, 5, 20)))
        repo.save(expense(id(2), 500L, food, LocalDate(2026, 5, 21)))
        repo.save(expense(id(3), 5000L, food, LocalDate(2026, 5, 22)))
        val handler = SearchTransactionsQueryHandler(repo)

        val result = handler.handle(
            SearchTransactionsQuery(amountMinCents = 100L, amountMaxCents = 1000L)
        )

        result.items shouldHaveSize 1
        result.items.single().amountCents shouldBe 500L
    }

    @Test
    fun limit_caps_the_result_size() = runTest {
        val repo = InMemoryTransactionRepository()
        repeat(5) { i ->
            repo.save(expense(id(i + 1), (i + 1) * 100L, food, LocalDate(2026, 5, 20 + i)))
        }
        val handler = SearchTransactionsQueryHandler(repo)

        val result = handler.handle(SearchTransactionsQuery(limit = 3))

        result.items shouldHaveSize 3
    }

    private fun id(n: Int): String = "00000000-0000-4000-8000-${n.toString().padStart(12, '0')}"

    private fun expense(idValue: String, cents: Long, cat: CategoryRef, on: LocalDate): Transaction =
        Transaction.register(
            id = TransactionId(idValue),
            type = TransactionType.EXPENSE,
            amount = Amount(cents),
            date = TransactionDate(on),
            description = TransactionDescription.EMPTY,
            categoryRef = cat,
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )
}
