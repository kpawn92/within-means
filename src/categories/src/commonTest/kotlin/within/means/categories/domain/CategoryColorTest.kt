package within.means.categories.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CategoryColorTest {

    @Test
    fun `normalizes to uppercase hex`() {
        CategoryColor("#abcdef").value shouldBe "#ABCDEF"
        CategoryColor("#A1B2C3").value shouldBe "#A1B2C3"
    }

    @Test
    fun `rejects invalid hex`() {
        shouldThrow<IllegalArgumentException> { CategoryColor("ABCDEF") }
        shouldThrow<IllegalArgumentException> { CategoryColor("#XYZ123") }
        shouldThrow<IllegalArgumentException> { CategoryColor("#FFF") }
    }
}
