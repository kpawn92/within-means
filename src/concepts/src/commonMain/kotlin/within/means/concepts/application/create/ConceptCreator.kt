package within.means.concepts.application.create

import within.means.concepts.domain.Concept
import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptKey
import within.means.concepts.domain.ConceptKind
import within.means.concepts.domain.ConceptLabel
import within.means.concepts.domain.ConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.event.EventBus

/**
 * Creates a concept, enforcing the `(kind, key)` uniqueness invariant in the
 * application layer (the SQL index is non-unique on purpose, so a rename can't
 * crash the insert): typing "cerveza" when "Cerveza" already exists **returns
 * the existing one** instead of duplicating. This is what makes
 * resolve-or-create idempotent for the QuickAdd flow (CONCEPTS-SPEC §9).
 */
class ConceptCreator(
    private val repository: ConceptRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) {
    suspend fun create(command: CreateConceptCommand): ConceptId {
        val kind = ConceptKind.valueOf(command.kind)
        val label = ConceptLabel(command.label)
        val key = ConceptKey.of(label.value)

        repository.findByKey(kind, key)?.let { existing ->
            // Idempotent: same (kind, key) already known — reuse it.
            return existing.id
        }

        val id = command.id?.let { ConceptId(it) } ?: ConceptId(uuids.next())
        val concept = Concept.create(
            id = id,
            kind = kind,
            label = label,
            defaultCategoryId = command.defaultCategoryId,
            uuids = uuids,
        )

        repository.save(concept)
        eventBus.publish(concept.pullDomainEvents())
        return id
    }
}
