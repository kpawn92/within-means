package within.means.categories.application

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import within.means.categories.SequentialUuidGenerator
import within.means.categories.application.create.CategoryCreator
import within.means.categories.application.create.CreateCategoryCommand
import within.means.categories.domain.CategoryKind
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.EventBus
import kotlin.test.Test

class CategoryCreatorTest {

    private class CollectingEventBus : EventBus {
        val published = mutableListOf<DomainEvent>()
        override suspend fun publish(events: List<DomainEvent>) {
            published.addAll(events)
        }
    }

    @Test
    fun `creates an INCOME category and publishes one event`() = runTest {
        val repo = InMemoryCategoryRepository()
        val uuids = SequentialUuidGenerator()
        val bus = CollectingEventBus()
        val creator = CategoryCreator(repo, uuids, bus)

        val id = creator.create(
            CreateCategoryCommand(
                kind = "INCOME",
                name = "Nómina",
                color = "#2E7D32",
                icon = "work",
            )
        )

        val saved = repo.search(id)!!
        saved.kind shouldBe CategoryKind.INCOME
        saved.name.value shouldBe "Nómina"
        bus.published.size shouldBe 1
    }

    @Test
    fun `creates an EXPENSE with full classifiers`() = runTest {
        val repo = InMemoryCategoryRepository()
        val uuids = SequentialUuidGenerator()
        val bus = CollectingEventBus()
        val creator = CategoryCreator(repo, uuids, bus)

        val id = creator.create(
            CreateCategoryCommand(
                kind = "EXPENSE",
                name = "Alquiler",
                color = "#1976D2",
                icon = "home",
                nature = "FIXED",
                essentiality = "ESSENTIAL",
                productive = false,
                engelGroup = "HOUSING",
            )
        )
        val saved = repo.search(id)!!
        saved.classifiers.nature shouldNotBe null
        saved.classifiers.essentiality shouldNotBe null
        saved.classifiers.engelGroup shouldNotBe null
    }
}
