package within.means.android.di

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import within.means.android.ui.analytics.StatsViewModel
import within.means.android.ui.categories.CategoriesListViewModel
import within.means.android.ui.categories.CategoryEditViewModel
import within.means.android.ui.onboarding.OnboardingViewModel
import within.means.android.ui.transactions.TransactionEditViewModel
import within.means.android.ui.transactions.TransactionsListViewModel
import within.means.android.ui.unlock.UnlockViewModel

val uiModule = module {
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::UnlockViewModel)
    viewModelOf(::CategoriesListViewModel)
    viewModelOf(::CategoryEditViewModel)
    viewModelOf(::TransactionsListViewModel)
    viewModelOf(::TransactionEditViewModel)
    viewModelOf(::StatsViewModel)
}
