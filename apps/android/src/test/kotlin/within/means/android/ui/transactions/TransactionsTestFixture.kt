package within.means.android.ui.transactions

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import within.means.android.capture.FallbackCategoryResolver
import within.means.android.capture.MovementCaptureService
import within.means.android.ui.categories.SequentialUuidGenerator
import within.means.categories.application.create.CategoryCreator
import within.means.categories.application.create.CreateCategoryCommand
import within.means.categories.application.create.CreateCategoryCommandHandler
import within.means.categories.application.search.ListAllCategoriesQueryHandler
import within.means.categories.application.search.SearchCategoriesQueryHandler
import within.means.categories.domain.CategoryRepository
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.concepts.application.create.ConceptCreator
import within.means.concepts.application.find.FindConceptQueryHandler
import within.means.concepts.application.record_usage.RecordConceptUsageCommandHandler
import within.means.concepts.application.suggest.ListAllConceptsQueryHandler
import within.means.concepts.application.suggest.SuggestConceptsQueryHandler
import within.means.concepts.domain.ConceptRepository
import within.means.concepts.infrastructure.persistence.InMemoryConceptRepository
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
import within.means.transactions.application.delete.DeleteTransactionCommandHandler
import within.means.transactions.application.edit.EditTransactionCommandHandler
import within.means.transactions.application.find.FindTransactionQueryHandler
import within.means.transactions.application.register.RegisterTransactionCommandHandler
import within.means.transactions.application.register.TransactionRegistrar
import within.means.transactions.application.search.SearchTransactionsQueryHandler
import within.means.transactions.domain.TransactionRepository
import within.means.transactions.infrastructure.persistence.InMemoryTransactionRepository

/**
 * In-memory mirror of `transactionsModule + categoriesModule + busModule`
 * without SQLDelight / SQLCipher. Categories are included because the
 * transactions UI looks them up to render names and pickers.
 */
class TransactionsTestFixture(
    val txRepository: InMemoryTransactionRepository = InMemoryTransactionRepository(),
    val categoryRepository: InMemoryCategoryRepository = InMemoryCategoryRepository(),
    val conceptRepository: InMemoryConceptRepository = InMemoryConceptRepository(),
    val uuids: UuidGenerator = SequentialUuidGenerator(),
    eventSubscribers: List<DomainEventSubscriber<out DomainEvent>> = emptyList(),
) {
    val eventBus: EventBus = InMemoryEventBus(eventSubscribers)

    private val categoryCreator = CategoryCreator(categoryRepository, uuids, eventBus)
    private val conceptCreator = ConceptCreator(conceptRepository, uuids, eventBus)
    private val createCategoryHandler = CreateCategoryCommandHandler(categoryCreator)
    private val registrar = TransactionRegistrar(txRepository, uuids, eventBus)
    private val registerHandler = RegisterTransactionCommandHandler(registrar)
    private val editHandler = EditTransactionCommandHandler(txRepository, uuids, eventBus)
    private val deleteHandler = DeleteTransactionCommandHandler(txRepository, uuids, eventBus)
    private val recordUsageHandler = RecordConceptUsageCommandHandler(conceptRepository, uuids, eventBus)

    private val findHandler = FindTransactionQueryHandler(txRepository)
    private val searchHandler = SearchTransactionsQueryHandler(txRepository)
    private val searchCategoriesHandler = SearchCategoriesQueryHandler(categoryRepository)
    private val listAllCategoriesHandler = ListAllCategoriesQueryHandler(categoryRepository)
    private val suggestConceptsHandler = SuggestConceptsQueryHandler(conceptRepository)
    private val listAllConceptsHandler = ListAllConceptsQueryHandler(conceptRepository)
    private val findConceptHandler = FindConceptQueryHandler(conceptRepository)

    private val commandHandlers: List<CommandHandler<out Command>> = listOf(
        createCategoryHandler,
        registerHandler,
        editHandler,
        deleteHandler,
        recordUsageHandler,
    )
    private val queryHandlers: List<QueryHandler<out Query, out Response>> = listOf(
        findHandler,
        searchHandler,
        searchCategoriesHandler,
        listAllCategoriesHandler,
        suggestConceptsHandler,
        listAllConceptsHandler,
        findConceptHandler,
    )

    val commandBus: CommandBus = InMemoryCommandBus(commandHandlers)
    val queryBus: QueryBus = InMemoryQueryBus(queryHandlers)

    private val fallbackCategory = FallbackCategoryResolver(categoryRepository, categoryCreator)
    val captureService = MovementCaptureService(conceptCreator, conceptRepository, fallbackCategory, commandBus, uuids)

    suspend fun seedCategory(name: String, kind: String = "EXPENSE"): String {
        val before = categoryRepository.countAll()
        commandBus.dispatch(
            CreateCategoryCommand(
                kind = kind,
                name = name,
                color = "#1976D2",
                icon = "label",
            )
        )
        return categoryRepository.all().drop(before.toInt()).first().id.value
    }

    fun start() {
        startKoin {
            modules(
                module {
                    single<TransactionRepository> { txRepository }
                    single<CategoryRepository> { categoryRepository }
                    single<ConceptRepository> { conceptRepository }
                    single { commandBus }
                    single { queryBus }
                    single { eventBus }
                    single { uuids }
                    single { captureService }
                }
            )
        }
    }

    fun stop() {
        stopKoin()
    }
}
