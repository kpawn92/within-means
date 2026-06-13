package within.means.transactions.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ConceptRefsTest {

    @Test
    fun `empty is allowed`() {
        ConceptRefs.EMPTY.isEmpty shouldBe true
        ConceptRefs(emptyList()).ids shouldBe emptyList()
    }

    @Test
    fun `keeps order — first ref drives the inferred category`() {
        ConceptRefs.of("papa", "transporte").ids shouldBe listOf("papa", "transporte")
    }

    @Test
    fun `rejects duplicates`() {
        shouldThrow<IllegalArgumentException> { ConceptRefs.of("a", "a") }
    }

    @Test
    fun `rejects blank ids`() {
        shouldThrow<IllegalArgumentException> { ConceptRefs.of("a", " ") }
    }

    @Test
    fun `rejects more than the maximum`() {
        val tooMany = (1..ConceptRefs.MAX + 1).map { "c$it" }
        shouldThrow<IllegalArgumentException> { ConceptRefs(tooMany) }
    }

    @Test
    fun `equality is by id list`() {
        ConceptRefs.of("a", "b") shouldBe ConceptRefs.of("a", "b")
        (ConceptRefs.of("a", "b") == ConceptRefs.of("b", "a")) shouldBe false
    }
}
