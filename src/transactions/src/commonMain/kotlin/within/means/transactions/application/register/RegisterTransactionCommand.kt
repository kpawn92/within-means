package within.means.transactions.application.register

import within.means.shared.domain.bus.command.Command

data class RegisterTransactionCommand(
    val id: String? = null,
    val type: String,
    val amountCents: Long,
    val date: String,
    /** Optional ISO local time (`HH:mm[:ss]`); null means no time-of-day. */
    val time: String? = null,
    val description: String = "",
    val categoryId: String,
    val incomeSource: String? = null,
    val originRef: String? = null,
    val recurringRef: String? = null,
) : Command
