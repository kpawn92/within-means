package within.means.android.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import within.means.transactions.application.delete.DeleteTransactionCommandHandler
import within.means.transactions.application.edit.EditTransactionCommandHandler
import within.means.transactions.application.find.FindTransactionQueryHandler
import within.means.transactions.application.register.RegisterTransactionCommandHandler
import within.means.transactions.application.register.TransactionRegistrar
import within.means.transactions.application.search.SearchTransactionsQueryHandler

val transactionsModule = module {

    singleOf(::TransactionRegistrar)

    singleOf(::RegisterTransactionCommandHandler)
    singleOf(::EditTransactionCommandHandler)
    singleOf(::DeleteTransactionCommandHandler)

    singleOf(::FindTransactionQueryHandler)
    singleOf(::SearchTransactionsQueryHandler)
}
