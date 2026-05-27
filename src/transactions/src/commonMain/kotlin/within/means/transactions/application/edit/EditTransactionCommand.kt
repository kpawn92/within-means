package within.means.transactions.application.edit

import within.means.shared.domain.bus.command.Command

data class EditTransactionCommand(
    val transactionId: String,
    val amountCents: Long,
    val date: String,
    val description: String,
    val categoryId: String,
    val incomeSource: String? = null,
) : Command
