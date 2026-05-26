package within.means.users.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DisplayNameTest {

    @Test
    fun `trims whitespace`() {
        DisplayName("  Alice  ").value shouldBe "Alice"
    }

    @Test
    fun `rejects blank input`() {
        shouldThrow<IllegalArgumentException> { DisplayName("   ") }
        shouldThrow<IllegalArgumentException> { DisplayName("") }
    }

    @Test
    fun `rejects values longer than the max length`() {
        shouldThrow<IllegalArgumentException> { DisplayName("x".repeat(DisplayName.MAX_LENGTH + 1)) }
    }
}
