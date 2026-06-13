package within.means.transactions.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import within.means.shared.domain.criteria.Criteria
import within.means.shared.domain.criteria.FilterOperator
import within.means.shared.domain.criteria.OrderType
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionRepository

class InMemoryTransactionRepository : TransactionRepository {

    private val data = mutableMapOf<TransactionId, Transaction>()
    private val flow = MutableStateFlow<List<Transaction>>(emptyList())

    override suspend fun save(transaction: Transaction) {
        data[transaction.id] = transaction
        flow.value = data.values.toList()
    }

    override suspend fun search(id: TransactionId): Transaction? = data[id]

    override suspend fun all(): List<Transaction> = data.values.toList()

    override suspend fun matching(criteria: Criteria): List<Transaction> {
        var result = data.values.toList()

        criteria.filters.filters.forEach { filter ->
            val raw = filter.value.value
            result = when (filter.field.value) {
                "type" -> result.filter { it.type.name == raw }
                "categoryId" -> result.filter { it.categoryRef.value == raw }
                "conceptId" -> result.filter { raw in it.conceptRefs.ids }
                "date" -> {
                    val pivot = LocalDate.parse(raw)
                    when (filter.operator) {
                        FilterOperator.EQUALS -> result.filter { it.date.value == pivot }
                        FilterOperator.GTE -> result.filter { it.date.value >= pivot }
                        FilterOperator.LTE -> result.filter { it.date.value <= pivot }
                        FilterOperator.GT -> result.filter { it.date.value > pivot }
                        FilterOperator.LT -> result.filter { it.date.value < pivot }
                        else -> error("Unsupported operator for date: ${filter.operator}")
                    }
                }
                "amount" -> {
                    val pivot = raw.toLong()
                    when (filter.operator) {
                        FilterOperator.EQUALS -> result.filter { it.amount.cents == pivot }
                        FilterOperator.GTE -> result.filter { it.amount.cents >= pivot }
                        FilterOperator.LTE -> result.filter { it.amount.cents <= pivot }
                        FilterOperator.GT -> result.filter { it.amount.cents > pivot }
                        FilterOperator.LT -> result.filter { it.amount.cents < pivot }
                        else -> error("Unsupported operator for amount: ${filter.operator}")
                    }
                }
                else -> error("Unsupported filter field for transactions: ${filter.field.value}")
            }
        }

        if (criteria.order.type != OrderType.NONE) {
            val field = criteria.order.field?.value ?: ""
            result = when (field) {
                "date" -> result.sortedBy { it.date.value }
                "amount" -> result.sortedBy { it.amount.cents }
                else -> result
            }
            if (criteria.order.type == OrderType.DESC) result = result.reversed()
        }
        criteria.offset?.let { result = result.drop(it) }
        criteria.limit?.let { result = result.take(it) }
        return result
    }

    override suspend fun delete(id: TransactionId) {
        data.remove(id)
        flow.value = data.values.toList()
    }

    override suspend fun countAll(): Long = data.size.toLong()

    override fun observeAll(): Flow<List<Transaction>> = flow.asStateFlow()
}
