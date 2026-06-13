package within.means.concepts.application

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import within.means.concepts.SequentialUuidGenerator
import within.means.concepts.application.create.ConceptCreator
import within.means.concepts.application.create.CreateConceptCommand
import within.means.concepts.domain.ConceptKind
import within.means.concepts.infrastructure.persistence.InMemoryConceptRepository
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.EventBus
import kotlin.test.Test

class ConceptCreatorTest {

    private class CollectingEventBus : EventBus {
        val published = mutableListOf<DomainEvent>()
        override suspend fun publish(events: List<DomainEvent>) {
            published.addAll(events)
        }
    }

    @Test
    fun `creates an expense concept and publishes one event`() = runTest {
        val repo = InMemoryConceptRepository()
        val bus = CollectingEventBus()
        val creator = ConceptCreator(repo, SequentialUuidGenerator(), bus)

        val id = creator.create(
            CreateConceptCommand(kind = "EXPENSE", label = "Cerveza", defaultCategoryId = "cat-food")
        )

        val saved = repo.search(id)!!
        saved.kind shouldBe ConceptKind.EXPENSE
        saved.label.value shouldBe "Cerveza"
        saved.key.value shouldBe "cerveza"
        saved.defaultCategoryId shouldBe "cat-food"
        bus.published.size shouldBe 1
    }

    @Test
    fun `is idempotent on the normalized key — same kind reuses the existing id`() = runTest {
        val repo = InMemoryConceptRepository()
        val bus = CollectingEventBus()
        val creator = ConceptCreator(repo, SequentialUuidGenerator(), bus)

        val first = creator.create(CreateConceptCommand(kind = "EXPENSE", label = "Cerveza"))
        val again = creator.create(CreateConceptCommand(kind = "EXPENSE", label = "  cerveza 🍺"))

        again shouldBe first
        repo.countAll() shouldBe 1L
        bus.published.size shouldBe 1 // only the first created an event
    }

    @Test
    fun `same key under a different kind is a distinct concept`() = runTest {
        val repo = InMemoryConceptRepository()
        val creator = ConceptCreator(repo, SequentialUuidGenerator(), CollectingEventBus())

        val expense = creator.create(CreateConceptCommand(kind = "EXPENSE", label = "Extra"))
        val income = creator.create(CreateConceptCommand(kind = "INCOME", label = "Extra"))

        (expense == income) shouldBe false
        repo.countAll() shouldBe 2L
    }
}
