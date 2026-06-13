package within.means.android.capture

import within.means.categories.application.create.CategoryCreator
import within.means.categories.application.create.CreateCategoryCommand
import within.means.categories.domain.CategoryRepository

/**
 * Resolves the "Otros" category used when a movement has no concept-inferred
 * category and the user didn't pick one (CONCEPTS-SPEC §10-B). Find-or-create
 * so it works on fresh installs (no seed needed) and on existing ones alike;
 * idempotent because the lookup precedes creation.
 *
 * Lives in apps/android (composition root) because it spans the `categories`
 * context and the capture flow — neither KMP module learns about the other.
 */
class FallbackCategoryResolver(
    private val categories: CategoryRepository,
    private val creator: CategoryCreator,
) {
    /** [kind] is a category kind: EXPENSE | INCOME | TRANSFER. */
    suspend fun otrosId(kind: String): String {
        categories.all()
            .firstOrNull { it.kind.name == kind && it.name.value.equals(OTROS, ignoreCase = true) }
            ?.let { return it.id.value }

        return creator.create(
            CreateCategoryCommand(kind = kind, name = OTROS, color = FALLBACK_COLOR, icon = FALLBACK_ICON)
        ).value
    }

    companion object {
        const val OTROS = "Otros"
        private const val FALLBACK_COLOR = "#8C877E" // `muted` token from SPEC.md
        private const val FALLBACK_ICON = "more_horiz"
    }
}
