package within.means.shared.infrastructure

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import within.means.shared.domain.bus.command.Command
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.infrastructure.bus.command.InMemoryCommandBus
import kotlin.reflect.KClass
import kotlin.test.Test

class InMemoryCommandBusTest {

    private data class GreetCommand(val name: String) : Command
    private data class FarewellCommand(val name: String) : Command

    private class GreetHandler : CommandHandler<GreetCommand> {
        val greeted = mutableListOf<String>()
        override val commandType: KClass<GreetCommand> = GreetCommand::class
        override suspend fun handle(command: GreetCommand) {
            greeted.add(command.name)
        }
    }

    @Test
    fun `dispatches a command to its registered handler`() = runTest {
        val handler = GreetHandler()
        val bus = InMemoryCommandBus(listOf(handler))

        bus.dispatch(GreetCommand("Alice"))
        bus.dispatch(GreetCommand("Bob"))

        handler.greeted shouldBe listOf("Alice", "Bob")
    }

    @Test
    fun `throws when no handler is registered for a command`() = runTest {
        val bus = InMemoryCommandBus(emptyList())
        shouldThrow<IllegalStateException> {
            bus.dispatch(FarewellCommand("Alice"))
        }
    }

    @Test
    fun `rejects duplicate handlers for the same command`() {
        shouldThrow<IllegalArgumentException> {
            InMemoryCommandBus(listOf(GreetHandler(), GreetHandler()))
        }
    }
}
