package within.means.concepts.domain

import kotlinx.coroutines.flow.Flow

interface ConceptRepository {
    suspend fun save(concept: Concept)
    suspend fun search(id: ConceptId): Concept?

    /** Identity lookup used to keep `(kind, key)` unique on create (idempotent). */
    suspend fun findByKey(kind: ConceptKind, key: ConceptKey): Concept?

    /** All concepts of a kind, most-used first — the chip-row ordering. */
    suspend fun byKind(kind: ConceptKind): List<Concept>

    suspend fun all(): List<Concept>
    suspend fun delete(id: ConceptId)
    suspend fun countAll(): Long
    fun observeAll(): Flow<List<Concept>>
}
