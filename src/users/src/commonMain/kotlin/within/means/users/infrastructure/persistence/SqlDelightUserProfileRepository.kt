package within.means.users.infrastructure.persistence

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import within.means.users.db.UsersDatabase
import within.means.users.domain.UserId
import within.means.users.domain.UserProfile
import within.means.users.domain.UserProfileRepository

class SqlDelightUserProfileRepository(
    private val db: UsersDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : UserProfileRepository {

    override suspend fun save(profile: UserProfile): Unit = withContext(ioDispatcher) {
        val row = profile.toRow(isDefault = true)
        db.userProfileQueries.upsert(
            id = row.id,
            display_name = row.displayName,
            locale = row.locale,
            base_currency = row.baseCurrency,
            created_at = row.createdAt,
            is_default = row.isDefault,
        )
    }

    override suspend fun search(id: UserId): UserProfile? = withContext(ioDispatcher) {
        db.userProfileQueries.findById(id.value).executeAsOneOrNull()?.toAggregate()
    }

    override suspend fun searchDefault(): UserProfile? = withContext(ioDispatcher) {
        db.userProfileQueries.findDefault().executeAsOneOrNull()?.toAggregate()
    }
}
