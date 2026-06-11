package within.means.transactions.infrastructure.persistence

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import within.means.transactions.db.Transaction_entry as TransactionRow
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.IncomeSource
import within.means.transactions.domain.OriginRef
import within.means.transactions.domain.RecurringRef
import within.means.transactions.domain.Transaction
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionType

internal fun TransactionRow.toAggregate(): Transaction = Transaction.rehydrate(
    id = TransactionId(id),
    type = TransactionType.valueOf(type),
    amount = Amount(amount_cents),
    date = TransactionDate(LocalDate.parse(date)),
    time = time?.let { LocalTime.parse(it) },
    description = TransactionDescription(description),
    categoryRef = CategoryRef(category_id),
    incomeSource = income_source?.let { IncomeSource(it) },
    originRef = origin_ref?.let { OriginRef(it) },
    recurringRef = recurring_ref?.let { RecurringRef(it) },
    createdAt = Instant.parse(created_at),
)
