package within.means.android.ui.categories

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import within.means.categories.application.create.CategoryCreator
import within.means.categories.application.create.CreateCategoryCommandHandler
import within.means.categories.application.delete.DeleteCategoryCommandHandler
import within.means.categories.application.find.FindCategoryQueryHandler
import within.means.categories.application.reclassify.ReclassifyCategoryCommandHandler
import within.means.categories.application.recolor.RestyleCategoryCommandHandler
import within.means.categories.application.rename.RenameCategoryCommandHandler
import within.means.categories.application.search.ListAllCategoriesQueryHandler
import within.means.categories.application.search.SearchCategoriesQueryHandler
import within.means.categories.domain.CategoryRepository
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.shared.domain.UuidGenerator
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
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import within.means.shared.infrastructure.bus.query.InMemoryQueryBus

/**
 * In-memory wiring that mirrors `categoriesModule` + `busModule` without
 * SQLDelight / SQLCipher. Used by ViewModel unit tests so they exercise the
 * full path (VM → bus → handler → repo) on the JVM without an emulator.
 */
class CategoriesTestFixture(
    val repository: InMemoryCategoryRepository = InMemoryCategoryRepository(),
    val uuids: UuidGenerator = SequentialUuidGenerator(),
    val eventSubscribers: List<DomainEventSubscriber<out DomainEvent>> = emptyList(),
) {
    val eventBus: EventBus = InMemoryEventBus(eventSubscribers)

    private val createHandler = CreateCategoryCommandHandler(CategoryCreator(repository, uuids, eventBus))
    private val renameHandler = RenameCategoryCommandHandler(repository, uuids, eventBus)
    private val restyleHandler = RestyleCategoryCommandHandler(repository, uuids, eventBus)
    private val reclassifyHandler = ReclassifyCategoryCommandHandler(repository, uuids, eventBus)
    private val deleteHandler = DeleteCategoryCommandHandler(repository, uuids, eventBus)

    private val findHandler = FindCategoryQueryHandler(repository)
    private val searchHandler = SearchCategoriesQueryHandler(repository)
    private val listAllHandler = ListAllCategoriesQueryHandler(repository)

    private val commandHandlers: List<CommandHandler<out Command>> = listOf(
        createHandler,
        renameHandler,
        restyleHandler,
        reclassifyHandler,
        deleteHandler,
    )

    private val queryHandlers: List<QueryHandler<out Query, out Response>> = listOf(
        findHandler,
        searchHandler,
        listAllHandler,
    )

    val commandBus: CommandBus = InMemoryCommandBus(commandHandlers)
    val queryBus: QueryBus = InMemoryQueryBus(queryHandlers)

    fun start() {
        startKoin {
            modules(
                module {
                    single<CategoryRepository> { repository }
                    single { commandBus }
                    single { queryBus }
                    single { eventBus }
                    single { uuids }
                }
            )
        }
    }

    fun stop() {
        stopKoin()
    }
}

class SequentialUuidGenerator(start: Int = 0) : UuidGenerator {
    private var counter = start
    override fun next(): String {
        counter++
        val n = counter.toString().padStart(12, '0')
        return "00000000-0000-4000-8000-$n"
    }
}
