package within.means.android.di

import kotlinx.serialization.KSerializer
import org.koin.dsl.module
import within.means.analytics.application.find_breakdown.FindCategoryBreakdownQueryHandler
import within.means.analytics.application.find_evolution.FindMonthlyEvolutionQueryHandler
import within.means.analytics.application.find_summary.FindCurrentMonthSummaryQueryHandler
import within.means.analytics.application.find_summary.FindMonthlySummaryQueryHandler
import within.means.categories.application.create.CreateCategoryCommandHandler
import within.means.categories.application.delete.DeleteCategoryCommandHandler
import within.means.categories.application.find.FindCategoryQueryHandler
import within.means.categories.application.reclassify.ReclassifyCategoryCommandHandler
import within.means.categories.application.recolor.RestyleCategoryCommandHandler
import within.means.categories.application.rename.RenameCategoryCommandHandler
import within.means.android.subscribers.SeedDefaultCategoriesOnUserDefaultCreated
import within.means.categories.application.search.ListAllCategoriesQueryHandler
import within.means.categories.application.search.SearchCategoriesQueryHandler
import within.means.categories.domain.CategoryCreated
import within.means.categories.domain.CategoryDeleted
import within.means.categories.domain.CategoryReclassified
import within.means.categories.domain.CategoryRenamed
import within.means.categories.domain.CategoryRestyled
import within.means.shared.domain.bus.command.Command
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.DomainEventSubscriber
import within.means.shared.domain.bus.event.EventBus
import within.means.shared.domain.bus.query.Query
import within.means.shared.domain.bus.query.QueryBus
import within.means.shared.domain.bus.query.QueryHandler
import within.means.shared.domain.bus.query.Response
import within.means.shared.infrastructure.bus.command.InMemoryCommandBus
import within.means.shared.infrastructure.bus.event.EventStoreBackedEventBus
import within.means.shared.infrastructure.bus.query.InMemoryQueryBus
import within.means.shared.infrastructure.serialization.DomainEventJsonSerializer
import within.means.transactions.application.delete.DeleteTransactionCommandHandler
import within.means.transactions.application.edit.EditTransactionCommandHandler
import within.means.transactions.application.find.FindTransactionQueryHandler
import within.means.transactions.application.register.RegisterTransactionCommandHandler
import within.means.transactions.application.search.SearchTransactionsQueryHandler
import within.means.transactions.domain.TransactionDeleted
import within.means.transactions.domain.TransactionEdited
import within.means.transactions.domain.TransactionRegistered
import within.means.users.application.ensure_default.EnsureDefaultUserCommandHandler
import within.means.users.application.find.FindDefaultUserQueryHandler
import within.means.users.application.update_preferences.UpdateUserPreferencesCommandHandler
import within.means.users.domain.UserDefaultCreated
import within.means.users.domain.UserPreferencesUpdated

/**
 * Wires the three buses (Command / Query / Event) and the event
 * serializer. Handlers and subscribers are collected via explicit lists
 * — every new context registers its handlers here. No reflection.
 */
val busModule = module {

    single<CommandBus> {
        val handlers: List<CommandHandler<out Command>> = listOf(
            get<EnsureDefaultUserCommandHandler>(),
            get<UpdateUserPreferencesCommandHandler>(),
            get<CreateCategoryCommandHandler>(),
            get<RenameCategoryCommandHandler>(),
            get<RestyleCategoryCommandHandler>(),
            get<ReclassifyCategoryCommandHandler>(),
            get<DeleteCategoryCommandHandler>(),
            get<RegisterTransactionCommandHandler>(),
            get<EditTransactionCommandHandler>(),
            get<DeleteTransactionCommandHandler>(),
        )
        InMemoryCommandBus(handlers)
    }

    single<QueryBus> {
        val handlers: List<QueryHandler<out Query, out Response>> = listOf(
            get<FindDefaultUserQueryHandler>(),
            get<FindCategoryQueryHandler>(),
            get<SearchCategoriesQueryHandler>(),
            get<ListAllCategoriesQueryHandler>(),
            get<FindTransactionQueryHandler>(),
            get<SearchTransactionsQueryHandler>(),
            get<FindMonthlySummaryQueryHandler>(),
            get<FindCurrentMonthSummaryQueryHandler>(),
            get<FindCategoryBreakdownQueryHandler>(),
            get<FindMonthlyEvolutionQueryHandler>(),
        )
        InMemoryQueryBus(handlers)
    }

    single {
        val registry: Map<String, KSerializer<out DomainEvent>> = mapOf(
            UserDefaultCreated.NAME to UserDefaultCreated.serializer(),
            UserPreferencesUpdated.NAME to UserPreferencesUpdated.serializer(),
            CategoryCreated.NAME to CategoryCreated.serializer(),
            CategoryRenamed.NAME to CategoryRenamed.serializer(),
            CategoryRestyled.NAME to CategoryRestyled.serializer(),
            CategoryReclassified.NAME to CategoryReclassified.serializer(),
            CategoryDeleted.NAME to CategoryDeleted.serializer(),
            TransactionRegistered.NAME to TransactionRegistered.serializer(),
            TransactionEdited.NAME to TransactionEdited.serializer(),
            TransactionDeleted.NAME to TransactionDeleted.serializer(),
        )
        DomainEventJsonSerializer(serializers = registry)
    }

    single<EventBus> {
        val subscribers: List<DomainEventSubscriber<out DomainEvent>> = listOf(
            get<SeedDefaultCategoriesOnUserDefaultCreated>(),
        )
        EventStoreBackedEventBus(get(), get(), subscribers)
    }
}
