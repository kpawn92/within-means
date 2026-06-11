package within.means.transactions.application.edit

import within.means.shared.domain.bus.command.Command

data class EditTransactionCommand(
    val transactionId: String,
    val amountCents: Long,
    val date: String,
    /** Optional ISO local time (`HH:mm[:ss]`); null means no time-of-day. */
    val time: String? = null,
    val description: String,
    val categoryId: String,
    val incomeSource: String? = null,
) : Command
