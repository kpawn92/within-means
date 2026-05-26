package within.means.users.domain

interface UserProfileRepository {
    suspend fun save(profile: UserProfile)
    suspend fun search(id: UserId): UserProfile?
    suspend fun searchDefault(): UserProfile?
}
