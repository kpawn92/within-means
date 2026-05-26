package within.means.shared.infrastructure

import io.kotest.matchers.shouldBe
import within.means.shared.domain.Identifier
import kotlin.test.Test

class RealUuidGeneratorTest {

    private class AnyId(value: String) : Identifier(value)

    @Test
    fun `produces a UUID accepted by Identifier`() {
        val gen = RealUuidGenerator()
        val id = AnyId(gen.next())
        id.value.length shouldBe 36
    }

    @Test
    fun `produces different values across calls`() {
        val gen = RealUuidGenerator()
        val a = gen.next()
        val b = gen.next()
        (a == b) shouldBe false
    }
}
