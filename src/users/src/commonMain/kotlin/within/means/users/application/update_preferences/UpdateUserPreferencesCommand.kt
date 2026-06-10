package within.means.users.application.update_preferences

import within.means.shared.domain.bus.command.Command

data class UpdateUserPreferencesCommand(
    val userId: String,
    val displayName: String,
    val locale: String,
    val baseCurrency: String,
    val monthlyBudgetCents: Long,
    val spendingAlertsEnabled: Boolean,
) : Command
