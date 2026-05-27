package within.means.android.subscribers

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import within.means.android.ui.categories.SequentialUuidGenerator
import within.means.categories.application.create.CategoryCreator
import within.means.categories.application.seed.DefaultCategoriesSeeder
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import within.means.users.domain.UserDefaultCreated

/**
 * Exercises the full cross-context wiring: a `UserDefaultCreated` event
 * published on the bus reaches the subscriber, which resolves the seeder
 * via Koin (lazy by design to break the EventBus → subscriber → seeder
 * → CategoryCreator → EventBus cycle) and populates the repository with
 * the 13 default categories.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SeedDefaultCategoriesIntegrationTest {

    private lateinit var repository: InMemoryCategoryRepository
    private lateinit var subscriber: SeedDefaultCategoriesOnUserDefaultCreated

    @Before
    fun setUp() {
        repository = InMemoryCategoryRepository()
        val uuids = SequentialUuidGenerator()
        val eventBus = InMemoryEventBus(emptyList())
        val creator = CategoryCreator(repository, uuids, eventBus)
        val seeder = DefaultCategoriesSeeder(repository, creator)
        subscriber = SeedDefaultCategoriesOnUserDefaultCreated()

        startKoin {
            modules(
                module {
                    single { seeder }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `publishing UserDefaultCreated through the bus seeds the 13 default categories`() = runTest {
        val bus = InMemoryEventBus(listOf<DomainEventSubscriber<out DomainEvent>>(subscriber))

        bus.publish(listOf(newUserDefaultCreatedEvent()))

        val stored = repository.all()
        stored.size shouldBe DefaultCategoriesSeeder.DEFAULTS.size
        stored.map { it.name.value } shouldContainAll listOf(
            "Nómina",
            "Alquiler",
            "Supermercado",
            "Transferencia",
        )
    }

    @Test
    fun `seeding is idempotent — a second event does not duplicate categories`() = runTest {
        val bus = InMemoryEventBus(listOf<DomainEventSubscriber<out DomainEvent>>(subscriber))

        bus.publish(listOf(newUserDefaultCreatedEvent()))
        bus.publish(listOf(newUserDefaultCreatedEvent()))

        repository.all().size shouldBe DefaultCategoriesSeeder.DEFAULTS.size
    }

    private fun newUserDefaultCreatedEvent(): UserDefaultCreated = UserDefaultCreated(
        eventId = "00000000-0000-4000-8000-000000000999",
        aggregateId = "00000000-0000-4000-8000-000000000001",
        occurredOn = Clock.System.now(),
        displayName = "Yo",
        locale = "ES",
        baseCurrency = "EUR",
    )
}
