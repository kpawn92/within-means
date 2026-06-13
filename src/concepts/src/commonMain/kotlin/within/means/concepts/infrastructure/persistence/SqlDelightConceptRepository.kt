package within.means.concepts.infrastructure.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import within.means.concepts.db.ConceptsDatabase
import within.means.concepts.domain.Concept
import within.means.concepts.domain.ConceptId
import within.means.concepts.domain.ConceptKey
import within.means.concepts.domain.ConceptKind
import within.means.concepts.domain.ConceptRepository

class SqlDelightConceptRepository(
    private val db: ConceptsDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : ConceptRepository {

    override suspend fun save(concept: Concept): Unit = withContext(ioDispatcher) {
        db.conceptQueries.upsert(
            id = concept.id.value,
            kind = concept.kind.name,
            label = concept.label.value,
            concept_key = concept.key.value,
            default_category_id = concept.defaultCategoryId,
            usage_count = concept.usageCount.toLong(),
            last_used_at = concept.lastUsedAt?.toString(),
            created_at = concept.createdAt.toString(),
        )
    }

    override suspend fun search(id: ConceptId): Concept? = withContext(ioDispatcher) {
        db.conceptQueries.findById(id.value).executeAsOneOrNull()?.toAggregate()
    }

    override suspend fun findByKey(kind: ConceptKind, key: ConceptKey): Concept? =
        withContext(ioDispatcher) {
            db.conceptQueries.findByKey(kind.name, key.value).executeAsOneOrNull()?.toAggregate()
        }

    override suspend fun byKind(kind: ConceptKind): List<Concept> = withContext(ioDispatcher) {
        db.conceptQueries.findByKind(kind.name).executeAsList().map { it.toAggregate() }
    }

    override suspend fun all(): List<Concept> = withContext(ioDispatcher) {
        db.conceptQueries.findAll().executeAsList().map { it.toAggregate() }
    }

    override suspend fun delete(id: ConceptId): Unit = withContext(ioDispatcher) {
        db.conceptQueries.deleteById(id.value)
    }

    override suspend fun countAll(): Long = withContext(ioDispatcher) {
        db.conceptQueries.countAll().executeAsOne()
    }

    override fun observeAll(): Flow<List<Concept>> =
        db.conceptQueries.findAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toAggregate() } }
}
