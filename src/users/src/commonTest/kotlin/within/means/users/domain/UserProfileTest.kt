package within.means.users.domain

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import within.means.shared.domain.money.Currency
import within.means.users.SequentialUuidGenerator
import kotlin.test.Test

class UserProfileTest {

    @Test
    fun `bootstrap produces a UserDefaultCreated event with the right primitives`() {
        val uuids = SequentialUuidGenerator()
        val profile = UserProfile.bootstrap(
            id = UserId(uuids.next()),
            uuids = uuids,
        )

        val events = profile.pullDomainEvents()
        events shouldHaveSize 1
        val event = events.first()
        event.shouldBeInstanceOf<UserDefaultCreated>()
        event.aggregateId shouldBe profile.id.value
        event.locale shouldBe "es"
        event.baseCurrency shouldBe "EUR"
    }

    @Test
    fun `updatePreferences mutates state and emits UserPreferencesUpdated`() {
        val uuids = SequentialUuidGenerator()
        val profile = UserProfile.bootstrap(id = UserId(uuids.next()), uuids = uuids)
        profile.pullDomainEvents() // clear

        profile.updatePreferences(
            displayName = DisplayName("Alejandro"),
            locale = Locale.EN,
            baseCurrency = Currency.USD,
            uuids = uuids,
        )

        profile.displayName.value shouldBe "Alejandro"
        profile.locale shouldBe Locale.EN
        profile.baseCurrency shouldBe Currency.USD

        val events = profile.pullDomainEvents()
        events shouldHaveSize 1
        events.first().shouldBeInstanceOf<UserPreferencesUpdated>()
    }

    @Test
    fun `rehydrate restores state without emitting events`() {
        val profile = UserProfile.rehydrate(
            id = UserId("00000000-0000-4000-8000-000000000001"),
            displayName = DisplayName("Ada"),
            locale = Locale.EN,
            baseCurrency = Currency.USD,
            createdAt = kotlinx.datetime.Instant.fromEpochSeconds(0),
        )
        profile.pullDomainEvents() shouldHaveSize 0
    }
}
