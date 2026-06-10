package within.means.users.application.update_preferences

import within.means.shared.domain.bus.command.CommandHandler
import within.means.shared.domain.money.Currency
import within.means.users.domain.DisplayName
import within.means.users.domain.Locale
import within.means.users.domain.UserId
import kotlin.reflect.KClass

class UpdateUserPreferencesCommandHandler(
    private val updater: UserPreferencesUpdater,
) : CommandHandler<UpdateUserPreferencesCommand> {

    override val commandType: KClass<UpdateUserPreferencesCommand> = UpdateUserPreferencesCommand::class

    override suspend fun handle(command: UpdateUserPreferencesCommand) {
        updater.update(
            userId = UserId(command.userId),
            displayName = DisplayName(command.displayName),
            locale = Locale.ofCode(command.locale),
            baseCurrency = Currency.ofCode(command.baseCurrency),
            monthlyBudgetCents = command.monthlyBudgetCents,
            spendingAlertsEnabled = command.spendingAlertsEnabled,
        )
    }
}
