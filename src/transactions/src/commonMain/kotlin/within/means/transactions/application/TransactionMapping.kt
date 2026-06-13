package within.means.transactions.application

import within.means.transactions.domain.Transaction

fun Transaction.toResponse(): TransactionResponse = TransactionResponse(
    id = id.value,
    type = type.name,
    amountCents = amount.cents,
    date = date.value.toString(),
    time = time?.toString(),
    description = description.value,
    categoryId = categoryRef.value,
    incomeSource = incomeSource?.value,
    conceptIds = conceptRefs.ids,
    originRef = originRef?.value,
    recurringRef = recurringRef?.value,
    batchRef = batchRef?.value,
    createdAt = createdAt.toString(),
)
