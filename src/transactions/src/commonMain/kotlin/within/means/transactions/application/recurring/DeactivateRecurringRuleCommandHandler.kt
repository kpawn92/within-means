package within.means.transactions.application.recurring

import kotlinx.datetime.Clock
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import within.means.transactions.domain.RecurringRuleId
import within.means.transactions.domain.RecurringRuleRepository
import kotlin.reflect.KClass

/**
 * Deactivates an existing rule via the aggregate (which emits
 * [within.means.transactions.domain.RecurringRuleDeactivated]) and persists
 * the new state. No-op if the rule is missing or already inactive.
 */
class DeactivateRecurringRuleCommandHandler(
    private val rules: RecurringRuleRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
    private val clock: Clock = Clock.System,
) : CommandHandler<DeactivateRecurringRuleCommand> {

    override val commandType: KClass<DeactivateRecurringRuleCommand> =
        DeactivateRecurringRuleCommand::class

    override suspend fun handle(command: DeactivateRecurringRuleCommand) {
        val rule = rules.search(RecurringRuleId(command.id)) ?: return
        rule.deactivate(uuids, clock)
        rules.save(rule)
        eventBus.publish(rule.pullDomainEvents())
    }
}
