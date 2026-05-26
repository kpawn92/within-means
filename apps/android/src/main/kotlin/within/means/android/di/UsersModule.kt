package within.means.android.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import within.means.users.application.ensure_default.DefaultUserBootstrap
import within.means.users.application.ensure_default.EnsureDefaultUserCommandHandler
import within.means.users.application.find.FindDefaultUserQueryHandler
import within.means.users.application.update_preferences.UpdateUserPreferencesCommandHandler
import within.means.users.application.update_preferences.UserPreferencesUpdater

/**
 * Application services and handlers of the users bounded context.
 * The handlers are aggregated into the buses by [busModule] via an
 * explicit list — no reflection, no magic.
 */
val usersModule = module {
    singleOf(::DefaultUserBootstrap)
    singleOf(::UserPreferencesUpdater)

    singleOf(::EnsureDefaultUserCommandHandler)
    singleOf(::UpdateUserPreferencesCommandHandler)
    singleOf(::FindDefaultUserQueryHandler)
}
