package within.means.android.ui.settings

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
import within.means.android.ui.home.HomeTestFixture

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `initial load populates the form from the default user`() = runTest {
        fixture.seedUser(displayName = "Ana", locale = "ES", currency = "EUR")

        val vm = SettingsViewModel()
        advanceUntilIdle()

        val s = vm.state.value
        s.displayName shouldBe "Ana"
        s.locale shouldBe "es"
        s.baseCurrency shouldBe "EUR"
        s.loading shouldBe false
        s.userId?.shouldNotBeEmpty()
    }

    @Test
    fun `save dispatches UpdateUserPreferencesCommand and persists the changes`() = runTest {
        val userId = fixture.seedUser(displayName = "Ana", locale = "ES", currency = "EUR")

        val vm = SettingsViewModel()
        advanceUntilIdle()

        vm.onDisplayNameChanged("Ana María")
        vm.onLocaleChanged("en")
        vm.onBaseCurrencyChanged("USD")
        vm.save()
        advanceUntilIdle()

        val stored = fixture.userRepository.searchDefault()!!
        stored.id.value shouldBe userId
        stored.displayName.value shouldBe "Ana María"
        stored.locale.name shouldBe "EN"
        stored.baseCurrency.name shouldBe "USD"

        vm.state.value.savedAck shouldBe true
        vm.state.value.saving shouldBe false
    }

    @Test
    fun `save with blank displayName surfaces a validation error and does not touch the repo`() = runTest {
        fixture.seedUser(displayName = "Ana")
        val vm = SettingsViewModel()
        advanceUntilIdle()

        vm.onDisplayNameChanged("   ")
        vm.save()
        advanceUntilIdle()

        vm.state.value.errorMessage shouldBe "El nombre no puede estar vacío"
        fixture.userRepository.searchDefault()!!.displayName.value shouldBe "Ana"
    }

    @Test
    fun `editing any field clears the previous savedAck`() = runTest {
        fixture.seedUser(displayName = "Ana")
        val vm = SettingsViewModel()
        advanceUntilIdle()

        vm.onDisplayNameChanged("Ana 2")
        vm.save()
        advanceUntilIdle()
        vm.state.value.savedAck shouldBe true

        vm.onDisplayNameChanged("Ana 3")
        vm.state.value.savedAck shouldBe false
    }
}
