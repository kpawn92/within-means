package within.means.android.di

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import within.means.android.ui.onboarding.OnboardingViewModel
import within.means.android.ui.unlock.UnlockViewModel

val uiModule = module {
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::UnlockViewModel)
}
