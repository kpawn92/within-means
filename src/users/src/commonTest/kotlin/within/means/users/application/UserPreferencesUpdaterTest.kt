package within.means.users.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.EventBus
import within.means.shared.domain.money.Currency
import within.means.users.SequentialUuidGenerator
import within.means.users.application.ensure_default.DefaultUserBootstrap
import within.means.users.application.update_preferences.UserPreferencesUpdater
import within.means.users.domain.DisplayName
import within.means.users.domain.Locale
import within.means.users.domain.UserId
import within.means.users.domain.UserPreferencesUpdated
import within.means.users.infrastructure.persistence.InMemoryUserProfileRepository
import kotlin.test.Test

class UserPreferencesUpdaterTest {

    private class RecordingEventBus : EventBus {
        val published = mutableListOf<DomainEvent>()
        override suspend fun publish(events: List<DomainEvent>) {
            published.addAll(events)
        }
    }

    @Test
    fun `updates an existing profile and emits UserPreferencesUpdated`() = runTest {
        val repo = InMemoryUserProfileRepository()
        val bus = RecordingEventBus()
        val uuids = SequentialUuidGenerator()
        DefaultUserBootstrap(repo, uuids, bus).ensure()
        bus.published.clear()

        val userId = repo.searchDefault()!!.id
        val updater = UserPreferencesUpdater(repo, uuids, bus)
        updater.update(
            userId = userId,
            displayName = DisplayName("Alejandro"),
            locale = Locale.EN,
            baseCurrency = Currency.USD,
            monthlyBudgetCents = 120000L,
            spendingAlertsEnabled = false,
            monthStartDay = 10,
            hideAmounts = true,
        )

        val updated = repo.search(userId)!!
        updated.displayName.value shouldBe "Alejandro"
        updated.locale shouldBe Locale.EN
        updated.baseCurrency shouldBe Currency.USD
        updated.monthlyBudgetCents shouldBe 120000L
        updated.spendingAlertsEnabled shouldBe false
        updated.monthStartDay shouldBe 10
        updated.hideAmounts shouldBe true

        bus.published shouldHaveSize 1
        bus.published.first().shouldBeInstanceOf<UserPreferencesUpdated>()
    }

    @Test
    fun `fails when the user does not exist`() = runTest {
        val repo = InMemoryUserProfileRepository()
        val bus = RecordingEventBus()
        val uuids = SequentialUuidGenerator()
        val updater = UserPreferencesUpdater(repo, uuids, bus)

        shouldThrow<IllegalStateException> {
            updater.update(
                userId = UserId("00000000-0000-4000-8000-000000000999"),
                displayName = DisplayName("X"),
                locale = Locale.EN,
                baseCurrency = Currency.USD,
                monthlyBudgetCents = 0L,
                spendingAlertsEnabled = true,
                monthStartDay = 1,
                hideAmounts = false,
            )
        }
    }
}
