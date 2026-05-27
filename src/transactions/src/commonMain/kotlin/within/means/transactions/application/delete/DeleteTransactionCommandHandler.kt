package within.means.transactions.application.delete

import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionRepository
import kotlin.reflect.KClass

class DeleteTransactionCommandHandler(
    private val repository: TransactionRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) : CommandHandler<DeleteTransactionCommand> {

    override val commandType: KClass<DeleteTransactionCommand> = DeleteTransactionCommand::class

    override suspend fun handle(command: DeleteTransactionCommand) {
        val id = TransactionId(command.transactionId)
        val transaction = repository.search(id) ?: return
        transaction.markDeleted(uuids)
        repository.delete(id)
        eventBus.publish(transaction.pullDomainEvents())
    }
}
