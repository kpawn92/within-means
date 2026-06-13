package within.means.android.capture

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Test
import within.means.categories.domain.Category
import within.means.categories.domain.CategoryClassifiers
import within.means.categories.domain.CategoryColor
import within.means.categories.domain.CategoryIcon
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryKind
import within.means.categories.domain.CategoryName
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.concepts.application.create.ConceptCreator
import within.means.concepts.infrastructure.persistence.InMemoryConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.EventBus

@OptIn(ExperimentalCoroutinesApi::class)
class ConceptsFromCategoriesSeederTest {

    private class SequentialUuidGenerator : UuidGenerator {
        private var counter = 0
        override fun next(): String {
            counter++
            return "00000000-0000-4000-8000-${counter.toString().padStart(12, '0')}"
        }
    }

    private class NoopEventBus : EventBus {
        override suspend fun publish(events: List<DomainEvent>) {}
    }

    private val uuids = SequentialUuidGenerator()
    private val categories = InMemoryCategoryRepository()
    private val concepts = InMemoryConceptRepository()
    private val creator = ConceptCreator(concepts, uuids, NoopEventBus())
    private val seeder = ConceptsFromCategoriesSeeder(categories, concepts, creator)

    private suspend fun category(name: String, kind: CategoryKind): String {
        val id = CategoryId(uuids.next())
        categories.save(
            Category.rehydrate(
                id = id,
                kind = kind,
                name = CategoryName(name),
                color = CategoryColor("#1976D2"),
                icon = CategoryIcon("label"),
                classifiers = CategoryClassifiers(null, null, false, null),
                parentId = null,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            )
        )
        return id.value
    }

    @Test
    fun `seeds one concept per expense or income category, inferring that category`() = runTest {
        val market = category("Mercado", CategoryKind.EXPENSE)
        val salary = category("Nómina", CategoryKind.INCOME)
        category("Transferencia", CategoryKind.TRANSFER) // skipped

        seeder.seedIfNeeded()

        val seeded = concepts.all()
        seeded.map { it.label.value } shouldContainExactlyInAnyOrder listOf("Mercado", "Nómina")
        seeded.single { it.label.value == "Mercado" }.defaultCategoryId shouldBe market
        seeded.single { it.label.value == "Nómina" }.defaultCategoryId shouldBe salary
    }

    @Test
    fun `is idempotent — running twice does not duplicate`() = runTest {
        category("Mercado", CategoryKind.EXPENSE)
        seeder.seedIfNeeded()
        seeder.seedIfNeeded()
        concepts.countAll() shouldBe 1L
    }
}
