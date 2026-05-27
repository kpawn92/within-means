package within.means.android.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import within.means.android.subscribers.SeedDefaultCategoriesOnUserDefaultCreated
import within.means.categories.application.create.CategoryCreator
import within.means.categories.application.create.CreateCategoryCommandHandler
import within.means.categories.application.delete.DeleteCategoryCommandHandler
import within.means.categories.application.find.FindCategoryQueryHandler
import within.means.categories.application.reclassify.ReclassifyCategoryCommandHandler
import within.means.categories.application.recolor.RestyleCategoryCommandHandler
import within.means.categories.application.rename.RenameCategoryCommandHandler
import within.means.categories.application.search.ListAllCategoriesQueryHandler
import within.means.categories.application.search.SearchCategoriesQueryHandler
import within.means.categories.application.seed.DefaultCategoriesSeeder

val categoriesModule = module {

    singleOf(::CategoryCreator)
    singleOf(::DefaultCategoriesSeeder)

    singleOf(::CreateCategoryCommandHandler)
    singleOf(::RenameCategoryCommandHandler)
    singleOf(::RestyleCategoryCommandHandler)
    singleOf(::ReclassifyCategoryCommandHandler)
    singleOf(::DeleteCategoryCommandHandler)

    singleOf(::FindCategoryQueryHandler)
    singleOf(::SearchCategoriesQueryHandler)
    singleOf(::ListAllCategoriesQueryHandler)

    singleOf(::SeedDefaultCategoriesOnUserDefaultCreated)
}
