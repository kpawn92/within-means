package within.means.android.capture

import within.means.categories.domain.CategoryRepository
import within.means.concepts.application.create.ConceptCreator
import within.means.concepts.application.create.CreateConceptCommand
import within.means.concepts.domain.ConceptRepository

/**
 * Day-1 seed (CONCEPTS-SPEC §4.3): turn each default EXPENSE/INCOME category
 * into a concept of the same name that already infers that category. So the
 * QuickAdd chip row looks identical to before ("Mercado", "Transporte"…) but
 * those chips are now concepts — strictly more power, zero new friction.
 *
 * Idempotent: bails if any concept already exists. Lives in apps/android because
 * it spans `categories` and `concepts`; the modules never reference each other.
 * TRANSFER categories are skipped — concepts are expense/income only (§10-C).
 */
class ConceptsFromCategoriesSeeder(
    private val categories: CategoryRepository,
    private val concepts: ConceptRepository,
    private val creator: ConceptCreator,
) {
    suspend fun seedIfNeeded() {
        if (concepts.countAll() > 0L) return
        categories.all()
            .filter { it.kind.name == "EXPENSE" || it.kind.name == "INCOME" }
            .forEach { category ->
                creator.create(
                    CreateConceptCommand(
                        kind = category.kind.name,
                        label = category.name.value,
                        defaultCategoryId = category.id.value,
                    )
                )
            }
    }
}
