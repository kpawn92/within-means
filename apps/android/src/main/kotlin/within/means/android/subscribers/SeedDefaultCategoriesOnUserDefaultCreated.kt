package within.means.android.subscribers

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.categories.application.seed.DefaultCategoriesSeeder
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.users.domain.UserDefaultCreated
import kotlin.reflect.KClass

/**
 * Cross-context glue wired in the composition root: when the default user
 * is created (during onboarding), seed the default category set. Lives in
 * apps/android so neither :categories nor :users learns about the other.
 *
 * The seeder is resolved lazily via [KoinComponent.get] to break the Koin
 * cycle: EventBus → this subscriber → seeder → CategoryCreator → EventBus.
 * Idempotency lives inside [DefaultCategoriesSeeder.seedIfNeeded].
 */
class SeedDefaultCategoriesOnUserDefaultCreated :
    DomainEventSubscriber<UserDefaultCreated>, KoinComponent {

    override val subscribedTo: KClass<UserDefaultCreated> = UserDefaultCreated::class

    override suspend fun consume(event: UserDefaultCreated) {
        get<DefaultCategoriesSeeder>().seedIfNeeded()
    }
}
