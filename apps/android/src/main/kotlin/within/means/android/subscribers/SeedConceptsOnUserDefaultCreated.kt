package within.means.android.subscribers

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import within.means.android.capture.ConceptsFromCategoriesSeeder
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.users.domain.UserDefaultCreated
import kotlin.reflect.KClass

/**
 * Seeds the starter concepts from the default categories during onboarding.
 * Subscribes to the same `UserDefaultCreated` event as the category seeder, but
 * must run **after** it (categories must exist first) — the subscriber order in
 * the EventBus wiring guarantees that.
 *
 * The seeder is resolved lazily via [KoinComponent.get] to break the Koin cycle
 * EventBus → subscriber → seeder → ConceptCreator → EventBus. Idempotency lives
 * in [ConceptsFromCategoriesSeeder.seedIfNeeded].
 */
class SeedConceptsOnUserDefaultCreated :
    DomainEventSubscriber<UserDefaultCreated>, KoinComponent {

    override val subscribedTo: KClass<UserDefaultCreated> = UserDefaultCreated::class

    override suspend fun consume(event: UserDefaultCreated) {
        get<ConceptsFromCategoriesSeeder>().seedIfNeeded()
    }
}
