package within.means.categories.domain

import kotlinx.coroutines.flow.Flow
import within.means.shared.domain.criteria.Criteria

interface CategoryRepository {
    suspend fun save(category: Category)
    suspend fun search(id: CategoryId): Category?
    suspend fun all(): List<Category>
    suspend fun matching(criteria: Criteria): List<Category>
    suspend fun delete(id: CategoryId)
    suspend fun countAll(): Long
    fun observeAll(): Flow<List<Category>>
}
