package within.means.users.application.update_preferences

import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.event.EventBus
import within.means.shared.domain.money.Currency
import within.means.users.domain.DisplayName
import within.means.users.domain.Locale
import within.means.users.domain.UserId
import within.means.users.domain.UserProfileRepository

class UserPreferencesUpdater(
    private val repository: UserProfileRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) {
    suspend fun update(
        userId: UserId,
        displayName: DisplayName,
        locale: Locale,
        baseCurrency: Currency,
    ) {
        val profile = repository.search(userId)
            ?: error("UserProfile not found: ${userId.value}")

        profile.updatePreferences(
            displayName = displayName,
            locale = locale,
            baseCurrency = baseCurrency,
            uuids = uuids,
        )
        repository.save(profile)
        eventBus.publish(profile.pullDomainEvents())
    }
}
