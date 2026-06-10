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
    monthStartDay: Int,
    hideAmounts: Boolean,
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

    /**
     * Day of month the budget cycle resets on (1..28; capped at 28 so every
     * month has the day). The "month" used for pace/available spans from this
     * day to the day before it next month.
     */
    var monthStartDay: Int = monthStartDay
        private set

    /** When true, the UI masks amounts until the user reveals them. */
    var hideAmounts: Boolean = hideAmounts
        private set

    fun updatePreferences(
        displayName: DisplayName,
        locale: Locale,
        baseCurrency: Currency,
        monthlyBudgetCents: Long,
        spendingAlertsEnabled: Boolean,
        monthStartDay: Int,
        hideAmounts: Boolean,
        uuids: UuidGenerator,
        clock: Clock = Clock.System,
    ) {
        require(monthlyBudgetCents >= 0L) { "monthlyBudgetCents cannot be negative" }
        require(monthStartDay in MONTH_START_RANGE) { "monthStartDay must be in $MONTH_START_RANGE" }
        this.displayName = displayName
        this.locale = locale
        this.baseCurrency = baseCurrency
        this.monthlyBudgetCents = monthlyBudgetCents
        this.spendingAlertsEnabled = spendingAlertsEnabled
        this.monthStartDay = monthStartDay
        this.hideAmounts = hideAmounts
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
                monthStartDay = monthStartDay,
                hideAmounts = hideAmounts,
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
                monthStartDay = DEFAULT_MONTH_START_DAY,
                hideAmounts = false,
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
            monthStartDay: Int,
            hideAmounts: Boolean,
            createdAt: Instant,
        ): UserProfile = UserProfile(
            id = id,
            displayName = displayName,
            locale = locale,
            baseCurrency = baseCurrency,
            monthlyBudgetCents = monthlyBudgetCents,
            spendingAlertsEnabled = spendingAlertsEnabled,
            monthStartDay = monthStartDay,
            hideAmounts = hideAmounts,
            createdAt = createdAt,
        )

        private const val DEFAULT_NAME = "Yo"

        /** Budget cycle reset day; 1 = calendar month. */
        const val DEFAULT_MONTH_START_DAY = 1

        /** Capped at 28 so the chosen day exists in every month. */
        val MONTH_START_RANGE = 1..28
    }
}
