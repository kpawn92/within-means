package within.means.android.capture

import within.means.categories.domain.CategoryRepository
import within.means.concepts.application.create.ConceptCreator
import within.means.concepts.application.create.CreateConceptCommand
import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandBus
import within.means.transactions.application.register.RegisterTransactionCommand

/**
 * The Save use-case behind QuickAdd (CONCEPTS-SPEC §9). Turns what the user
 * typed/tapped — concept *labels* — into a registered movement:
 *
 *  1. resolve each label to a concept id (create if new; idempotent on `(kind,key)`),
 *  2. infer the category from the **first** concept that carries one (D0.3),
 *     falling back to a user override, then to "Otros",
 *  3. dispatch [RegisterTransactionCommand] with the concept ids (and a batch ref
 *     when capturing a basket).
 *
 * Usage counts are NOT bumped here — that happens off the `TransactionRegistered`
 * event in [within.means.android.subscribers.RecordConceptUsageOnTransactionRegistered],
 * so registration stays decoupled from the chip-ranking side effect.
 *
 * Lives in apps/android because it spans `concepts`, `categories` and
 * `transactions`; the KMP modules never reference each other.
 */
class MovementCaptureService(
    private val conceptCreator: ConceptCreator,
    private val concepts: ConceptRepository,
    private val categories: CategoryRepository,
    private val suggester: ConceptCategorySuggester,
    private val fallbackCategory: FallbackCategoryResolver,
    private val commandBus: CommandBus,
    private val uuids: UuidGenerator,
) {

    /** A single line of a batch capture ("vaciar la cesta", §4.4). */
    data class Line(
        val amountCents: Long,
        val conceptLabels: List<String> = emptyList(),
        val categoryOverride: String? = null,
        val description: String = "",
    )

    data class BatchResult(val batchRef: String, val transactionIds: List<String>)

    /** Registers one movement. Returns its id. */
    suspend fun register(
        type: String,
        amountCents: Long,
        date: String,
        time: String? = null,
        description: String = "",
        conceptLabels: List<String> = emptyList(),
        categoryOverride: String? = null,
        incomeSource: String? = null,
    ): String = registerInternal(
        type = type,
        date = date,
        time = time,
        incomeSource = incomeSource,
        line = Line(amountCents, conceptLabels, categoryOverride, description),
        batchRef = null,
    )

    /**
     * Registers N movements that share a generated [BatchResult.batchRef], so the
     * list can group them and "undo the batch" deletes them together (D0.4). They
     * share [type], [date] and [time]; each line keeps its own amount, concepts and
     * inferred category (which is why a basket can't be a single transaction).
     */
    suspend fun registerBatch(
        type: String,
        date: String,
        time: String? = null,
        lines: List<Line>,
    ): BatchResult {
        require(lines.isNotEmpty()) { "A batch capture needs at least one line" }
        val batchRef = uuids.next()
        val ids = lines.map { line ->
            registerInternal(type = type, date = date, time = time, incomeSource = null, line = line, batchRef = batchRef)
        }
        return BatchResult(batchRef = batchRef, transactionIds = ids)
    }

    private suspend fun registerInternal(
        type: String,
        date: String,
        time: String?,
        incomeSource: String?,
        line: Line,
        batchRef: String?,
    ): String {
        val conceptIds = resolveConceptIds(type, line.conceptLabels)
        val categoryId = inferCategoryId(type, conceptIds, line.categoryOverride)

        val id = uuids.next()
        commandBus.dispatch(
            RegisterTransactionCommand(
                id = id,
                type = type,
                amountCents = line.amountCents,
                date = date,
                time = time,
                description = line.description,
                categoryId = categoryId,
                incomeSource = incomeSource,
                conceptIds = conceptIds,
                batchRef = batchRef,
            )
        )
        return id
    }

    /**
     * Labels → distinct concept ids (order preserved), creating new concepts on
     * the fly. TRANSFER carries none. Exposed so the editor can resolve concepts
     * for the recurring/edit paths that don't go through [register].
     */
    suspend fun resolveConceptIds(type: String, labels: List<String>): List<String> {
        val kind = conceptKindFor(type) ?: return emptyList()
        // Same-kind categories feed the suggester so a brand-new concept can be
        // born already mapped (e.g. "gasolina" → Transporte). Existing concepts
        // ignore the suggestion — create is idempotent on (kind, key).
        val sameKindCategories = categories.all().filter { it.kind.name == kind }
        return labels
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { label ->
                val suggested = suggester.suggest(label, sameKindCategories)
                conceptCreator.create(
                    CreateConceptCommand(kind = kind, label = label, defaultCategoryId = suggested)
                ).value
            }
            .distinct()
    }

    /** override → first concept that has a default category → "Otros" (find-or-create). */
    suspend fun inferCategoryId(type: String, conceptIds: List<String>, override: String?): String {
        override?.takeIf { it.isNotBlank() }?.let { return it }
        conceptIds.firstNotNullOfOrNull { concepts.search(ConceptId(it))?.defaultCategoryId }?.let { return it }
        return fallbackCategory.otrosId(type)
    }

    private fun conceptKindFor(type: String): String? = when (type) {
        "EXPENSE", "INCOME" -> type
        else -> null // TRANSFER has no concepts in the MVP (§10-C)
    }
}
