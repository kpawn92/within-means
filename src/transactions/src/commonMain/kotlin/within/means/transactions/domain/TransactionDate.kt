package within.means.transactions.domain

import kotlinx.datetime.LocalDate
import within.means.shared.domain.DateValueObject

/**
 * Calendar day on which the movement occurred. Structurally just a date —
 * the business rule "must be today or in the past" depends on a clock and
 * is enforced by the aggregate at register / edit time, not here, so
 * rehydration from storage stays trivial.
 */
class TransactionDate(value: LocalDate) : DateValueObject(value)
