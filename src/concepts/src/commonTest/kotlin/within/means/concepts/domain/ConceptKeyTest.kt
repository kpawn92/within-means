package within.means.concepts.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ConceptKeyTest {

    @Test
    fun `case and surrounding space collapse to the same key`() {
        ConceptKey.of("Cerveza") shouldBe ConceptKey.of("  cerveza ")
        ConceptKey.of("CERVEZA") shouldBe ConceptKey.of("cerveza")
    }

    @Test
    fun `accents are stripped but the enie is kept`() {
        ConceptKey.of("Papá").value shouldBe "papa"
        ConceptKey.of("café").value shouldBe "cafe"
        // ñ is preserved: año must NOT collapse into ano
        ConceptKey.of("Año").value shouldBe "año"
        ConceptKey.of("año") shouldNotBeEqualTo ConceptKey.of("ano")
    }

    @Test
    fun `emoji and punctuation are dropped`() {
        ConceptKey.of("Cerveza 🍺").value shouldBe "cerveza"
        ConceptKey.of("cerveza!!!").value shouldBe "cerveza"
    }

    @Test
    fun `inner whitespace collapses to a single space`() {
        ConceptKey.of("Bus   casa   trabajo").value shouldBe "bus casa trabajo"
    }

    @Test
    fun `route arrows reduce to the surrounding words`() {
        // "carro ruta1 → ruta2" — the arrow is a symbol, dropped; words kept.
        ConceptKey.of("carro ruta1 → ruta2").value shouldBe "carro ruta1 ruta2"
    }

    @Test
    fun `a label that normalizes to nothing is rejected`() {
        shouldThrow<IllegalArgumentException> { ConceptKey.of("🍺🍺") }
    }

    private infix fun ConceptKey.shouldNotBeEqualTo(other: ConceptKey) {
        (this == other) shouldBe false
    }
}
