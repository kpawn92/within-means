package within.means.android.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
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
}
