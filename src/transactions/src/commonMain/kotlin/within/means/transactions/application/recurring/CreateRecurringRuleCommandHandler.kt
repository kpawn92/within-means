package within.means.transactions.application.recurring

import within.means.shared.domain.bus.command.CommandHandler
import kotlin.reflect.KClass

class CreateRecurringRuleCommandHandler(
    private val registrar: RecurringRuleRegistrar,
) : CommandHandler<CreateRecurringRuleCommand> {

    override val commandType: KClass<CreateRecurringRuleCommand> = CreateRecurringRuleCommand::class

    override suspend fun handle(command: CreateRecurringRuleCommand) {
        registrar.register(command)
    }
}
