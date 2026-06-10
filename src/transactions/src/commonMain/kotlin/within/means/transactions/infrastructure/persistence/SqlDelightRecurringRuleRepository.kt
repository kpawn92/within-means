package within.means.transactions.infrastructure.persistence

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import within.means.transactions.db.TransactionsDatabase
import within.means.transactions.domain.RecurringRule
import within.means.transactions.domain.RecurringRuleId
import within.means.transactions.domain.RecurringRuleRepository

class SqlDelightRecurringRuleRepository(
    private val db: TransactionsDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : RecurringRuleRepository {

    override suspend fun save(rule: RecurringRule): Unit = withContext(ioDispatcher) {
        db.recurringRuleQueries.upsert(
            id = rule.id.value,
            type = rule.type.name,
            amount_cents = rule.amount.cents,
            category_id = rule.categoryRef.value,
            description = rule.description.value,
            income_source = rule.incomeSource?.value,
            frequency = rule.frequency.name,
            start_date = rule.startDate.value.toString(),
            next_occurrence = rule.nextOccurrence.value.toString(),
            active = if (rule.active) 1L else 0L,
            created_at = rule.createdAt.toString(),
        )
    }

    override suspend fun search(id: RecurringRuleId): RecurringRule? = withContext(ioDispatcher) {
        db.recurringRuleQueries.findById(id.value).executeAsOneOrNull()?.toAggregate()
    }

    override suspend fun all(): List<RecurringRule> = withContext(ioDispatcher) {
        db.recurringRuleQueries.findAll().executeAsList().map { it.toAggregate() }
    }

    override suspend fun allActive(): List<RecurringRule> = withContext(ioDispatcher) {
        db.recurringRuleQueries.findActive().executeAsList().map { it.toAggregate() }
    }

    override suspend fun countActive(): Long = withContext(ioDispatcher) {
        db.recurringRuleQueries.countActive().executeAsOne()
    }

    override suspend fun delete(id: RecurringRuleId): Unit = withContext(ioDispatcher) {
        db.recurringRuleQueries.deleteById(id.value)
    }
}
