package within.means.shared.domain.bus.command

interface CommandBus {
    suspend fun <C : Command> dispatch(command: C)
}
