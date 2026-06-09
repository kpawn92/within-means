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
    fun `selecting the week period narrows the range to the current week`() = runTest {
        // Clock is fixed at 2026-05-27 (Wed); week = 2026-05-25..05-31.
        val cat = fixture.seedCategory("Comida")
        fixture.seedTransaction("EXPENSE", 100L, "2026-05-15", cat) // month, not week
        fixture.seedTransaction("EXPENSE", 500L, "2026-05-27", cat) // week + month

        val vm = StatsViewModel(fixture.txRepository, fixture.clock, fixture.zone)
        advanceUntilIdle()
        vm.state.value.summary?.totalExpenseCents shouldBe 600L // default: month

        vm.selectPeriod(StatsPeriod.WEEK)
        advanceUntilIdle()

        vm.state.value.period shouldBe StatsPeriod.WEEK
        vm.state.value.summary?.totalExpenseCents shouldBe 500L
    }

    @Test
    fun `selecting the year period spans the whole year`() = runTest {
        val cat = fixture.seedCategory("Comida")
        fixture.seedTransaction("EXPENSE", 100L, "2026-01-15", cat)
        fixture.seedTransaction("EXPENSE", 500L, "2026-05-15", cat)

        val vm = StatsViewModel(fixture.txRepository, fixture.clock, fixture.zone)
        advanceUntilIdle()

        vm.selectPeriod(StatsPeriod.YEAR)
        advanceUntilIdle()

        vm.state.value.summary?.totalExpenseCents shouldBe 600L
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
    fun `period label reflects the selected period`() = runTest {
        val vm = StatsViewModel(fixture.txRepository, fixture.clock, fixture.zone)
        advanceUntilIdle()
        vm.state.value.periodLabel shouldBe "Mayo"

        vm.selectPeriod(StatsPeriod.WEEK)
        advanceUntilIdle()
        vm.state.value.periodLabel shouldBe "Esta semana"

        vm.selectPeriod(StatsPeriod.YEAR)
        advanceUntilIdle()
        vm.state.value.periodLabel shouldBe "2026"
    }
}
