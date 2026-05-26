package within.means.shared.infrastructure.bus.command

import within.means.shared.domain.bus.command.Command
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.command.CommandHandler

class InMemoryCommandBus(handlers: List<CommandHandler<out Command>>) : CommandBus {

    private val registry = buildMap {
        handlers.forEach { handler ->
            val existing = put(handler.commandType, handler)
            require(existing == null) {
                "Duplicate handler registered for command ${handler.commandType.simpleName}"
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <C : Command> dispatch(command: C) {
        val handler = registry[command::class] as? CommandHandler<C>
            ?: error("No handler registered for command ${command::class.simpleName}")
        handler.handle(command)
    }
}
