package within.means.categories.infrastructure.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import within.means.categories.db.CategoriesDatabase
import within.means.categories.domain.Category
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.criteria.Criteria

class SqlDelightCategoryRepository(
    private val db: CategoriesDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : CategoryRepository {

    override suspend fun save(category: Category): Unit = withContext(ioDispatcher) {
        db.categoryQueries.upsert(
            id = category.id.value,
            kind = category.kind.name,
            name = category.name.value,
            color = category.color.value,
            icon = category.icon.value,
            nature = category.classifiers.nature?.name,
            essentiality = category.classifiers.essentiality?.name,
            productive = if (category.classifiers.productive) 1L else 0L,
            engel_group = category.classifiers.engelGroup?.name,
            parent_id = category.parentId?.value,
            
            created_at = category.createdAt.toString(),
        )
    }

    override suspend fun search(id: CategoryId): Category? = withContext(ioDispatcher) {
        db.categoryQueries.findById(id.value).executeAsOneOrNull()?.toAggregate()
    }

    override suspend fun all(): List<Category> = withContext(ioDispatcher) {
        db.categoryQueries.findAll().executeAsList().map { it.toAggregate() }
    }

    override suspend fun matching(criteria: Criteria): List<Category> = withContext(ioDispatcher) {
        // For MVP we apply filters in-memory after a `findAll`. Switching
        // to `SqlCriteriaTranslator.translate(...)` with raw SQL is left
        // for when the table grows enough to matter.
        val all = db.categoryQueries.findAll().executeAsList().map { it.toAggregate() }
        applyCriteriaInMemory(all, criteria)
    }

    override suspend fun delete(id: CategoryId): Unit = withContext(ioDispatcher) {
        db.categoryQueries.deleteById(id.value)
    }

    override suspend fun countAll(): Long = withContext(ioDispatcher) {
        db.categoryQueries.countAll().executeAsOne()
    }

    override fun observeAll(): Flow<List<Category>> =
        db.categoryQueries.findAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toAggregate() } }

    private fun applyCriteriaInMemory(items: List<Category>, criteria: Criteria): List<Category> {
        var result = items
        criteria.filters.filters.forEach { filter ->
            result = when (filter.field.value) {
                "kind" -> result.filter { it.kind.name == filter.value.value }
                "name" -> result.filter { it.name.value.contains(filter.value.value, ignoreCase = true) }
                else -> error("Unsupported filter field for categories: ${filter.field.value}")
            }
        }
        if (criteria.order.type != within.means.shared.domain.criteria.OrderType.NONE) {
            val field = criteria.order.field?.value ?: ""
            result = when (field) {
                "name" -> result.sortedBy { it.name.value.lowercase() }
                "kind" -> result.sortedBy { it.kind.name }
                else -> result
            }
            if (criteria.order.type == within.means.shared.domain.criteria.OrderType.DESC) {
                result = result.reversed()
            }
        }
        criteria.offset?.let { result = result.drop(it) }
        criteria.limit?.let { result = result.take(it) }
        return result
    }
}
