package within.means.android.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import within.means.android.capture.ConceptCategorySuggester
import within.means.android.capture.ConceptsFromCategoriesSeeder
import within.means.android.capture.FallbackCategoryResolver
import within.means.android.capture.MovementCaptureService
import within.means.android.subscribers.RecordConceptUsageOnTransactionRegistered
import within.means.android.subscribers.SeedConceptsOnUserDefaultCreated
import within.means.concepts.application.create.ConceptCreator
import within.means.concepts.application.create.CreateConceptCommandHandler
import within.means.concepts.application.delete.DeleteConceptCommandHandler
import within.means.concepts.application.find.FindConceptQueryHandler
import within.means.concepts.application.recategorize.SetConceptDefaultCategoryCommandHandler
import within.means.concepts.application.record_usage.RecordConceptUsageCommandHandler
import within.means.concepts.application.rename.RenameConceptCommandHandler
import within.means.concepts.application.suggest.ListAllConceptsQueryHandler
import within.means.concepts.application.suggest.SuggestConceptsQueryHandler

val conceptsModule = module {

    singleOf(::ConceptCreator)

    singleOf(::CreateConceptCommandHandler)
    singleOf(::RecordConceptUsageCommandHandler)
    singleOf(::SetConceptDefaultCategoryCommandHandler)
    singleOf(::RenameConceptCommandHandler)
    singleOf(::DeleteConceptCommandHandler)

    singleOf(::FindConceptQueryHandler)
    singleOf(::SuggestConceptsQueryHandler)
    singleOf(::ListAllConceptsQueryHandler)

    // Capture orchestration (F3) — spans concepts + categories + transactions.
    singleOf(::FallbackCategoryResolver)
    singleOf(::RecordConceptUsageOnTransactionRegistered)

    // Learning (F6): category suggestion + day-1 concepts from categories.
    singleOf(::ConceptCategorySuggester)
    singleOf(::ConceptsFromCategoriesSeeder)
    singleOf(::SeedConceptsOnUserDefaultCreated)

    // 7 ctor params exceed singleOf's arity, so wire it explicitly.
    single {
        MovementCaptureService(get(), get(), get(), get(), get(), get(), get())
    }
}
