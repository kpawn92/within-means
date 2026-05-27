package within.means.android.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import within.means.analytics.application.find_breakdown.FindCategoryBreakdownQueryHandler
import within.means.analytics.application.find_evolution.FindMonthlyEvolutionQueryHandler
import within.means.analytics.application.find_summary.FindCurrentMonthSummaryQueryHandler
import within.means.analytics.application.find_summary.FindMonthlySummaryQueryHandler

val analyticsModule = module {
    singleOf(::FindMonthlySummaryQueryHandler)
    singleOf(::FindCurrentMonthSummaryQueryHandler)
    singleOf(::FindCategoryBreakdownQueryHandler)
    singleOf(::FindMonthlyEvolutionQueryHandler)
}
