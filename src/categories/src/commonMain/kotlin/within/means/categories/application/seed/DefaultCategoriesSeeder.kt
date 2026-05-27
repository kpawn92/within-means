package within.means.categories.application.seed

import within.means.categories.application.create.CategoryCreator
import within.means.categories.application.create.CreateCategoryCommand
import within.means.categories.domain.CategoryRepository

/**
 * Seeds the default set of ~13 categories the first time it is run.
 * Idempotent: bails out if the repository already contains any category.
 *
 * It's invoked by an event subscriber wired in apps/<platform>: this
 * module stays unaware of which event triggers it.
 */
class DefaultCategoriesSeeder(
    private val repository: CategoryRepository,
    private val creator: CategoryCreator,
) {

    suspend fun seedIfNeeded() {
        if (repository.countAll() > 0L) return
        DEFAULTS.forEach { creator.create(it) }
    }

    companion object {
        // EXPENSE categories with full classifiers
        private fun expense(
            name: String,
            color: String,
            icon: String,
            nature: String,
            essentiality: String,
            productive: Boolean = false,
            engelGroup: String,
        ) = CreateCategoryCommand(
            kind = "EXPENSE",
            name = name,
            color = color,
            icon = icon,
            nature = nature,
            essentiality = essentiality,
            productive = productive,
            engelGroup = engelGroup,
        )

        private fun income(name: String, color: String, icon: String) = CreateCategoryCommand(
            kind = "INCOME",
            name = name,
            color = color,
            icon = icon,
        )

        private fun transfer(name: String, color: String, icon: String) = CreateCategoryCommand(
            kind = "TRANSFER",
            name = name,
            color = color,
            icon = icon,
        )

        val DEFAULTS: List<CreateCategoryCommand> = listOf(
            // Income
            income("Nómina", "#2E7D32", "work"),
            income("Freelance", "#388E3C", "computer"),
            // Expenses
            expense("Alquiler", "#1976D2", "home", "FIXED", "ESSENTIAL", false, "HOUSING"),
            expense("Hipoteca", "#1565C0", "house", "FIXED", "ESSENTIAL", false, "HOUSING"),
            expense("Supermercado", "#F9A825", "shopping_cart", "VARIABLE", "ESSENTIAL", false, "FOOD"),
            expense("Restaurantes", "#F57C00", "restaurant", "VARIABLE", "DISCRETIONARY", false, "FOOD"),
            expense("Transporte público", "#0288D1", "directions_bus", "FIXED", "ESSENTIAL", false, "TRANSPORT"),
            expense("Combustible", "#0277BD", "local_gas_station", "VARIABLE", "ESSENTIAL", false, "TRANSPORT"),
            expense("Salud", "#C2185B", "favorite", "VARIABLE", "ESSENTIAL", true, "HEALTH"),
            expense("Educación", "#7B1FA2", "school", "VARIABLE", "DISCRETIONARY", true, "EDUCATION"),
            expense("Suscripciones", "#5E35B1", "subscriptions", "FIXED", "DISCRETIONARY", false, "LEISURE"),
            expense("Ocio", "#D81B60", "celebration", "VARIABLE", "DISCRETIONARY", false, "LEISURE"),
            // Transfer
            transfer("Transferencia", "#616161", "swap_horiz"),
        )
    }
}
