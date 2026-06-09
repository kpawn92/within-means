package within.means.transactions.domain

interface RecurringRuleRepository {
    suspend fun save(rule: RecurringRule)
    suspend fun search(id: RecurringRuleId): RecurringRule?
    suspend fun all(): List<RecurringRule>
    suspend fun allActive(): List<RecurringRule>
    suspend fun countActive(): Long
    suspend fun delete(id: RecurringRuleId)
}
