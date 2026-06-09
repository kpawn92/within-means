package within.means.users.infrastructure.persistence

import kotlinx.datetime.Instant
import within.means.shared.domain.money.Currency
import within.means.users.db.User_profile
import within.means.users.domain.DisplayName
import within.means.users.domain.Locale
import within.means.users.domain.UserId
import within.means.users.domain.UserProfile

internal fun User_profile.toAggregate(): UserProfile = UserProfile.rehydrate(
    id = UserId(id),
    displayName = DisplayName(display_name),
    locale = Locale.ofCode(locale),
    baseCurrency = Currency.ofCode(base_currency),
    monthlyBudgetCents = monthly_budget_cents,
    spendingAlertsEnabled = spending_alerts_enabled == 1L,
    createdAt = Instant.parse(created_at),
)

internal data class UserProfileRow(
    val id: String,
    val displayName: String,
    val locale: String,
    val baseCurrency: String,
    val monthlyBudgetCents: Long,
    val spendingAlertsEnabled: Long,
    val createdAt: String,
    val isDefault: Long,
)

internal fun UserProfile.toRow(isDefault: Boolean): UserProfileRow = UserProfileRow(
    id = id.value,
    displayName = displayName.value,
    locale = locale.code,
    baseCurrency = baseCurrency.code,
    monthlyBudgetCents = monthlyBudgetCents,
    spendingAlertsEnabled = if (spendingAlertsEnabled) 1L else 0L,
    createdAt = createdAt.toString(),
    isDefault = if (isDefault) 1L else 0L,
)
