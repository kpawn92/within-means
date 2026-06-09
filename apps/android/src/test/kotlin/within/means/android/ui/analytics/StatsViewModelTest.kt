package within.means.android.ui.analytics

import io.kotest.matchers.nulls.shouldNotBeNull
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

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private lateinit var fixture: StatsTestFixture

    @Before
    fun setUp() {
        fixture = StatsTestFixture()
        fixture.start()
    }

    @After
    fun tearDown() {
        fixture.stop()
    }

    @Test
    fun `initial reload populates summary, breakdown and evolution`() = runTest {
        val rent = fixture.seedCategory("Alquiler", nature = "FIXED", essentiality = "ESSENTIAL")
        val food = fixture.seedCategory("Comida", nature = "VARIABLE", essentiality = "ESSENTIAL")
        fixture.seedTransaction("EXPENSE", 80000L, "2026-05-01", rent)
        fixture.seedTransaction("EXPENSE", 15000L, "2026-05-10", food)

        val vm = StatsViewModel(fixture.txRepository, fixture.clock, fixture.zone)
        advanceUntilIdle()

        val s = vm.state.value
        s.summary?.totalExpenseCents shouldBe 95000L
        s.summary?.fixedExpenseCents shouldBe 80000L
        s.summary?.variableExpenseCents shouldBe 15000L
        s.breakdown?.items?.size shouldBe 2
        s.evolution?.points?.size shouldBe 6 // default monthsBack=6
    }

    @Test
    fun `selecting another month re-fetches with that yearMonth`() = runTest {
        val cat = fixture.seedCategory("Comida")
        fixture.seedTransaction("EXPENSE", 100L, "2026-04-15", cat)
        fixture.seedTransaction("EXPENSE", 500L, "2026-05-15", cat)

        val vm = StatsViewModel(fixture.txRepository, fixture.clock, fixture.zone)
        advanceUntilIdle()
        vm.state.value.summary?.totalExpenseCents shouldBe 500L

        vm.selectYearMonth("2026-04")
        advanceUntilIdle()

        vm.state.value.yearMonth shouldBe "2026-04"
        vm.state.value.summary?.totalExpenseCents shouldBe 100L
    }

    @Test
    fun `adding a new transaction re-runs the queries automatically`() = runTest {
        val cat = fixture.seedCategory("Comida")
        val vm = StatsViewModel(fixture.txRepository, fixture.clock, fixture.zone)
        advanceUntilIdle()
        vm.state.value.summary?.totalExpenseCents shouldBe 0L

        fixture.seedTransaction("EXPENSE", 1500L, "2026-05-20", cat)
        advanceUntilIdle()

        vm.state.value.summary?.totalExpenseCents shouldBe 1500L
    }

    @Test
    fun `invalid yearMonth surfaces a user-friendly error`() = runTest {
        val vm = StatsViewModel(fixture.txRepository, fixture.clock, fixture.zone)
        advanceUntilIdle()

        vm.selectYearMonth("bogus")
        advanceUntilIdle()

        vm.state.value.errorMessage.shouldNotBeNull().shouldNotBeEmpty()
    }
}
