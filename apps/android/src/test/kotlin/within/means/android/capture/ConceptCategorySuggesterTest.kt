package within.means.android.capture

import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import org.junit.Test
import within.means.categories.domain.Category
import within.means.categories.domain.CategoryClassifiers
import within.means.categories.domain.CategoryColor
import within.means.categories.domain.CategoryIcon
import within.means.categories.domain.CategoryId
import within.means.categories.domain.CategoryKind
import within.means.categories.domain.CategoryName
import within.means.categories.domain.EngelGroup

class ConceptCategorySuggesterTest {

    private val suggester = ConceptCategorySuggester()
    private var counter = 0

    private fun expense(name: String, engel: EngelGroup?): Category {
        counter++
        return Category.rehydrate(
            id = CategoryId("00000000-0000-4000-8000-${counter.toString().padStart(12, '0')}"),
            kind = CategoryKind.EXPENSE,
            name = CategoryName(name),
            color = CategoryColor("#1976D2"),
            icon = CategoryIcon("label"),
            classifiers = CategoryClassifiers(null, null, false, engel),
            parentId = null,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
    }

    @Test
    fun `synonym maps a new concept to the category in that Engel group`() {
        val comida = expense("Comida", EngelGroup.FOOD)
        val transporte = expense("Transporte", EngelGroup.TRANSPORT)

        suggester.suggest("gasolina", listOf(comida, transporte)) shouldBe transporte.id.value
        suggester.suggest("Cerveza", listOf(comida, transporte)) shouldBe comida.id.value
    }

    @Test
    fun `name match wins over the synonym group`() {
        // "bus" is a TRANSPORT synonym, but a category literally named "Bus" exists.
        val bus = expense("Bus", null)
        val transporte = expense("Transporte", EngelGroup.TRANSPORT)

        suggester.suggest("bus", listOf(transporte, bus)) shouldBe bus.id.value
    }

    @Test
    fun `returns null when nothing matches`() {
        val comida = expense("Comida", EngelGroup.FOOD)
        suggester.suggest("zzqq", listOf(comida)) shouldBe null
    }

    @Test
    fun `accents and emoji do not break matching`() {
        val transporte = expense("Transporte", EngelGroup.TRANSPORT)
        suggester.suggest("Gasolina ⛽", listOf(transporte)) shouldBe transporte.id.value
    }
}
