package within.means.transactions.application.delete

import within.means.shared.domain.bus.command.Command

data class DeleteTransactionCommand(val transactionId: String) : Command
