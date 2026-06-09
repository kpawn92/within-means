package within.means.transactions.infrastructure.persistence

import within.means.transactions.domain.RecurringRule
import within.means.transactions.domain.RecurringRuleId
import within.means.transactions.domain.RecurringRuleRepository

class InMemoryRecurringRuleRepository : RecurringRuleRepository {

    private val data = mutableMapOf<RecurringRuleId, RecurringRule>()

    override suspend fun save(rule: RecurringRule) {
        data[rule.id] = rule
    }

    override suspend fun search(id: RecurringRuleId): RecurringRule? = data[id]

    override suspend fun all(): List<RecurringRule> = data.values.toList()

    override suspend fun allActive(): List<RecurringRule> = data.values.filter { it.active }

    override suspend fun countActive(): Long = data.values.count { it.active }.toLong()

    override suspend fun delete(id: RecurringRuleId) {
        data.remove(id)
    }
}
