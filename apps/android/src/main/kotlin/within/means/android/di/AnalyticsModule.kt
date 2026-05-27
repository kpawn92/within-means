package within.means.android.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import within.means.analytics.application.find_breakdown.FindCategoryBreakdownQueryHandler
import within.means.analytics.application.find_evolution.FindMonthlyEvolutionQueryHandler
import within.means.analytics.application.find_summary.FindCurrentMonthSummaryQueryHandler
import within.means.analytics.application.find_summary.FindMonthlySummaryQueryHandler

val analyticsModule = module {
    singleOf(::FindMonthlySummaryQueryHandler)
    singleOf(::FindCategoryBreakdownQueryHandler)

    // Handlers below take optional Clock+TimeZone with sane defaults; `singleOf`
    // would try to resolve them through DI and fail. Explicit factories let
    // Kotlin defaults apply (system clock + current zone).
    single { FindCurrentMonthSummaryQueryHandler(get(), get()) }
    single { FindMonthlyEvolutionQueryHandler(get()) }
}
