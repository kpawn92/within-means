package within.means.android.ui.transactions

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
class TransactionEditViewModelTest {

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
    fun `categories list is pre-loaded for the default EXPENSE type`() = runTest {
        fixture.seedCategory("Comida", kind = "EXPENSE")
        fixture.seedCategory("Transporte", kind = "EXPENSE")
        fixture.seedCategory("Nómina", kind = "INCOME")

        val vm = TransactionEditViewModel()
        advanceUntilIdle()

        vm.state.value.type shouldBe "EXPENSE"
        vm.state.value.availableCategories.map { it.name } shouldBe listOf("Comida", "Transporte")
    }

    @Test
    fun `switching type reloads the categories for the new type`() = runTest {
        fixture.seedCategory("Comida", kind = "EXPENSE")
        fixture.seedCategory("Nómina", kind = "INCOME")
        val vm = TransactionEditViewModel()
        advanceUntilIdle()

        vm.onTypeChanged("INCOME")
        advanceUntilIdle()

        vm.state.value.type shouldBe "INCOME"
        vm.state.value.availableCategories.map { it.name } shouldBe listOf("Nómina")
        vm.state.value.categoryId shouldBe null
    }

    @Test
    fun `save in create mode persists the transaction and marks finished`() = runTest {
        val cat = fixture.seedCategory("Comida")
        val vm = TransactionEditViewModel()
        advanceUntilIdle()

        vm.onAmountTextChanged("12,50")
        vm.onDateChanged("2026-05-20")
        vm.onDescriptionChanged("Supermercado")
        vm.onCategoryChanged(cat)

        vm.save()
        advanceUntilIdle()

        val stored = fixture.txRepository.all()
        stored shouldHaveSize 1
        stored.single().amount.cents shouldBe 1250L
        stored.single().description.value shouldBe "Supermercado"
        vm.state.value.isFinished shouldBe true
    }

    @Test
    fun `save with non-positive amount surfaces a validation error and does not touch the repo`() = runTest {
        val cat = fixture.seedCategory("Comida")
        val vm = TransactionEditViewModel()
        advanceUntilIdle()

        vm.onAmountTextChanged("0")
        vm.onCategoryChanged(cat)
        vm.save()
        advanceUntilIdle()

        vm.state.value.errorMessage shouldBe "El monto debe ser mayor que 0"
        fixture.txRepository.all() shouldHaveSize 0
    }

    @Test
    fun `save without selecting a category surfaces a validation error`() = runTest {
        val vm = TransactionEditViewModel()
        advanceUntilIdle()

        vm.onAmountTextChanged("10")
        vm.save()
        advanceUntilIdle()

        vm.state.value.errorMessage shouldBe "Selecciona una categoría"
    }

    @Test
    fun `loadExisting populates state from the query bus and switches to edit mode`() = runTest {
        val cat = fixture.seedCategory("Comida")
        fixture.commandBus.dispatch(
            RegisterTransactionCommand(
                type = "EXPENSE",
                amountCents = 1500L,
                date = "2026-05-20",
                description = "Mercado",
                categoryId = cat,
            )
        )
        val saved = fixture.txRepository.all().single()

        val vm = TransactionEditViewModel()
        vm.loadExisting(saved.id.value)
        advanceUntilIdle()

        val state = vm.state.value
        state.transactionId shouldBe saved.id.value
        state.amountText shouldBe "15.0"
        state.date shouldBe "2026-05-20"
        state.description shouldBe "Mercado"
        state.categoryId shouldBe cat
        vm.isEditMode shouldBe true
    }

    @Test
    fun `save with a future date propagates a user-friendly error from the aggregate`() = runTest {
        val cat = fixture.seedCategory("Comida")
        val vm = TransactionEditViewModel()
        advanceUntilIdle()

        vm.onAmountTextChanged("10")
        vm.onCategoryChanged(cat)
        vm.onDateChanged("2099-01-01")
        vm.save()
        advanceUntilIdle()

        vm.state.value.errorMessage?.shouldNotBeEmpty()
        vm.state.value.isFinished shouldBe false
        fixture.txRepository.all() shouldHaveSize 0
    }
}
