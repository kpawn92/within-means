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
    /** Opaque concept ids; 0..N, order = relevance (first drives the category). */
    val conceptIds: List<String> = emptyList(),
    val originRef: String? = null,
    val recurringRef: String? = null,
    /** Shared id grouping a batch capture ("vaciar la cesta"); null if standalone. */
    val batchRef: String? = null,
) : Command
