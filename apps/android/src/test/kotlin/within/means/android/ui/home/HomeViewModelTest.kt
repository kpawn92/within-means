package within.means.android.ui.home

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import within.means.android.ui.categories.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var fixture: HomeTestFixture

    @Before
    fun setUp() {
        fixture = HomeTestFixture()
        fixture.start()
    }

    @After
    fun tearDown() {
        fixture.stop()
    }

    @Test
    fun `initial load resolves displayName, base currency, summary and recent transactions`() = runTest {
        fixture.seedUser(displayName = "Ana", locale = "ES", currency = "EUR")
        val food = fixture.seedCategory("Comida")
        fixture.seedTransaction("EXPENSE", 1500L, "2026-05-20", food)
        fixture.seedTransaction("INCOME", 200000L, "2026-05-01", fixture.seedCategory("Nómina", kind = "INCOME"))

        val vm = HomeViewModel(fixture.txRepository)
        advanceUntilIdle()

        val s = vm.state.value
        s.displayName shouldBe "Ana"
        s.baseCurrency shouldBe "EUR"
        s.summary?.totalIncomeCents shouldBe 200000L
        s.summary?.totalExpenseCents shouldBe 1500L
        s.recentTransactions.size shouldBe 2
        s.loading shouldBe false
    }

    @Test
    fun `recent transactions are capped at 5 even when more exist`() = runTest {
        fixture.seedUser()
        val cat = fixture.seedCategory("Comida")
        repeat(8) { i ->
            fixture.seedTransaction("EXPENSE", (i + 1) * 100L, "2026-05-${(20 - i).toString().padStart(2, '0')}", cat)
        }

        val vm = HomeViewModel(fixture.txRepository)
        advanceUntilIdle()

        vm.state.value.recentTransactions.size shouldBe 5
    }

    @Test
    fun `new transaction propagates to summary and recent list via observeAll`() = runTest {
        fixture.seedUser()
        val cat = fixture.seedCategory("Comida")
        val vm = HomeViewModel(fixture.txRepository)
        advanceUntilIdle()
        vm.state.value.summary?.totalExpenseCents shouldBe 0L

        fixture.seedTransaction("EXPENSE", 5000L, "2026-05-20", cat)
        advanceUntilIdle()

        vm.state.value.summary?.totalExpenseCents shouldBe 5000L
        vm.state.value.recentTransactions.size shouldBe 1
    }

    @Test
    fun `category names map is populated for the recent-transactions list`() = runTest {
        fixture.seedUser()
        val cat = fixture.seedCategory("Comida")
        fixture.seedTransaction("EXPENSE", 1500L, "2026-05-20", cat)

        val vm = HomeViewModel(fixture.txRepository)
        advanceUntilIdle()

        vm.state.value.categoryNames[cat] shouldBe "Comida"
    }
}
