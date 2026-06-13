package within.means.concepts.domain

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Instant
import within.means.concepts.SequentialUuidGenerator
import kotlin.test.Test

class ConceptTest {

    private val uuids = SequentialUuidGenerator()

    private fun newConcept(label: String = "Cerveza", category: String? = "cat-1") = Concept.create(
        id = ConceptId(uuids.next()),
        kind = ConceptKind.EXPENSE,
        label = ConceptLabel(label),
        defaultCategoryId = category,
        uuids = uuids,
    )

    @Test
    fun `create emits ConceptCreated and derives the key`() {
        val c = newConcept("Cerveza 🍺")
        c.key.value shouldBe "cerveza"
        c.usageCount shouldBe 0
        val events = c.pullDomainEvents()
        events shouldHaveSize 1
        events.first().shouldBeInstanceOf<ConceptCreated>()
    }

    @Test
    fun `rename updates label and recomputes the key`() {
        val c = newConcept().also { it.pullDomainEvents() }
        c.rename(ConceptLabel("Birra"), uuids)
        c.label.value shouldBe "Birra"
        c.key.value shouldBe "birra"
        c.pullDomainEvents().first().shouldBeInstanceOf<ConceptRenamed>()
    }

    @Test
    fun `rename to the same label is a no-op`() {
        val c = newConcept().also { it.pullDomainEvents() }
        c.rename(c.label, uuids)
        c.pullDomainEvents() shouldHaveSize 0
    }

    @Test
    fun `recordUsage bumps count and keeps the most recent instant`() {
        val c = newConcept().also { it.pullDomainEvents() }
        val t1 = Instant.parse("2026-06-01T10:00:00Z")
        val t2 = Instant.parse("2026-06-10T10:00:00Z")

        c.recordUsage(at = t2, uuids = uuids)
        c.recordUsage(at = t1, uuids = uuids) // older, must not move lastUsedAt back

        c.usageCount shouldBe 2
        c.lastUsedAt shouldBe t2
        c.pullDomainEvents().forEach { it.shouldBeInstanceOf<ConceptUsageRecorded>() }
    }

    @Test
    fun `changeDefaultCategory emits only when it actually changes`() {
        val c = newConcept(category = "cat-1").also { it.pullDomainEvents() }

        c.changeDefaultCategory("cat-1", uuids) // same → no-op
        c.pullDomainEvents() shouldHaveSize 0

        c.changeDefaultCategory("cat-2", uuids)
        c.defaultCategoryId shouldBe "cat-2"
        val ev = c.pullDomainEvents().first()
        ev.shouldBeInstanceOf<ConceptDefaultCategoryChanged>()
        (ev as ConceptDefaultCategoryChanged).defaultCategoryId shouldBe "cat-2"
    }

    @Test
    fun `markDeleted emits ConceptDeleted`() {
        val c = newConcept().also { it.pullDomainEvents() }
        c.markDeleted(uuids)
        c.pullDomainEvents().first().shouldNotBeNull().shouldBeInstanceOf<ConceptDeleted>()
    }
}
