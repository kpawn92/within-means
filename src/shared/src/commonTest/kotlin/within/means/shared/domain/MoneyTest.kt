package within.means.shared.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import within.means.shared.domain.money.Currency
import within.means.shared.domain.money.Money
import kotlin.test.Test

class MoneyTest {

    @Test
    fun `adds two amounts of the same currency`() {
        val sum = Money(1000, Currency.EUR) + Money(500, Currency.EUR)
        sum shouldBe Money(1500, Currency.EUR)
    }

    @Test
    fun `subtracts amounts of the same currency producing a negative result`() {
        val diff = Money(300, Currency.USD) - Money(800, Currency.USD)
        diff shouldBe Money(-500, Currency.USD)
        diff.isNegative shouldBe true
    }

    @Test
    fun `multiplies by an integer factor`() {
        (Money(250, Currency.CUP) * 4) shouldBe Money(1000, Currency.CUP)
    }

    @Test
    fun `unary minus produces the opposite amount`() {
        (-Money(700, Currency.EUR)) shouldBe Money(-700, Currency.EUR)
    }

    @Test
    fun `absolute value is always non-negative`() {
        Money(-1234, Currency.USD).abs() shouldBe Money(1234, Currency.USD)
        Money(99, Currency.USD).abs() shouldBe Money(99, Currency.USD)
    }

    @Test
    fun `comparison works within the same currency`() {
        (Money(100, Currency.EUR) < Money(200, Currency.EUR)) shouldBe true
        (Money(200, Currency.EUR) >= Money(200, Currency.EUR)) shouldBe true
    }

    @Test
    fun `operating across different currencies throws`() {
        shouldThrow<IllegalArgumentException> {
            Money(100, Currency.EUR) + Money(100, Currency.USD)
        }
        shouldThrow<IllegalArgumentException> {
            Money(100, Currency.EUR).compareTo(Money(100, Currency.USD))
        }
    }

    @Test
    fun `equality includes currency`() {
        (Money(100, Currency.EUR) == Money(100, Currency.USD)) shouldBe false
        (Money(100, Currency.EUR) == Money(100, Currency.EUR)) shouldBe true
    }

    @Test
    fun `zero is the additive identity`() {
        val zero = Money.zero(Currency.CUP)
        (zero + Money(900, Currency.CUP)) shouldBe Money(900, Currency.CUP)
        zero.isZero shouldBe true
    }

    @Test
    fun `currency lookup is case-insensitive`() {
        Currency.ofCode("eur") shouldBe Currency.EUR
        Currency.ofCode("CUP") shouldBe Currency.CUP
        Currency.ofCode("Usd") shouldBe Currency.USD
    }
}
