package within.means.android.ui.transactions

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import within.means.android.ui.categories.MainDispatcherRule
import within.means.transactions.application.register.RegisterTransactionCommand

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var fixture: TransactionsTestFixture

    @Before
    fun setUp() {
        fixture = TransactionsTestFixture()
        fixture.start()
    }

    @After
    fun tearDown() {
        fixture.stop()
    }

    @Test
    fun `initial state observes existing transactions and category names`() = runTest {
        val categoryId = fixture.seedCategory("Comida")
        fixture.commandBus.dispatch(
            RegisterTransactionCommand(
                type = "EXPENSE",
                amountCents = 1500L,
                date = "2026-05-20",
                description = "Supermercado",
                categoryId = categoryId,
            )
        )

        val vm = TransactionsListViewModel(fixture.txRepository)
        advanceUntilIdle()

        vm.state.value.items shouldHaveSize 1
        vm.state.value.categoryNames[categoryId] shouldBe "Comida"
    }

    @Test
    fun `visibleItems filters by selected type tab`() = runTest {
        val expenseCat = fixture.seedCategory("Comida", kind = "EXPENSE")
        val incomeCat = fixture.seedCategory("Nómina", kind = "INCOME")
        fixture.commandBus.dispatch(
            RegisterTransactionCommand(
                type = "EXPENSE",
                amountCents = 1500L,
                date = "2026-05-20",
                categoryId = expenseCat,
            )
        )
        fixture.commandBus.dispatch(
            RegisterTransactionCommand(
                type = "INCOME",
                amountCents = 200000L,
                date = "2026-05-25",
                categoryId = incomeCat,
            )
        )

        val vm = TransactionsListViewModel(fixture.txRepository)
        advanceUntilIdle()

        vm.visibleItems().map { it.type } shouldContainExactly listOf("INCOME", "EXPENSE")

        vm.selectTypeFilter(TransactionTypeFilter.EXPENSE)
        vm.visibleItems().single().amountCents shouldBe 1500L

        vm.selectTypeFilter(TransactionTypeFilter.INCOME)
        vm.visibleItems().single().amountCents shouldBe 200000L
    }

    @Test
    fun `delete dispatches command and the list reactively shrinks`() = runTest {
        val cat = fixture.seedCategory("Comida")
        fixture.commandBus.dispatch(
            RegisterTransactionCommand(
                type = "EXPENSE",
                amountCents = 100L,
                date = "2026-05-20",
                categoryId = cat,
            )
        )
        val saved = fixture.txRepository.all().single()

        val vm = TransactionsListViewModel(fixture.txRepository)
        advanceUntilIdle()
        vm.state.value.items shouldHaveSize 1

        vm.delete(saved.id.value)
        advanceUntilIdle()

        vm.state.value.items shouldHaveSize 0
        fixture.txRepository.search(saved.id) shouldBe null
    }

    @Test
    fun `delete with invalid id surfaces an error message`() = runTest {
        val vm = TransactionsListViewModel(fixture.txRepository)
        advanceUntilIdle()

        vm.delete("not-a-uuid")
        advanceUntilIdle()

        vm.state.value.errorMessage?.shouldNotBeEmpty()
    }
}
