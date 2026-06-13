package within.means.transactions.domain

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import within.means.transactions.FixedClock
import within.means.transactions.SequentialUuidGenerator
import kotlin.test.Test

class TransactionConceptsTest {

    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 5, 27)
    private val clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z"))
    private val uuids = SequentialUuidGenerator()

    private val anyId = TransactionId("00000000-0000-4000-8000-000000000001")
    private val anyCategory = CategoryRef("00000000-0000-4000-8000-000000000999")

    private fun expense(
        concepts: ConceptRefs = ConceptRefs.EMPTY,
        batch: BatchRef? = null,
    ) = Transaction.register(
        id = anyId,
        type = TransactionType.EXPENSE,
        amount = Amount(7800L),
        date = TransactionDate(today),
        description = TransactionDescription("Carro ruta1 a ruta2"),
        categoryRef = anyCategory,
        conceptRefs = concepts,
        batchRef = batch,
        uuids = uuids,
        clock = clock,
        timeZone = zone,
    )

    @Test
    fun register_carries_concepts_and_batch_into_the_event() {
        val tx = expense(
            concepts = ConceptRefs.of("concept-carro", "concept-papa"),
            batch = BatchRef("batch-1"),
        )

        tx.conceptRefs shouldBe ConceptRefs.of("concept-carro", "concept-papa")
        tx.batchRef?.value shouldBe "batch-1"

        val registered = tx.pullDomainEvents().single().shouldBeInstanceOf<TransactionRegistered>()
        registered.conceptIds shouldBe listOf("concept-carro", "concept-papa")
        registered.batchRef shouldBe "batch-1"
    }

    @Test
    fun register_defaults_to_no_concepts_and_no_batch() {
        val tx = expense()
        tx.conceptRefs.isEmpty shouldBe true
        tx.batchRef shouldBe null
        val registered = tx.pullDomainEvents().single().shouldBeInstanceOf<TransactionRegistered>()
        registered.conceptIds shouldBe emptyList()
        registered.batchRef shouldBe null
    }

    @Test
    fun edit_replaces_the_concepts_and_emits_them() {
        val tx = expense(concepts = ConceptRefs.of("concept-carro")).also { it.pullDomainEvents() }

        tx.edit(
            newAmount = Amount(7800L),
            newDate = TransactionDate(today),
            newTime = null,
            newDescription = TransactionDescription("Carro ruta1 a ruta2"),
            newCategoryRef = anyCategory,
            newIncomeSource = null,
            newConceptRefs = ConceptRefs.of("concept-papa"),
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )

        tx.conceptRefs shouldBe ConceptRefs.of("concept-papa")
        val edited = tx.pullDomainEvents().single().shouldBeInstanceOf<TransactionEdited>()
        edited.conceptIds shouldBe listOf("concept-papa")
    }

    @Test
    fun edit_with_only_a_concept_change_is_not_a_noop() {
        val tx = expense(concepts = ConceptRefs.of("concept-carro")).also { it.pullDomainEvents() }

        tx.edit(
            newAmount = Amount(7800L),
            newDate = TransactionDate(today),
            newTime = null,
            newDescription = TransactionDescription("Carro ruta1 a ruta2"),
            newCategoryRef = anyCategory,
            newIncomeSource = null,
            newConceptRefs = ConceptRefs.of("concept-carro", "concept-papa"),
            uuids = uuids,
            clock = clock,
            timeZone = zone,
        )

        tx.pullDomainEvents() shouldHaveSize 1
    }
}
