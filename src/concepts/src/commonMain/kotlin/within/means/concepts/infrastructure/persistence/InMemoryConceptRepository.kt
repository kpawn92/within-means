package within.means.concepts.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import within.means.concepts.domain.Concept
import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptKey
import within.means.concepts.domain.ConceptKind
import within.means.concepts.domain.ConceptRepository

class InMemoryConceptRepository : ConceptRepository {

    private val data = mutableMapOf<ConceptId, Concept>()
    private val flow = MutableStateFlow<List<Concept>>(emptyList())

    override suspend fun save(concept: Concept) {
        data[concept.id] = concept
        flow.value = data.values.toList()
    }

    override suspend fun search(id: ConceptId): Concept? = data[id]

    override suspend fun findByKey(kind: ConceptKind, key: ConceptKey): Concept? =
        data.values.firstOrNull { it.kind == kind && it.key == key }

    override suspend fun byKind(kind: ConceptKind): List<Concept> =
        data.values
            .filter { it.kind == kind }
            .sortedWith(
                compareByDescending<Concept> { it.usageCount }
                    .thenByDescending { it.lastUsedAt }
                    .thenBy { it.label.value.lowercase() }
            )

    override suspend fun all(): List<Concept> = data.values.toList()

    override suspend fun delete(id: ConceptId) {
        data.remove(id)
        flow.value = data.values.toList()
    }

    override suspend fun countAll(): Long = data.size.toLong()

    override fun observeAll(): Flow<List<Concept>> = flow.asStateFlow()
}
