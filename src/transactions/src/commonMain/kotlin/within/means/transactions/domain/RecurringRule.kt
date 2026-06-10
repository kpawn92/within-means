package within.means.transactions.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import within.means.shared.domain.AggregateRoot
import within.means.shared.domain.UuidGenerator

/**
 * A template that repeatedly produces transactions on a fixed cadence
 * (weekly / monthly). The rule owns its [nextOccurrence] cursor; a
 * materializer asks it for the dates due up to "today" and advances the
 * cursor past them, which keeps generation idempotent across app launches.
 */
class RecurringRule private constructor(
    val id: RecurringRuleId,
    val type: TransactionType,
    amount: Amount,
    categoryRef: CategoryRef,
    description: TransactionDescription,
    incomeSource: IncomeSource?,
    frequency: RecurrenceFrequency,
    val startDate: TransactionDate,
    nextOccurrence: TransactionDate,
    active: Boolean,
    val createdAt: Instant,
) : AggregateRoot() {

    var amount: Amount = amount
        private set

    var categoryRef: CategoryRef = categoryRef
        private set

    var description: TransactionDescription = description
        private set

    var incomeSource: IncomeSource? = incomeSource
        private set

    var frequency: RecurrenceFrequency = frequency
        private set

    var nextOccurrence: TransactionDate = nextOccurrence
        private set

    var active: Boolean = active
        private set

    /**
     * Dates this rule still owes up to and including [today], without
     * mutating the cursor. Bounded to avoid runaway loops on bad data.
     */
    fun dueDates(today: LocalDate): List<LocalDate> {
        if (!active) return emptyList()
        val dates = mutableListOf<LocalDate>()
        var cursor = nextOccurrence.value
        var guard = 0
        while (cursor <= today && guard < MAX_CATCH_UP) {
            dates += cursor
            cursor = advance(cursor)
            guard++
        }
        return dates
    }

    /** Moves the cursor to the first occurrence strictly after [today]. */
    fun advancePast(today: LocalDate) {
        if (!active) return
        var cursor = nextOccurrence.value
        var guard = 0
        while (cursor <= today && guard < MAX_CATCH_UP) {
            cursor = advance(cursor)
            guard++
        }
        nextOccurrence = TransactionDate(cursor)
    }

    fun deactivate(uuids: UuidGenerator, clock: Clock = Clock.System) {
        if (!active) return
        active = false
        record(
            RecurringRuleDeactivated(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
            )
        )
    }

    /**
     * Edits the rule's details going forward. [type] and [startDate] are fixed
     * (the type ties to the category kind and the cursor); a changed [frequency]
     * applies from the next occurrence onward without disturbing the cursor.
     */
    fun updateDetails(
        amount: Amount,
        categoryRef: CategoryRef,
        description: TransactionDescription,
        incomeSource: IncomeSource?,
        frequency: RecurrenceFrequency,
        uuids: UuidGenerator,
        clock: Clock = Clock.System,
    ) {
        requireIncomeSourceConsistency(type, incomeSource)
        this.amount = amount
        this.categoryRef = categoryRef
        this.description = description
        this.incomeSource = incomeSource
        this.frequency = frequency
        record(
            RecurringRuleUpdated(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
                amountCents = amount.cents,
                categoryId = categoryRef.value,
                description = description.value,
                incomeSource = incomeSource?.value,
                frequency = frequency.name,
            )
        )
    }

    private fun advance(date: LocalDate): LocalDate = when (frequency) {
        RecurrenceFrequency.WEEKLY -> date.plus(DatePeriod(days = 7))
        RecurrenceFrequency.MONTHLY -> date.plus(DatePeriod(months = 1))
    }

    companion object {

        /** Upper bound on occurrences materialized in one pass (safety net). */
        const val MAX_CATCH_UP = 366

        fun create(
            id: RecurringRuleId,
            type: TransactionType,
            amount: Amount,
            categoryRef: CategoryRef,
            description: TransactionDescription = TransactionDescription.EMPTY,
            incomeSource: IncomeSource? = null,
            frequency: RecurrenceFrequency,
            startDate: TransactionDate,
            uuids: UuidGenerator,
            clock: Clock = Clock.System,
        ): RecurringRule {
            requireIncomeSourceConsistency(type, incomeSource)
            val now = clock.now()
            return RecurringRule(
                id = id,
                type = type,
                amount = amount,
                categoryRef = categoryRef,
                description = description,
                incomeSource = incomeSource,
                frequency = frequency,
                startDate = startDate,
                nextOccurrence = startDate,
                active = true,
                createdAt = now,
            ).apply {
                record(
                    RecurringRuleCreated(
                        eventId = uuids.next(),
                        aggregateId = id.value,
                        occurredOn = now,
                        type = type.name,
                        amountCents = amount.cents,
                        categoryId = categoryRef.value,
                        description = description.value,
                        incomeSource = incomeSource?.value,
                        frequency = frequency.name,
                        startDate = startDate.value.toString(),
                    )
                )
            }
        }

        fun rehydrate(
            id: RecurringRuleId,
            type: TransactionType,
            amount: Amount,
            categoryRef: CategoryRef,
            description: TransactionDescription,
            incomeSource: IncomeSource?,
            frequency: RecurrenceFrequency,
            startDate: TransactionDate,
            nextOccurrence: TransactionDate,
            active: Boolean,
            createdAt: Instant,
        ): RecurringRule = RecurringRule(
            id = id,
            type = type,
            amount = amount,
            categoryRef = categoryRef,
            description = description,
            incomeSource = incomeSource,
            frequency = frequency,
            startDate = startDate,
            nextOccurrence = nextOccurrence,
            active = active,
            createdAt = createdAt,
        )

        private fun requireIncomeSourceConsistency(
            type: TransactionType,
            incomeSource: IncomeSource?,
        ) {
            if (type != TransactionType.INCOME) {
                require(incomeSource == null) {
                    "$type recurring rules cannot declare an incomeSource"
                }
            }
        }
    }
}
