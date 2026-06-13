package within.means.android.capture

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import within.means.categories.application.create.CategoryCreator
import within.means.categories.domain.Category
import within.means.categories.domain.CategoryClassifiers
import within.means.categories.domain.CategoryColor
import within.means.categories.domain.CategoryIcon
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryKind
import within.means.categories.domain.CategoryName
import within.means.categories.domain.EngelGroup
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.concepts.application.create.ConceptCreator
import within.means.concepts.application.create.CreateConceptCommand
import within.means.concepts.infrastructure.persistence.InMemoryConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.Command
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import within.means.transactions.application.register.RegisterTransactionCommand
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovementCaptureServiceTest {

    private class SequentialUuidGenerator : UuidGenerator {
        private var counter = 0
        override fun next(): String {
            counter++
            return "00000000-0000-4000-8000-${counter.toString().padStart(12, '0')}"
        }
    }

    private class RecordingCommandBus : CommandBus {
        val commands = mutableListOf<Command>()
        override suspend fun <C : Command> dispatch(command: C) {
            commands.add(command)
        }

        fun registers(): List<RegisterTransactionCommand> =
            commands.filterIsInstance<RegisterTransactionCommand>()
    }

    private val uuids = SequentialUuidGenerator()
    private val eventBus = InMemoryEventBus(emptyList())
    private val conceptRepo = InMemoryConceptRepository()
    private val conceptCreator = ConceptCreator(conceptRepo, uuids, eventBus)
    private val categoryRepo = InMemoryCategoryRepository()
    private val categoryCreator = CategoryCreator(categoryRepo, uuids, eventBus)
    private val fallback = FallbackCategoryResolver(categoryRepo, categoryCreator)
    private val suggester = ConceptCategorySuggester()
    private val bus = RecordingCommandBus()
    private val service = MovementCaptureService(
        conceptCreator, conceptRepo, categoryRepo, suggester, fallback, bus, uuids,
    )

    private val today = "2026-06-13"

    @Test
    fun `resolves labels to concept ids and dedups normalized variants`() = runTest {
        service.register(
            type = "EXPENSE",
            amountCents = 90,
            date = today,
            conceptLabels = listOf("Patata", "  patata 🥔"),
        )

        val cmd = bus.registers().single()
        cmd.conceptIds shouldHaveSize 1
        conceptRepo.countAll() shouldBe 1L
    }

    @Test
    fun `infers the category from the first concept that has one`() = runTest {
        conceptCreator.create(CreateConceptCommand(kind = "EXPENSE", label = "Cerveza", defaultCategoryId = "cat-food"))

        service.register(type = "EXPENSE", amountCents = 500, date = today, conceptLabels = listOf("Cerveza"))

        bus.registers().single().categoryId shouldBe "cat-food"
    }

    @Test
    fun `falls back to a created Otros category when nothing resolves one`() = runTest {
        service.register(type = "EXPENSE", amountCents = 70, date = today, conceptLabels = listOf("Detergente"))

        val otros = categoryRepo.all().single { it.name.value == "Otros" }
        otros.kind.name shouldBe "EXPENSE"
        bus.registers().single().categoryId shouldBe otros.id.value
    }

    @Test
    fun `an explicit category override wins over inference`() = runTest {
        service.register(
            type = "EXPENSE",
            amountCents = 70,
            date = today,
            conceptLabels = listOf("Detergente"),
            categoryOverride = "cat-manual",
        )

        bus.registers().single().categoryId shouldBe "cat-manual"
    }

    @Test
    fun `batch capture shares one batch ref across all lines`() = runTest {
        val result = service.registerBatch(
            type = "EXPENSE",
            date = today,
            lines = listOf(
                MovementCaptureService.Line(amountCents = 90, conceptLabels = listOf("Patata")),
                MovementCaptureService.Line(amountCents = 70, conceptLabels = listOf("Detergente")),
                MovementCaptureService.Line(amountCents = 78, conceptLabels = listOf("Carro ruta1 a ruta2")),
            ),
        )

        val registers = bus.registers()
        registers shouldHaveSize 3
        registers.map { it.batchRef }.toSet() shouldBe setOf(result.batchRef)
        result.transactionIds shouldHaveSize 3
    }

    @Test
    fun `a new concept is born mapped to a suggested category`() = runTest {
        categoryRepo.save(
            Category.rehydrate(
                id = CategoryId("00000000-0000-4000-8000-000000000900"),
                kind = CategoryKind.EXPENSE,
                name = CategoryName("Transporte"),
                color = CategoryColor("#0288D1"),
                icon = CategoryIcon("label"),
                classifiers = CategoryClassifiers(null, null, false, EngelGroup.TRANSPORT),
                parentId = null,
                createdAt = kotlinx.datetime.Instant.parse("2026-01-01T00:00:00Z"),
            )
        )

        service.register(type = "EXPENSE", amountCents = 7800, date = today, conceptLabels = listOf("gasolina"))

        val concept = conceptRepo.all().single { it.label.value == "gasolina" }
        val transporte = categoryRepo.all().single { it.name.value == "Transporte" }
        concept.defaultCategoryId shouldBe transporte.id.value
        bus.registers().single().categoryId shouldBe transporte.id.value
    }

    @Test
    fun `previewCategoryId infers without creating a concept`() = runTest {
        categoryRepo.save(
            Category.rehydrate(
                id = CategoryId("00000000-0000-4000-8000-000000000901"),
                kind = CategoryKind.EXPENSE,
                name = CategoryName("Transporte"),
                color = CategoryColor("#0288D1"),
                icon = CategoryIcon("label"),
                classifiers = CategoryClassifiers(null, null, false, EngelGroup.TRANSPORT),
                parentId = null,
                createdAt = kotlinx.datetime.Instant.parse("2026-01-01T00:00:00Z"),
            )
        )

        val catId = service.previewCategoryId("EXPENSE", "gasolina")

        catId shouldBe "00000000-0000-4000-8000-000000000901"
        // The preview must be a pure read — no concept persisted, no command sent.
        conceptRepo.countAll() shouldBe 0L
        bus.registers() shouldHaveSize 0
    }

    @Test
    fun `previewCategoryId returns null when nothing matches (caller shows Otros)`() = runTest {
        service.previewCategoryId("EXPENSE", "xyzzy") shouldBe null
        conceptRepo.countAll() shouldBe 0L
    }

    @Test
    fun `TRANSFER carries no concepts`() = runTest {
        service.register(type = "TRANSFER", amountCents = 100, date = today, conceptLabels = listOf("ignored"))

        bus.registers().single().conceptIds shouldHaveSize 0
        conceptRepo.countAll() shouldBe 0L
    }
}
