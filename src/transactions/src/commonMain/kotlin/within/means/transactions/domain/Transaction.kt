package within.means.transactions.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import within.means.shared.domain.AggregateRoot
import within.means.shared.domain.UuidGenerator

class Transaction private constructor(
    val id: TransactionId,
    val type: TransactionType,
    amount: Amount,
    date: TransactionDate,
    description: TransactionDescription,
    categoryRef: CategoryRef,
    incomeSource: IncomeSource?,
    val originRef: OriginRef?,
    val recurringRef: RecurringRef?,
    val createdAt: Instant,
) : AggregateRoot() {

    var amount: Amount = amount
        private set
    var date: TransactionDate = date
        private set
    var description: TransactionDescription = description
        private set
    var categoryRef: CategoryRef = categoryRef
        private set
    var incomeSource: IncomeSource? = incomeSource
        private set

    fun edit(
        newAmount: Amount,
        newDate: TransactionDate,
        newDescription: TransactionDescription,
        newCategoryRef: CategoryRef,
        newIncomeSource: IncomeSource?,
        uuids: UuidGenerator,
        clock: Clock = Clock.System,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ) {
        requireDateNotInFuture(newDate, clock, timeZone)
        requireIncomeSourceConsistency(type, newIncomeSource)

        val unchanged = newAmount == this.amount &&
            newDate == this.date &&
            newDescription == this.description &&
            newCategoryRef == this.categoryRef &&
            newIncomeSource == this.incomeSource
        if (unchanged) return

        this.amount = newAmount
        this.date = newDate
        this.description = newDescription
        this.categoryRef = newCategoryRef
        this.incomeSource = newIncomeSource

        record(
            TransactionEdited(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
                amountCents = newAmount.cents,
                date = newDate.value.toString(),
                description = newDescription.value,
                categoryId = newCategoryRef.value,
                incomeSource = newIncomeSource?.value,
            )
        )
    }

    fun markDeleted(uuids: UuidGenerator, clock: Clock = Clock.System) {
        record(
            TransactionDeleted(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
            )
        )
    }

    companion object {

        fun register(
            id: TransactionId,
            type: TransactionType,
            amount: Amount,
            date: TransactionDate,
            description: TransactionDescription = TransactionDescription.EMPTY,
            categoryRef: CategoryRef,
            incomeSource: IncomeSource? = null,
            originRef: OriginRef? = null,
            recurringRef: RecurringRef? = null,
            uuids: UuidGenerator,
            clock: Clock = Clock.System,
            timeZone: TimeZone = TimeZone.currentSystemDefault(),
        ): Transaction {
            requireDateNotInFuture(date, clock, timeZone)
            requireIncomeSourceConsistency(type, incomeSource)

            val now = clock.now()
            return Transaction(
                id = id,
                type = type,
                amount = amount,
                date = date,
                description = description,
                categoryRef = categoryRef,
                incomeSource = incomeSource,
                originRef = originRef,
                recurringRef = recurringRef,
                createdAt = now,
            ).apply {
                record(
                    TransactionRegistered(
                        eventId = uuids.next(),
                        aggregateId = id.value,
                        occurredOn = now,
                        type = type.name,
                        amountCents = amount.cents,
                        date = date.value.toString(),
                        description = description.value,
                        categoryId = categoryRef.value,
                        incomeSource = incomeSource?.value,
                        originRef = originRef?.value,
                        recurringRef = recurringRef?.value,
                    )
                )
            }
        }

        fun rehydrate(
            id: TransactionId,
            type: TransactionType,
            amount: Amount,
            date: TransactionDate,
            description: TransactionDescription,
            categoryRef: CategoryRef,
            incomeSource: IncomeSource?,
            originRef: OriginRef?,
            recurringRef: RecurringRef?,
            createdAt: Instant,
        ): Transaction = Transaction(
            id = id,
            type = type,
            amount = amount,
            date = date,
            description = description,
            categoryRef = categoryRef,
            incomeSource = incomeSource,
            originRef = originRef,
            recurringRef = recurringRef,
            createdAt = createdAt,
        )

        private fun requireDateNotInFuture(
            date: TransactionDate,
            clock: Clock,
            timeZone: TimeZone,
        ) {
            val today: LocalDate = clock.now().toLocalDateTime(timeZone).date
            require(date.value <= today) {
                "TransactionDate cannot be in the future (got ${date.value}, today is $today)"
            }
        }

        private fun requireIncomeSourceConsistency(
            type: TransactionType,
            incomeSource: IncomeSource?,
        ) {
            if (type != TransactionType.INCOME) {
                require(incomeSource == null) {
                    "$type transactions cannot declare an incomeSource"
                }
            }
        }
    }
}
