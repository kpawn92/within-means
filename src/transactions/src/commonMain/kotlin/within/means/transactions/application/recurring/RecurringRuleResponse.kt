package within.means.transactions.application.recurring

import within.means.shared.domain.bus.query.Response
import within.means.transactions.domain.RecurringRule

data class RecurringRuleResponse(
    val id: String,
    val type: String,
    val amountCents: Long,
    val categoryId: String,
    val description: String,
    val incomeSource: String?,
    val frequency: String,
    val startDate: String,
    val nextOccurrence: String,
    val active: Boolean,
) : Response

data class RecurringRulesResponse(val items: List<RecurringRuleResponse>) : Response

fun RecurringRule.toResponse(): RecurringRuleResponse = RecurringRuleResponse(
    id = id.value,
    type = type.name,
    amountCents = amount.cents,
    categoryId = categoryRef.value,
    description = description.value,
    incomeSource = incomeSource?.value,
    frequency = frequency.name,
    startDate = startDate.value.toString(),
    nextOccurrence = nextOccurrence.value.toString(),
    active = active,
)
