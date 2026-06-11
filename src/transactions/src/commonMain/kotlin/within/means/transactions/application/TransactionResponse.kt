package within.means.transactions.application

import within.means.shared.domain.bus.query.Response

data class TransactionResponse(
    val id: String,
    val type: String,
    val amountCents: Long,
    val date: String,
    val time: String?,
    val description: String,
    val categoryId: String,
    val incomeSource: String?,
    val originRef: String?,
    val recurringRef: String?,
    val createdAt: String,
) : Response

data class TransactionsResponse(val items: List<TransactionResponse>) : Response

data class OptionalTransactionResponse(val transaction: TransactionResponse?) : Response
