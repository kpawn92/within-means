package within.means.android.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import within.means.android.persistence.AndroidDatabaseFactory
import within.means.android.persistence.BiometricAvailability
import within.means.android.persistence.BiometricVault
import within.means.android.persistence.DatabaseUnlocker
import within.means.android.persistence.KeystoreManager
import within.means.android.persistence.OnboardingState
import within.means.android.persistence.PassphraseProvider
import within.means.android.persistence.ThemePreference

val appModule = module {
    single<CoroutineDispatcher>(qualifier = org.koin.core.qualifier.named("io")) { Dispatchers.IO }
    single { KeystoreManager() }
    single { PassphraseProvider(get()) }
    single { AndroidDatabaseFactory(androidContext()) }
    single { DatabaseUnlocker(get(), get()) }
    single { OnboardingState(androidContext()) }
    single { ThemePreference(androidContext()) }
    // Biometric unlock (BiometricGate is built per-screen — it needs the activity).
    single { BiometricAvailability(androidContext()) }
    single { BiometricVault(androidContext()) }
}
