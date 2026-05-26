package within.means.android.di

import kotlinx.serialization.KSerializer
import org.koin.dsl.module
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
        )
        InMemoryCommandBus(handlers)
    }

    single<QueryBus> {
        val handlers: List<QueryHandler<out Query, out Response>> = listOf(
            get<FindDefaultUserQueryHandler>(),
        )
        InMemoryQueryBus(handlers)
    }

    single {
        val registry: Map<String, KSerializer<out DomainEvent>> = mapOf(
            UserDefaultCreated.NAME to UserDefaultCreated.serializer(),
            UserPreferencesUpdated.NAME to UserPreferencesUpdated.serializer(),
        )
        DomainEventJsonSerializer(serializers = registry)
    }

    single<EventBus> {
        // No subscribers in Phase 3; future contexts (budgets, analytics)
        // will add their subscribers here.
        val subscribers = emptyList<DomainEventSubscriber<out DomainEvent>>()
        EventStoreBackedEventBus(get(), get(), subscribers)
    }
}
