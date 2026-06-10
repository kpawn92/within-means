package within.means.android.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import within.means.android.ui.analytics.StatsViewModel
import within.means.android.ui.categories.CategoriesListViewModel
import within.means.android.ui.categories.CategoryEditViewModel
import within.means.android.ui.home.HomeViewModel
import within.means.android.ui.onboarding.OnboardingViewModel
import within.means.android.ui.quickadd.QuickAddViewModel
import within.means.android.ui.recurring.RecurringRulesViewModel
import within.means.android.ui.settings.SettingsViewModel
import within.means.android.ui.transactions.TransactionEditViewModel
import within.means.android.ui.transactions.TransactionsListViewModel
import within.means.android.ui.unlock.UnlockViewModel

val uiModule = module {
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::UnlockViewModel)
    // HomeViewModel takes optional Clock+TimeZone with defaults (budget pace).
    // Explicit factory lets Kotlin defaults apply, like StatsViewModel below.
    viewModel { HomeViewModel(get()) }
    viewModelOf(::CategoriesListViewModel)
    viewModelOf(::CategoryEditViewModel)
    viewModelOf(::TransactionsListViewModel)
    viewModelOf(::TransactionEditViewModel)
    // StatsViewModel takes optional Clock+TimeZone with defaults; viewModelOf
    // would try to resolve them via DI. Explicit factory lets Kotlin defaults
    // apply (system clock + current zone). Mirrors analyticsModule.
    viewModel { StatsViewModel(get()) }
    // QuickAddViewModel takes optional Clock+TimeZone with defaults.
    viewModel { QuickAddViewModel() }
    viewModelOf(::SettingsViewModel)
    viewModelOf(::RecurringRulesViewModel)
}
