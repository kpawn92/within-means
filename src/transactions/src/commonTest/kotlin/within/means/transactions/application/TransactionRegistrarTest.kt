package within.means.transactions.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import within.means.transactions.SequentialUuidGenerator
import within.means.transactions.application.register.RegisterTransactionCommand
import within.means.transactions.application.register.TransactionRegistrar
import within.means.transactions.domain.TransactionRegistered
import within.means.transactions.infrastructure.persistence.InMemoryTransactionRepository

class TransactionRegistrarTest {

    @Test
    fun register_persists_and_publishes_TransactionRegistered() = runTest {
        val repo = InMemoryTransactionRepository()
        val capturing = CapturingSubscriber()
        val bus = InMemoryEventBus(listOf(capturing))
        val registrar = TransactionRegistrar(repo, SequentialUuidGenerator(), bus)

        val id = registrar.register(
            RegisterTransactionCommand(
                type = "EXPENSE",
                amountCents = 1500L,
                date = "2026-05-20",
                description = "Comida",
                categoryId = "00000000-0000-4000-8000-000000000999",
            )
        )

        repo.search(id)!!.amount.cents shouldBe 1500L
        capturing.events.single().shouldBeInstanceOf<TransactionRegistered>().categoryId shouldBe
            "00000000-0000-4000-8000-000000000999"
    }

    @Test
    fun register_propagates_domain_invariants() = runTest {
        val registrar = TransactionRegistrar(
            InMemoryTransactionRepository(),
            SequentialUuidGenerator(),
            InMemoryEventBus(emptyList()),
        )

        shouldThrow<IllegalArgumentException> {
            registrar.register(
                RegisterTransactionCommand(
                    type = "EXPENSE",
                    amountCents = 0L, // structural invariant violation in Amount
                    date = "2026-05-20",
                    categoryId = "00000000-0000-4000-8000-000000000999",
                )
            )
        }
    }
}
