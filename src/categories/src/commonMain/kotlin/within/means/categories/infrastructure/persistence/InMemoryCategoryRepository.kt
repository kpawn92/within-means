package within.means.categories.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import within.means.categories.domain.Category
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryRepository
import within.means.shared.domain.criteria.Criteria

class InMemoryCategoryRepository : CategoryRepository {

    private val data = mutableMapOf<CategoryId, Category>()
    private val flow = MutableStateFlow<List<Category>>(emptyList())

    override suspend fun save(category: Category) {
        data[category.id] = category
        flow.value = data.values.toList()
    }

    override suspend fun search(id: CategoryId): Category? = data[id]

    override suspend fun all(): List<Category> = data.values.toList()

    override suspend fun matching(criteria: Criteria): List<Category> {
        var result = data.values.toList()
        criteria.filters.filters.forEach { filter ->
            result = when (filter.field.value) {
                "kind" -> result.filter { it.kind.name == filter.value.value }
                else -> result
            }
        }
        return result
    }

    override suspend fun delete(id: CategoryId) {
        data.remove(id)
        flow.value = data.values.toList()
    }

    override suspend fun countAll(): Long = data.size.toLong()

    override fun observeAll(): Flow<List<Category>> = flow.asStateFlow()
}
