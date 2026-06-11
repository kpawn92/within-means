package within.means.transactions.application.register

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.event.EventBus
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.IncomeSource
import within.means.transactions.domain.OriginRef
import within.means.transactions.domain.RecurringRef
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionRepository
import within.means.transactions.domain.TransactionType

class TransactionRegistrar(
    private val repository: TransactionRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
) {
    suspend fun register(command: RegisterTransactionCommand): TransactionId {
        val id = command.id?.let { TransactionId(it) } ?: TransactionId(uuids.next())
        val transaction = Transaction.register(
            id = id,
            type = TransactionType.valueOf(command.type),
            amount = Amount(command.amountCents),
            date = TransactionDate(LocalDate.parse(command.date)),
            time = command.time?.let { LocalTime.parse(it) },
            description = TransactionDescription(command.description),
            categoryRef = CategoryRef(command.categoryId),
            incomeSource = command.incomeSource?.let { IncomeSource(it) },
            originRef = command.originRef?.let { OriginRef(it) },
            recurringRef = command.recurringRef?.let { RecurringRef(it) },
            uuids = uuids,
        )
        repository.save(transaction)
        eventBus.publish(transaction.pullDomainEvents())
        return id
    }
}
