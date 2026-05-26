package within.means.users.application.ensure_default

import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.event.EventBus
import within.means.users.domain.UserId
import within.means.users.domain.UserProfile
import within.means.users.domain.UserProfileRepository

class DefaultUserBootstrap(
    private val repository: UserProfileRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) {
    suspend fun ensure() {
        if (repository.searchDefault() != null) return

        val profile = UserProfile.bootstrap(
            id = UserId(uuids.next()),
            uuids = uuids,
        )
        repository.save(profile)
        eventBus.publish(profile.pullDomainEvents())
    }
}
