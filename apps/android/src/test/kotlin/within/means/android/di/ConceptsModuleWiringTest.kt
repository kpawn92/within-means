package within.means.android.di

import org.junit.After
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import within.means.android.capture.FallbackCategoryResolver
import within.means.android.capture.MovementCaptureService
import within.means.categories.application.create.CategoryCreator
import within.means.categories.domain.CategoryRepository
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.concepts.application.create.ConceptCreator
import within.means.concepts.application.create.CreateConceptCommandHandler
import within.means.concepts.application.delete.DeleteConceptCommandHandler
import within.means.concepts.application.find.FindConceptQueryHandler
import within.means.concepts.application.recategorize.SetConceptDefaultCategoryCommandHandler
import within.means.concepts.application.record_usage.RecordConceptUsageCommandHandler
import within.means.concepts.application.rename.RenameConceptCommandHandler
import within.means.concepts.application.suggest.ListAllConceptsQueryHandler
import within.means.concepts.application.suggest.SuggestConceptsQueryHandler
import within.means.concepts.domain.ConceptRepository
import within.means.concepts.infrastructure.persistence.InMemoryConceptRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.Command
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.event.DomainEvent
import within.means.shared.domain.bus.event.EventBus
import within.means.shared.infrastructure.RealUuidGenerator

/**
 * Wiring guard for [conceptsModule]: starts Koin with the module + fakes for its
 * leaf dependencies and resolves every definition. Catches the class of bug that
 * unit tests miss because they construct handlers by hand — e.g. registering a
 * handler with an optional `Clock = Clock.System` via `singleOf`, which makes
 * Koin try (and fail) to resolve `Clock` from the graph (see
 * RecordConceptUsageCommandHandler). This blew up the whole CommandBus at runtime
 * but no unit test saw it.
 */
class ConceptsModuleWiringTest {

    private class NoopCommandBus : CommandBus {
        override suspend fun <C : Command> dispatch(command: C) {}
    }

    private class NoopEventBus : EventBus {
        override suspend fun publish(events: List<DomainEvent>) {}
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun `every concepts definition resolves with no missing dependencies`() {
        val fakes = module {
            single<ConceptRepository> { InMemoryConceptRepository() }
            single<CategoryRepository> { InMemoryCategoryRepository() }
            single<UuidGenerator> { RealUuidGenerator() }
            single<EventBus> { NoopEventBus() }
            single<CommandBus> { NoopCommandBus() }
            // FallbackCategoryResolver needs a CategoryCreator (lives in categoriesModule).
            single { CategoryCreator(get(), get(), get()) }
        }

        val koin = startKoin { modules(fakes, conceptsModule) }.koin

        // Resolving each forces Koin to satisfy its constructor graph. A missing
        // definition (like Clock for a `singleOf` handler) throws here.
        koin.get<ConceptCreator>()
        koin.get<CreateConceptCommandHandler>()
        koin.get<RecordConceptUsageCommandHandler>()
        koin.get<SetConceptDefaultCategoryCommandHandler>()
        koin.get<RenameConceptCommandHandler>()
        koin.get<DeleteConceptCommandHandler>()
        koin.get<FindConceptQueryHandler>()
        koin.get<SuggestConceptsQueryHandler>()
        koin.get<ListAllConceptsQueryHandler>()
        koin.get<FallbackCategoryResolver>()
        koin.get<MovementCaptureService>()
    }
}
