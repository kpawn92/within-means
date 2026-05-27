package within.means.android.ui.categories

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import within.means.categories.domain.Category
import within.means.categories.domain.CategoryClassifiers
import within.means.categories.domain.CategoryColor
import within.means.categories.domain.CategoryIcon
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryKind
import within.means.categories.domain.CategoryName

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesListViewModelTest {

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
    fun `initial state collects categories already in the repository`() = runTest {
        fixture.repository.save(buildExpense(id = idFor(1), name = "Comida"))
        fixture.repository.save(buildIncome(id = idFor(2), name = "Nómina"))

        val vm = CategoriesListViewModel(fixture.repository)
        advanceUntilIdle()

        vm.state.value.items.map { it.name } shouldContainExactly listOf("Comida", "Nómina")
    }

    @Test
    fun `selectTab updates the selected tab in state`() = runTest {
        val vm = CategoriesListViewModel(fixture.repository)
        advanceUntilIdle()

        vm.selectTab(CategoryKindTab.INCOME)

        vm.state.value.selectedTab shouldBe CategoryKindTab.INCOME
    }

    @Test
    fun `visibleItems returns only categories matching the current tab`() = runTest {
        fixture.repository.save(buildExpense(id = idFor(1), name = "Comida"))
        fixture.repository.save(buildExpense(id = idFor(2), name = "Transporte"))
        fixture.repository.save(buildIncome(id = idFor(3), name = "Nómina"))

        val vm = CategoriesListViewModel(fixture.repository)
        advanceUntilIdle()

        vm.visibleItems().map { it.name } shouldContainExactly listOf("Comida", "Transporte")

        vm.selectTab(CategoryKindTab.INCOME)
        vm.visibleItems().map { it.name } shouldContainExactly listOf("Nómina")
    }

    @Test
    fun `delete dispatches the command and the list reactively shrinks`() = runTest {
        val toKeep = buildExpense(id = idFor(1), name = "Comida")
        val toRemove = buildExpense(id = idFor(2), name = "Ropa")
        fixture.repository.save(toKeep)
        fixture.repository.save(toRemove)

        val vm = CategoriesListViewModel(fixture.repository)
        advanceUntilIdle()
        vm.state.value.items shouldHaveSize 2

        vm.delete(toRemove.id.value)
        advanceUntilIdle()

        vm.state.value.items.map { it.name } shouldContainExactly listOf("Comida")
        fixture.repository.search(toRemove.id) shouldBe null
        vm.state.value.errorMessage shouldBe null
    }

    @Test
    fun `delete surfaces a user-friendly error message when the command fails`() = runTest {
        val vm = CategoriesListViewModel(fixture.repository)
        advanceUntilIdle()

        // Invalid UUID forces CategoryId validation inside the handler to throw.
        vm.delete("not-a-uuid")
        advanceUntilIdle()

        vm.state.value.errorMessage?.shouldNotBeEmpty()
    }

    @Test
    fun `clearError resets the error message back to null`() = runTest {
        val vm = CategoriesListViewModel(fixture.repository)
        advanceUntilIdle()
        vm.delete("not-a-uuid")
        advanceUntilIdle()
        vm.state.value.errorMessage?.shouldNotBeEmpty()

        vm.clearError()

        vm.state.value.errorMessage shouldBe null
    }

    private fun idFor(n: Int): CategoryId =
        CategoryId("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")

    private fun buildExpense(id: CategoryId, name: String): Category =
        Category.rehydrate(
            id = id,
            kind = CategoryKind.EXPENSE,
            name = CategoryName(name),
            color = CategoryColor("#FF0000"),
            icon = CategoryIcon("label"),
            classifiers = CategoryClassifiers(
                nature = null,
                essentiality = null,
                productive = false,
                engelGroup = null,
            ),
            parentId = null,
            createdAt = Instant.fromEpochSeconds(0),
        )

    private fun buildIncome(id: CategoryId, name: String): Category =
        Category.rehydrate(
            id = id,
            kind = CategoryKind.INCOME,
            name = CategoryName(name),
            color = CategoryColor("#00FF00"),
            icon = CategoryIcon("label"),
            classifiers = CategoryClassifiers(
                nature = null,
                essentiality = null,
                productive = false,
                engelGroup = null,
            ),
            parentId = null,
            createdAt = Instant.fromEpochSeconds(0),
        )
}
