package within.means.transactions.application.recurring

import within.means.shared.domain.bus.command.Command

data class CreateRecurringRuleCommand(
    val id: String? = null,
    val type: String,
    val amountCents: Long,
    val categoryId: String,
    val description: String = "",
    val incomeSource: String? = null,
    val frequency: String,
    val startDate: String,
) : Command
