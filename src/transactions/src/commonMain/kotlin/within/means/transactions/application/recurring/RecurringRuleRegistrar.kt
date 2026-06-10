package within.means.transactions.application.recurring

import kotlinx.datetime.LocalDate
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.event.EventBus
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.IncomeSource
import within.means.transactions.domain.RecurrenceFrequency
import within.means.transactions.domain.RecurringRule
import within.means.transactions.domain.RecurringRuleId
import within.means.transactions.domain.RecurringRuleRepository
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionType

/**
 * Creates a recurring rule and immediately materializes any occurrences that
 * are already due (so the first instance shows up without waiting for the
 * next app launch).
 */
class RecurringRuleRegistrar(
    private val rules: RecurringRuleRepository,
    private val materializer: RecurringTransactionsMaterializer,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) {
    suspend fun register(command: CreateRecurringRuleCommand): RecurringRuleId {
        val id = command.id?.let { RecurringRuleId(it) } ?: RecurringRuleId(uuids.next())
        val rule = RecurringRule.create(
            id = id,
            type = TransactionType.valueOf(command.type),
            amount = Amount(command.amountCents),
            categoryRef = CategoryRef(command.categoryId),
            description = TransactionDescription(command.description),
            incomeSource = command.incomeSource?.let { IncomeSource(it) },
            frequency = RecurrenceFrequency.valueOf(command.frequency),
            startDate = TransactionDate(LocalDate.parse(command.startDate)),
            uuids = uuids,
        )
        rules.save(rule)
        eventBus.publish(rule.pullDomainEvents())
        materializer.materialize(rule)
        return id
    }
}
