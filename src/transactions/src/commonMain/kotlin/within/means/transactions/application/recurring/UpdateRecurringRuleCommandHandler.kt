package within.means.transactions.application.recurring

import kotlinx.datetime.Clock
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.IncomeSource
import within.means.transactions.domain.RecurrenceFrequency
import within.means.transactions.domain.RecurringRuleId
import within.means.transactions.domain.RecurringRuleRepository
import within.means.transactions.domain.TransactionDescription
import kotlin.reflect.KClass

/**
 * Applies an edit to an existing rule via the aggregate (which validates the
 * income-source/type invariant and emits [within.means.transactions.domain.RecurringRuleUpdated]).
 * No-op when the rule is missing.
 */
class UpdateRecurringRuleCommandHandler(
    private val rules: RecurringRuleRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
    private val clock: Clock = Clock.System,
) : CommandHandler<UpdateRecurringRuleCommand> {

    override val commandType: KClass<UpdateRecurringRuleCommand> =
        UpdateRecurringRuleCommand::class

    override suspend fun handle(command: UpdateRecurringRuleCommand) {
        val rule = rules.search(RecurringRuleId(command.id)) ?: return
        rule.updateDetails(
            amount = Amount(command.amountCents),
            categoryRef = CategoryRef(command.categoryId),
            description = TransactionDescription(command.description),
            incomeSource = command.incomeSource?.let { IncomeSource(it) },
            frequency = RecurrenceFrequency.valueOf(command.frequency),
            uuids = uuids,
            clock = clock,
        )
        rules.save(rule)
        eventBus.publish(rule.pullDomainEvents())
    }
}
