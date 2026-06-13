package within.means.transactions.application.edit

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.ConceptRefs
import within.means.transactions.domain.IncomeSource
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionRepository
import kotlin.reflect.KClass

class EditTransactionCommandHandler(
    private val repository: TransactionRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) : CommandHandler<EditTransactionCommand> {

    override val commandType: KClass<EditTransactionCommand> = EditTransactionCommand::class

    override suspend fun handle(command: EditTransactionCommand) {
        val id = TransactionId(command.transactionId)
        val transaction = repository.search(id)
            ?: error("Transaction not found: ${command.transactionId}")

        transaction.edit(
            newAmount = Amount(command.amountCents),
            newDate = TransactionDate(LocalDate.parse(command.date)),
            newTime = command.time?.let { LocalTime.parse(it) },
            newDescription = TransactionDescription(command.description),
            newCategoryRef = CategoryRef(command.categoryId),
            newIncomeSource = command.incomeSource?.let { IncomeSource(it) },
            newConceptRefs = ConceptRefs(command.conceptIds),
            uuids = uuids,
        )

        repository.save(transaction)
        eventBus.publish(transaction.pullDomainEvents())
    }
}
