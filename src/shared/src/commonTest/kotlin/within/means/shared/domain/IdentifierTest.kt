package within.means.shared.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class IdentifierTest {

    private class TestId(value: String) : Identifier(value)

    @Test
    fun `accepts a valid v4 UUID`() {
        val id = TestId("550e8400-e29b-41d4-a716-446655440000")
        id.value shouldBe "550e8400-e29b-41d4-a716-446655440000"
    }

    @Test
    fun `rejects an empty string`() {
        shouldThrow<IllegalArgumentException> { TestId("") }
    }

    @Test
    fun `rejects a non-uuid string`() {
        shouldThrow<IllegalArgumentException> { TestId("not-a-uuid") }
    }

    @Test
    fun `rejects a uuid missing a section`() {
        shouldThrow<IllegalArgumentException> { TestId("550e8400-e29b-41d4-a716") }
    }

    @Test
    fun `two ids with the same value are equal`() {
        val a = TestId("550e8400-e29b-41d4-a716-446655440000")
        val b = TestId("550e8400-e29b-41d4-a716-446655440000")
        (a == b) shouldBe true
        a.hashCode() shouldBe b.hashCode()
    }

    @Test
    fun `ids of different concrete types with same value are not equal`() {
        class OtherId(value: String) : Identifier(value)

        val a = TestId("550e8400-e29b-41d4-a716-446655440000")
        val b = OtherId("550e8400-e29b-41d4-a716-446655440000")
        (a == b) shouldBe false
    }
}
