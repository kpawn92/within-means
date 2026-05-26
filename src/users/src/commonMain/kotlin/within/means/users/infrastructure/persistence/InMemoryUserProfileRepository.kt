package within.means.users.infrastructure.persistence

import within.means.users.domain.UserId
import within.means.users.domain.UserProfile
import within.means.users.domain.UserProfileRepository

class InMemoryUserProfileRepository : UserProfileRepository {

    private val data = mutableMapOf<UserId, UserProfile>()
    private var defaultId: UserId? = null

    override suspend fun save(profile: UserProfile) {
        data[profile.id] = profile
        if (defaultId == null) defaultId = profile.id
    }

    override suspend fun search(id: UserId): UserProfile? = data[id]

    override suspend fun searchDefault(): UserProfile? = defaultId?.let { data[it] }
}
