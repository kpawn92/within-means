package within.means.transactions.infrastructure.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import within.means.shared.domain.criteria.Criteria
import within.means.shared.domain.criteria.FilterOperator
import within.means.shared.domain.criteria.OrderType
import within.means.transactions.db.TransactionsDatabase
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionRepository

class SqlDelightTransactionRepository(
    private val db: TransactionsDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : TransactionRepository {

    override suspend fun save(transaction: Transaction): Unit = withContext(ioDispatcher) {
        // Row + its concept links go in one transaction: replace-all keeps the
        // bridge in sync on edits (links removed in the UI disappear here too).
        db.transaction {
            db.transactionQueries.upsert(
                id = transaction.id.value,
                type = transaction.type.name,
                amount_cents = transaction.amount.cents,
                date = transaction.date.value.toString(),
                time = transaction.time?.toString(),
                description = transaction.description.value,
                category_id = transaction.categoryRef.value,
                income_source = transaction.incomeSource?.value,
                origin_ref = transaction.originRef?.value,
                recurring_ref = transaction.recurringRef?.value,
                batch_ref = transaction.batchRef?.value,
                created_at = transaction.createdAt.toString(),
            )
            db.transactionConceptQueries.deleteLinksFor(transaction.id.value)
            transaction.conceptRefs.ids.forEachIndexed { position, conceptId ->
                db.transactionConceptQueries.insertLink(
                    transaction_id = transaction.id.value,
                    concept_id = conceptId,
                    position = position.toLong(),
                )
            }
        }
    }

    override suspend fun search(id: TransactionId): Transaction? = withContext(ioDispatcher) {
        db.transactionQueries.findById(id.value).executeAsOneOrNull()?.hydrate()
    }

    override suspend fun all(): List<Transaction> = withContext(ioDispatcher) {
        db.transactionQueries.findAll().executeAsList().map { it.hydrate() }
    }

    override suspend fun matching(criteria: Criteria): List<Transaction> = withContext(ioDispatcher) {
        // MVP: load all rows then apply filters in memory. The Event Store +
        // Criteria translator already exist (Phase 2) but the transactions
        // table is small enough in MVP that pushing this to raw SQL is not
        // worth the migration risk yet.
        val all = db.transactionQueries.findAll().executeAsList().map { it.hydrate() }
        applyCriteriaInMemory(all, criteria)
    }

    override suspend fun delete(id: TransactionId): Unit = withContext(ioDispatcher) {
        db.transaction {
            db.transactionConceptQueries.deleteLinksFor(id.value)
            db.transactionQueries.deleteById(id.value)
        }
    }

    override suspend fun countAll(): Long = withContext(ioDispatcher) {
        db.transactionQueries.countAll().executeAsOne()
    }

    override fun observeAll(): Flow<List<Transaction>> =
        db.transactionQueries.findAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.hydrate() } }

    /** Rehydrates a row, pulling its concept ids from the bridge table. */
    private fun within.means.transactions.db.Transaction_entry.hydrate(): Transaction =
        toAggregate(db.transactionConceptQueries.conceptIdsFor(id).executeAsList())

    private fun applyCriteriaInMemory(items: List<Transaction>, criteria: Criteria): List<Transaction> {
        var result = items
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
}
