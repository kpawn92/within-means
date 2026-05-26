package within.means.android.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import within.means.android.persistence.AndroidDatabaseFactory
import within.means.android.persistence.DatabaseUnlocker
import within.means.android.persistence.KeystoreManager
import within.means.android.persistence.OnboardingState
import within.means.android.persistence.PassphraseProvider

val appModule = module {
    single<CoroutineDispatcher>(qualifier = org.koin.core.qualifier.named("io")) { Dispatchers.IO }
    single { KeystoreManager() }
    single { PassphraseProvider(get()) }
    single { AndroidDatabaseFactory(androidContext()) }
    single { DatabaseUnlocker(get(), get()) }
    single { OnboardingState(androidContext()) }
}
