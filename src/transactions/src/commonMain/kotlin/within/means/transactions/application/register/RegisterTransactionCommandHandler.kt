package within.means.transactions.application.register

import within.means.shared.domain.bus.command.CommandHandler
import kotlin.reflect.KClass

class RegisterTransactionCommandHandler(
    private val registrar: TransactionRegistrar,
) : CommandHandler<RegisterTransactionCommand> {

    override val commandType: KClass<RegisterTransactionCommand> = RegisterTransactionCommand::class

    override suspend fun handle(command: RegisterTransactionCommand) {
        registrar.register(command)
    }
}
