package within.means.transactions.infrastructure.persistence

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import within.means.transactions.db.Recurring_rule as RecurringRuleRow
import within.means.transactions.domain.Amount
import within.means.transactions.domain.CategoryRef
import within.means.transactions.domain.IncomeSource
import within.means.transactions.domain.RecurrenceFrequency
import within.means.transactions.domain.RecurringRule
import within.means.transactions.domain.RecurringRuleId
import within.means.transactions.domain.TransactionDate
import within.means.transactions.domain.TransactionDescription
import within.means.transactions.domain.TransactionType

internal fun RecurringRuleRow.toAggregate(): RecurringRule = RecurringRule.rehydrate(
    id = RecurringRuleId(id),
    type = TransactionType.valueOf(type),
    amount = Amount(amount_cents),
    categoryRef = CategoryRef(category_id),
    description = TransactionDescription(description),
    incomeSource = income_source?.let { IncomeSource(it) },
    frequency = RecurrenceFrequency.valueOf(frequency),
    startDate = TransactionDate(LocalDate.parse(start_date)),
    nextOccurrence = TransactionDate(LocalDate.parse(next_occurrence)),
    active = active == 1L,
    createdAt = Instant.parse(created_at),
)
