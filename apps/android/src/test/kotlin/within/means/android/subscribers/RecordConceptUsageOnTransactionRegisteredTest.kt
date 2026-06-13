package within.means.android.subscribers

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import within.means.concepts.application.record_usage.RecordConceptUsageCommand
import within.means.shared.domain.bus.command.Command
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import within.means.transactions.domain.TransactionRegistered

/**
 * Exercises the cross-context wiring: a `TransactionRegistered` carrying concept
 * ids reaches the subscriber, which resolves the CommandBus lazily via Koin (to
 * break the EventBus → subscriber → CommandBus → handler → EventBus cycle) and
 * dispatches one usage-bump per concept, stamped with the movement's instant.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordConceptUsageOnTransactionRegisteredTest {

    private class RecordingCommandBus : CommandBus {
        val commands = mutableListOf<Command>()
        override suspend fun <C : Command> dispatch(command: C) {
            commands.add(command)
        }
    }

    private val recordingBus = RecordingCommandBus()
    private val subscriber = RecordConceptUsageOnTransactionRegistered()

    @Before
    fun setUp() {
        startKoin {
            modules(module { single<CommandBus> { recordingBus } })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun registered(conceptIds: List<String>) = TransactionRegistered(
        eventId = "00000000-0000-4000-8000-000000000001",
        aggregateId = "00000000-0000-4000-8000-000000000002",
        occurredOn = Instant.parse("2026-06-13T10:00:00Z"),
        type = "EXPENSE",
        amountCents = 90,
        date = "2026-06-13",
        description = "Patata",
        categoryId = "00000000-0000-4000-8000-000000000900",
        incomeSource = null,
        conceptIds = conceptIds,
        originRef = null,
        recurringRef = null,
        batchRef = null,
    )

    @Test
    fun `bumps usage once per concept, stamped with the movement instant`() = runTest {
        val bus = InMemoryEventBus(listOf<DomainEventSubscriber<out DomainEvent>>(subscriber))

        bus.publish(listOf(registered(listOf("concept-patata", "concept-papa"))))

        val usages = recordingBus.commands.filterIsInstance<RecordConceptUsageCommand>()
        usages shouldHaveSize 2
        usages.map { it.conceptId } shouldBe listOf("concept-patata", "concept-papa")
        usages.forEach { it.atIso shouldBe "2026-06-13T10:00:00Z" }
    }

    @Test
    fun `does nothing when the movement carries no concepts`() = runTest {
        val bus = InMemoryEventBus(listOf<DomainEventSubscriber<out DomainEvent>>(subscriber))

        bus.publish(listOf(registered(emptyList())))

        recordingBus.commands shouldHaveSize 0
    }
}
