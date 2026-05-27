package within.means.android

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import within.means.android.di.analyticsModule
import within.means.android.di.appModule
import within.means.android.di.busModule
import within.means.android.di.categoriesModule
import within.means.android.di.persistenceModule
import within.means.android.di.transactionsModule
import within.means.android.di.uiModule
import within.means.android.di.usersModule

class WithinMeansApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@WithinMeansApplication)
            modules(
                appModule,
                persistenceModule,
                usersModule,
                categoriesModule,
                transactionsModule,
                analyticsModule,
                busModule,
                uiModule,
            )
        }
    }
}
