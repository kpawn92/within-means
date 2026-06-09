package within.means.transactions.application.recurring

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.event.EventBus
import within.means.transactions.domain.OriginRef
import within.means.transactions.domain.RecurringRef
import within.means.transactions.domain.RecurringRule
import within.means.transactions.domain.RecurringRuleRepository
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionRepository

/**
 * Turns due recurring-rule occurrences into real transactions. Idempotent:
 * each rule's [RecurringRule.nextOccurrence] cursor only ever moves forward,
 * so re-running (e.g. on every app launch) never duplicates a date. Catch-up
 * is bounded by [RecurringRule.MAX_CATCH_UP] inside the aggregate.
 */
class RecurringTransactionsMaterializer(
    private val transactions: TransactionRepository,
    private val rules: RecurringRuleRepository,
    private val uuids: UuidGenerator,
    private val eventBus: EventBus,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    /** Materializes every active rule that owes one or more occurrences. */
    suspend fun materializeAllActive() {
        rules.allActive().forEach { materialize(it) }
    }

    /** Materializes a single rule and persists its advanced cursor. */
    suspend fun materialize(rule: RecurringRule) {
        val today = clock.now().toLocalDateTime(timeZone).date
        val due = rule.dueDates(today)
        if (due.isEmpty()) return

        for (date in due) {
            val transaction = Transaction.register(
                id = TransactionId(uuids.next()),
                type = rule.type,
                amount = rule.amount,
                date = TransactionDate(date),
                description = rule.description,
                categoryRef = rule.categoryRef,
                incomeSource = rule.incomeSource,
                originRef = OriginRef(ORIGIN),
                recurringRef = RecurringRef(rule.id.value),
                uuids = uuids,
                clock = clock,
                timeZone = timeZone,
            )
            transactions.save(transaction)
            eventBus.publish(transaction.pullDomainEvents())
        }

        rule.advancePast(today)
        rules.save(rule)
    }

    private companion object {
        const val ORIGIN = "recurring"
    }
}
