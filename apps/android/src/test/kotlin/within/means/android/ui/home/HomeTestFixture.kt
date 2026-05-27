package within.means.android.ui.home

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import within.means.analytics.application.find_breakdown.FindCategoryBreakdownQueryHandler
import within.means.analytics.application.find_evolution.FindMonthlyEvolutionQueryHandler
import within.means.analytics.application.find_summary.FindCurrentMonthSummaryQueryHandler
import within.means.analytics.application.find_summary.FindMonthlySummaryQueryHandler
import within.means.android.ui.categories.SequentialUuidGenerator
import within.means.categories.application.create.CategoryCreator
import within.means.categories.application.create.CreateCategoryCommand
import within.means.categories.application.create.CreateCategoryCommandHandler
import within.means.categories.application.search.ListAllCategoriesQueryHandler
import within.means.categories.domain.CategoryRepository
import within.means.categories.infrastructure.persistence.InMemoryCategoryRepository
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.bus.command.Command
import within.means.shared.domain.bus.command.CommandBus
import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.bus.event.EventBus
import within.means.shared.domain.bus.query.Query
import within.means.shared.domain.bus.query.QueryBus
import within.means.shared.domain.bus.query.QueryHandler
import within.means.shared.domain.bus.query.Response
import within.means.shared.domain.money.Currency
import within.means.shared.infrastructure.bus.command.InMemoryCommandBus
import within.means.shared.infrastructure.bus.event.InMemoryEventBus
import within.means.shared.infrastructure.bus.query.InMemoryQueryBus
import within.means.transactions.application.register.RegisterTransactionCommand
import within.means.transactions.application.register.RegisterTransactionCommandHandler
import within.means.transactions.application.register.TransactionRegistrar
import within.means.transactions.application.search.SearchTransactionsQueryHandler
import within.means.transactions.domain.TransactionRepository
import within.means.transactions.infrastructure.persistence.InMemoryTransactionRepository
import within.means.users.application.find.FindDefaultUserQueryHandler
import within.means.users.application.update_preferences.UpdateUserPreferencesCommandHandler
import within.means.users.application.update_preferences.UserPreferencesUpdater
import within.means.users.domain.DisplayName
import within.means.users.domain.Locale
import within.means.users.domain.UserId
import within.means.users.domain.UserProfile
import within.means.users.domain.UserProfileRepository
import within.means.users.infrastructure.persistence.InMemoryUserProfileRepository

/**
 * In-memory wiring for the redesigned Home + Settings screens: user
 * profile + categories + transactions + analytics, all reachable via the
 * same buses the production app uses.
 */
class HomeTestFixture(
    val txRepository: InMemoryTransactionRepository = InMemoryTransactionRepository(),
    val categoryRepository: InMemoryCategoryRepository = InMemoryCategoryRepository(),
    val userRepository: InMemoryUserProfileRepository = InMemoryUserProfileRepository(),
    val uuids: UuidGenerator = SequentialUuidGenerator(),
    val clock: Clock = FixedClock(Instant.parse("2026-05-27T12:00:00Z")),
    val zone: TimeZone = TimeZone.UTC,
) {
    val eventBus: EventBus = InMemoryEventBus(emptyList())

    private val createCategoryHandler =
        CreateCategoryCommandHandler(CategoryCreator(categoryRepository, uuids, eventBus))
    private val registrar = TransactionRegistrar(txRepository, uuids, eventBus)
    private val registerHandler = RegisterTransactionCommandHandler(registrar)
    private val updatePrefsHandler = UpdateUserPreferencesCommandHandler(
        UserPreferencesUpdater(userRepository, uuids, eventBus)
    )

    private val findUserHandler = FindDefaultUserQueryHandler(userRepository)
    private val listAllCategoriesHandler = ListAllCategoriesQueryHandler(categoryRepository)
    private val findMonthlySummaryHandler =
        FindMonthlySummaryQueryHandler(txRepository, categoryRepository)
    private val findCurrentMonthSummaryHandler =
        FindCurrentMonthSummaryQueryHandler(txRepository, categoryRepository, clock, zone)
    private val findBreakdownHandler =
        FindCategoryBreakdownQueryHandler(txRepository, categoryRepository)
    private val findEvolutionHandler =
        FindMonthlyEvolutionQueryHandler(txRepository, clock, zone)
    private val searchTransactionsHandler = SearchTransactionsQueryHandler(txRepository)

    private val commandHandlers: List<CommandHandler<out Command>> = listOf(
        createCategoryHandler,
        registerHandler,
        updatePrefsHandler,
    )
    private val queryHandlers: List<QueryHandler<out Query, out Response>> = listOf(
        findUserHandler,
        listAllCategoriesHandler,
        findMonthlySummaryHandler,
        findCurrentMonthSummaryHandler,
        findBreakdownHandler,
        findEvolutionHandler,
        searchTransactionsHandler,
    )

    val commandBus: CommandBus = InMemoryCommandBus(commandHandlers)
    val queryBus: QueryBus = InMemoryQueryBus(queryHandlers)

    suspend fun seedUser(
        displayName: String = "Yo",
        locale: String = "ES",
        currency: String = "EUR",
    ): String {
        val id = UserId(uuids.next())
        userRepository.save(
            UserProfile.rehydrate(
                id = id,
                displayName = DisplayName(displayName),
                locale = Locale.valueOf(locale),
                baseCurrency = Currency.valueOf(currency),
                createdAt = clock.now(),
            )
        )
        return id.value
    }

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

    suspend fun seedTransaction(type: String, cents: Long, date: String, categoryId: String) {
        commandBus.dispatch(
            RegisterTransactionCommand(
                type = type,
                amountCents = cents,
                date = date,
                categoryId = categoryId,
            )
        )
    }

    fun start() {
        startKoin {
            modules(
                module {
                    single<TransactionRepository> { txRepository }
                    single<CategoryRepository> { categoryRepository }
                    single<UserProfileRepository> { userRepository }
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

class FixedClock(private val moment: Instant) : Clock {
    override fun now(): Instant = moment
}
