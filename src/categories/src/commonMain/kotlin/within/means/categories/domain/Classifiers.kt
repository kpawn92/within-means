package within.means.categories.domain

enum class CategoryKind { EXPENSE, INCOME, TRANSFER }

enum class CategoryNature { FIXED, VARIABLE }

enum class CategoryEssentiality { ESSENTIAL, DISCRETIONARY }

enum class EngelGroup {
    FOOD,
    HOUSING,
    TRANSPORT,
    HEALTH,
    EDUCATION,
    LEISURE,
    OTHER,
}

/**
 * Bundle of the semantic classifiers attached to a category.
 *
 * `nature` / `essentiality` apply only to EXPENSE.
 * `productive` typically applies to EXPENSE (does the spend generate
 * future value? e.g. education, health, work tools).
 * `engelGroup` applies to EXPENSE only.
 */
data class CategoryClassifiers(
    val nature: CategoryNature?,
    val essentiality: CategoryEssentiality?,
    val productive: Boolean,
    val engelGroup: EngelGroup?,
) {
    companion object {
        val EMPTY = CategoryClassifiers(
            nature = null,
            essentiality = null,
            productive = false,
            engelGroup = null,
        )
    }
}
