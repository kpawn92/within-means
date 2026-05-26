package within.means.shared.domain.bus.command

import kotlin.reflect.KClass

interface CommandHandler<C : Command> {
    val commandType: KClass<C>
    suspend fun handle(command: C)
}
