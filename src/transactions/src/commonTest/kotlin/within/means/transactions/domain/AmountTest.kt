package within.means.transactions.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AmountTest {

    @Test
    fun accepts_strictly_positive_cents() {
        Amount(1L).cents shouldBe 1L
        Amount(123_45L).cents shouldBe 12345L
    }

    @Test
    fun rejects_zero() {
        shouldThrow<IllegalArgumentException> { Amount(0L) }
    }

    @Test
    fun rejects_negative() {
        shouldThrow<IllegalArgumentException> { Amount(-1L) }
    }

    @Test
    fun equals_by_cents() {
        (Amount(500L) == Amount(500L)) shouldBe true
        (Amount(500L) == Amount(501L)) shouldBe false
    }
}
