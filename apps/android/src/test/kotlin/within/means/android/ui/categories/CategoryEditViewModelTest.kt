package within.means.android.ui.categories

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import within.means.categories.application.create.CreateCategoryCommand

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryEditViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var fixture: CategoriesTestFixture

    @Before
    fun setUp() {
        fixture = CategoriesTestFixture()
        fixture.start()
    }

    @After
    fun tearDown() {
        fixture.stop()
    }

    @Test
    fun `save in create mode dispatches CreateCategoryCommand and persists the category`() = runTest {
        val vm = CategoryEditViewModel()
        vm.onNameChanged("Comida")

        vm.save()
        advanceUntilIdle()

        val stored = fixture.repository.all()
        stored shouldHaveSize 1
        stored.single().name.value shouldBe "Comida"
        vm.state.value.isFinished shouldBe true
        vm.state.value.errorMessage.shouldBeNull()
        vm.state.value.saving shouldBe false
    }

    @Test
    fun `save with blank name surfaces a validation error and does not touch the repository`() = runTest {
        val vm = CategoryEditViewModel()

        vm.save()
        advanceUntilIdle()

        vm.state.value.errorMessage shouldBe "El nombre no puede estar vacío"
        vm.state.value.isFinished shouldBe false
        fixture.repository.all() shouldHaveSize 0
    }

    @Test
    fun `loadExisting populates state from the query bus`() = runTest {
        // Seed an existing category via the create flow so the repo has a real row.
        fixture.commandBus.dispatch(
            CreateCategoryCommand(
                kind = "EXPENSE",
                name = "Comida",
                color = "#FF0000",
                icon = "label",
                nature = "VARIABLE",
                essentiality = "DISCRETIONARY",
                productive = false,
                engelGroup = "FOOD",
            )
        )
        val saved = fixture.repository.all().single()

        val vm = CategoryEditViewModel()
        vm.loadExisting(saved.id.value)
        advanceUntilIdle()

        val state = vm.state.value
        state.categoryId shouldBe saved.id.value
        state.name shouldBe "Comida"
        state.color shouldBe "#FF0000"
        state.kind shouldBe "EXPENSE"
        state.nature shouldBe "VARIABLE"
        state.essentiality shouldBe "DISCRETIONARY"
        state.engelGroup shouldBe "FOOD"
        state.loading shouldBe false
        vm.isEditMode shouldBe true
    }

    @Test
    fun `save in edit mode renames and restyles the existing category`() = runTest {
        fixture.commandBus.dispatch(
            CreateCategoryCommand(
                kind = "EXPENSE",
                name = "Comida",
                color = "#FF0000",
                icon = "label",
                nature = "VARIABLE",
                essentiality = "DISCRETIONARY",
                productive = false,
                engelGroup = "FOOD",
            )
        )
        val original = fixture.repository.all().single()

        val vm = CategoryEditViewModel()
        vm.loadExisting(original.id.value)
        advanceUntilIdle()

        vm.onNameChanged("Supermercado")
        vm.onColorChanged("#00FF00")
        vm.onIconChanged("shopping_cart")
        vm.save()
        advanceUntilIdle()

        val updated = fixture.repository.search(original.id)!!
        updated.name.value shouldBe "Supermercado"
        updated.color.value shouldBe "#00FF00"
        updated.icon.value shouldBe "shopping_cart"
        vm.state.value.isFinished shouldBe true
    }

    @Test
    fun `switching kind away from EXPENSE clears expense-only classifiers`() {
        val vm = CategoryEditViewModel()

        vm.onKindChanged("INCOME")

        val state = vm.state.value
        state.kind shouldBe "INCOME"
        state.nature.shouldBeNull()
        state.essentiality.shouldBeNull()
        state.engelGroup.shouldBeNull()
        state.productive shouldBe false
    }

    @Test
    fun `save propagates a user-friendly error when the command bus throws`() = runTest {
        val vm = CategoryEditViewModel()
        vm.onNameChanged("Comida")
        // CategoryColor validates the hex string in its constructor; an invalid
        // value makes the handler throw, exercising the VM's error path.
        vm.onColorChanged("not-a-hex")

        vm.save()
        advanceUntilIdle()

        vm.state.value.errorMessage?.shouldNotBeEmpty()
        vm.state.value.isFinished shouldBe false
        vm.state.value.saving shouldBe false
        fixture.repository.all() shouldHaveSize 0
    }
}
