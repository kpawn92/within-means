package within.means.users.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import within.means.shared.domain.AggregateRoot
import within.means.shared.domain.UuidGenerator
import within.means.shared.domain.money.Currency

class UserProfile private constructor(
    val id: UserId,
    displayName: DisplayName,
    locale: Locale,
    baseCurrency: Currency,
    monthlyBudgetCents: Long,
    spendingAlertsEnabled: Boolean,
    val createdAt: Instant,
) : AggregateRoot() {

    var displayName: DisplayName = displayName
        private set

    var locale: Locale = locale
        private set

    var baseCurrency: Currency = baseCurrency
        private set

    /** Monthly spending plan in cents of [baseCurrency]; 0 means "no plan set". */
    var monthlyBudgetCents: Long = monthlyBudgetCents
        private set

    var spendingAlertsEnabled: Boolean = spendingAlertsEnabled
        private set

    fun updatePreferences(
        displayName: DisplayName,
        locale: Locale,
        baseCurrency: Currency,
        monthlyBudgetCents: Long,
        spendingAlertsEnabled: Boolean,
        uuids: UuidGenerator,
        clock: Clock = Clock.System,
    ) {
        require(monthlyBudgetCents >= 0L) { "monthlyBudgetCents cannot be negative" }
        this.displayName = displayName
        this.locale = locale
        this.baseCurrency = baseCurrency
        this.monthlyBudgetCents = monthlyBudgetCents
        this.spendingAlertsEnabled = spendingAlertsEnabled
        record(
            UserPreferencesUpdated(
                eventId = uuids.next(),
                aggregateId = id.value,
                occurredOn = clock.now(),
                displayName = displayName.value,
                locale = locale.code,
                baseCurrency = baseCurrency.code,
                monthlyBudgetCents = monthlyBudgetCents,
                spendingAlertsEnabled = spendingAlertsEnabled,
            )
        )
    }

    companion object {

        fun bootstrap(
            id: UserId,
            displayName: DisplayName = DisplayName(DEFAULT_NAME),
            locale: Locale = Locale.ES,
            baseCurrency: Currency = Currency.EUR,
            uuids: UuidGenerator,
            clock: Clock = Clock.System,
        ): UserProfile {
            val now = clock.now()
            return UserProfile(
                id = id,
                displayName = displayName,
                locale = locale,
                baseCurrency = baseCurrency,
                monthlyBudgetCents = 0L,
                spendingAlertsEnabled = true,
                createdAt = now,
            ).apply {
                record(
                    UserDefaultCreated(
                        eventId = uuids.next(),
                        aggregateId = id.value,
                        occurredOn = now,
                        displayName = displayName.value,
                        locale = locale.code,
                        baseCurrency = baseCurrency.code,
                    )
                )
            }
        }

        fun rehydrate(
            id: UserId,
            displayName: DisplayName,
            locale: Locale,
            baseCurrency: Currency,
            monthlyBudgetCents: Long,
            spendingAlertsEnabled: Boolean,
            createdAt: Instant,
        ): UserProfile = UserProfile(
            id = id,
            displayName = displayName,
            locale = locale,
            baseCurrency = baseCurrency,
            monthlyBudgetCents = monthlyBudgetCents,
            spendingAlertsEnabled = spendingAlertsEnabled,
            createdAt = createdAt,
        )

        private const val DEFAULT_NAME = "Yo"
    }
}
