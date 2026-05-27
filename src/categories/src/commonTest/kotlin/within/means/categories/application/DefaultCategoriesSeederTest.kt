package within.means.categories.application

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import within.means.categories.SequentialUuidGenerator
import within.means.categories.application.create.CategoryCreator
import within.means.categories.application.seed.DefaultCategoriesSeeder
import within.means.categories.domain.CategoryKind
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.EventBus
import kotlin.test.Test

class DefaultCategoriesSeederTest {

    private class CollectingEventBus : EventBus {
        val published = mutableListOf<DomainEvent>()
        override suspend fun publish(events: List<DomainEvent>) {
            published.addAll(events)
        }
    }

    @Test
    fun `seedIfNeeded creates the default set the first time`() = runTest {
        val repo = InMemoryCategoryRepository()
        val uuids = SequentialUuidGenerator()
        val bus = CollectingEventBus()
        val creator = CategoryCreator(repo, uuids, bus)
        val seeder = DefaultCategoriesSeeder(repo, creator)

        seeder.seedIfNeeded()

        val all = repo.all()
        all.size shouldBe DefaultCategoriesSeeder.DEFAULTS.size
        all.map { it.name.value } shouldContain "Nómina"
        all.map { it.name.value } shouldContain "Alquiler"
        all.any { it.kind == CategoryKind.TRANSFER } shouldBe true
    }

    @Test
    fun `seedIfNeeded is a no-op when categories already exist`() = runTest {
        val repo = InMemoryCategoryRepository()
        val uuids = SequentialUuidGenerator()
        val bus = CollectingEventBus()
        val creator = CategoryCreator(repo, uuids, bus)
        val seeder = DefaultCategoriesSeeder(repo, creator)

        seeder.seedIfNeeded()
        val initialCount = repo.all().size

        seeder.seedIfNeeded()

        repo.all().size shouldBe initialCount
    }
}
