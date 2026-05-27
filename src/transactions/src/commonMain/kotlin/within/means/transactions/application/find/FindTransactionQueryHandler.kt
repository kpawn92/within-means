package within.means.transactions.application.find

import within.means.shared.domain.bus.query.QueryHandler
import within.means.transactions.application.OptionalTransactionResponse
import within.means.transactions.application.toResponse
import within.means.transactions.domain.TransactionId
import within.means.transactions.domain.TransactionRepository
import kotlin.reflect.KClass

class FindTransactionQueryHandler(
    private val repository: TransactionRepository,
) : QueryHandler<FindTransactionQuery, OptionalTransactionResponse> {

    override val queryType: KClass<FindTransactionQuery> = FindTransactionQuery::class

    override suspend fun handle(query: FindTransactionQuery): OptionalTransactionResponse {
        val transaction = repository.search(TransactionId(query.transactionId))
        return OptionalTransactionResponse(transaction = transaction?.toResponse())
    }
}
