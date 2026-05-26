package within.means.users.application.ensure_default

import within.means.shared.domain.bus.command.CommandHandler
import kotlin.reflect.KClass

class EnsureDefaultUserCommandHandler(
    private val bootstrap: DefaultUserBootstrap,
) : CommandHandler<EnsureDefaultUserCommand> {

    override val commandType: KClass<EnsureDefaultUserCommand> = EnsureDefaultUserCommand::class

    override suspend fun handle(command: EnsureDefaultUserCommand) {
        bootstrap.ensure()
    }
}
