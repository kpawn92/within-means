package within.means.android.di

import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module
import within.means.android.persistence.DatabaseUnlocker
import within.means.shared.db.SharedDatabase
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.event.DomainEventStore
import within.means.shared.infrastructure.RealUuidGenerator
import within.means.shared.infrastructure.persistence.SqlDelightDomainEventStore
import within.means.users.db.UsersDatabase
import within.means.users.domain.UserProfileRepository
import within.means.users.infrastructure.persistence.SqlDelightUserProfileRepository

val persistenceModule = module {

    single<UuidGenerator> { RealUuidGenerator() }

    // Databases are resolved lazily via DatabaseUnlocker. Requesting them
    // before unlock() throws an explicit error.
    single<SharedDatabase> { get<DatabaseUnlocker>().shared }
    single<UsersDatabase> { get<DatabaseUnlocker>().users }

    single<DomainEventStore> {
        SqlDelightDomainEventStore(get(), get(named("io")))
    }

    single<UserProfileRepository> {
        SqlDelightUserProfileRepository(get(), get(named("io")))
    }
}
