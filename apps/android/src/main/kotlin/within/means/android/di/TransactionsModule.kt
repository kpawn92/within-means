package within.means.android.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import within.means.transactions.application.delete.DeleteTransactionCommandHandler
import within.means.transactions.application.edit.EditTransactionCommandHandler
import within.means.transactions.application.find.FindTransactionQueryHandler
import within.means.transactions.application.recurring.CreateRecurringRuleCommandHandler
import within.means.transactions.application.recurring.ListActiveRecurringRulesQueryHandler
import within.means.transactions.application.recurring.RecurringRuleRegistrar
import within.means.transactions.application.recurring.RecurringTransactionsMaterializer
import within.means.transactions.application.register.RegisterTransactionCommandHandler
import within.means.transactions.application.register.TransactionRegistrar
import within.means.transactions.application.search.SearchTransactionsQueryHandler

val transactionsModule = module {

    singleOf(::TransactionRegistrar)
    // Materializer takes optional Clock+TimeZone with defaults; explicit factory
    // lets Kotlin defaults apply (system clock + current zone).
    single { RecurringTransactionsMaterializer(get(), get(), get(), get()) }
    singleOf(::RecurringRuleRegistrar)

    singleOf(::RegisterTransactionCommandHandler)
    singleOf(::EditTransactionCommandHandler)
    singleOf(::DeleteTransactionCommandHandler)
    singleOf(::CreateRecurringRuleCommandHandler)

    singleOf(::FindTransactionQueryHandler)
    singleOf(::SearchTransactionsQueryHandler)
    singleOf(::ListActiveRecurringRulesQueryHandler)
}
