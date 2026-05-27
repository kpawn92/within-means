package within.means.transactions.application.find

import within.means.shared.domain.bus.query.Query

data class FindTransactionQuery(val transactionId: String) : Query
