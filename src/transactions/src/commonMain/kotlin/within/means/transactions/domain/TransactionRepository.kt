package within.means.transactions.domain

import kotlinx.coroutines.flow.Flow
import within.means.shared.domain.criteria.Criteria

interface TransactionRepository {
    suspend fun save(transaction: Transaction)
    suspend fun search(id: TransactionId): Transaction?
    suspend fun all(): List<Transaction>
    suspend fun matching(criteria: Criteria): List<Transaction>
    suspend fun delete(id: TransactionId)
    suspend fun countAll(): Long
    fun observeAll(): Flow<List<Transaction>>
}
