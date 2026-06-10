package within.means.android.ui.calculator

import io.kotest.matchers.shouldBe
import org.junit.Test

class AmountCalculatorTest {

    private fun calc(vararg keys: Char): AmountCalculator {
        val c = AmountCalculator()
        keys.forEach { k ->
            when (k) {
                in '0'..'9' -> c.onDigit(k)
                '.' -> c.onDot()
                in "+-*/" -> c.onOperator(k)
            }
        }
        return c
    }

    @Test
    fun `a single number evaluates to its cents`() {
        val c = calc('1', '2', '.', '5', '0')
        c.resultCents() shouldBe 1250L
        c.resultText() shouldBe "12.50"
    }

    @Test
    fun `addition and subtraction`() {
        calc('1', '0', '+', '5', '-', '2').resultCents() shouldBe 1300L
    }

    @Test
    fun `multiplication takes precedence over addition`() {
        calc('2', '+', '3', '*', '4').resultCents() shouldBe 1400L // 2 + 12
    }

    @Test
    fun `division uses precise rounding to two decimals`() {
        calc('1', '0', '/', '3').resultCents() shouldBe 333L // 3.3333… → 3.33
    }

    @Test
    fun `decimal arithmetic avoids float drift`() {
        calc('0', '.', '1', '+', '0', '.', '2').resultCents() shouldBe 30L // 0.30, not 0.30000004
    }

    @Test
    fun `a trailing operator is ignored while typing`() {
        calc('1', '5', '+').resultCents() shouldBe 1500L
    }

    @Test
    fun `tapping a second operator replaces the first`() {
        val c = calc('1', '0', '+')
        c.onOperator('*')
        c.onDigit('2')
        c.resultCents() shouldBe 2000L // 10 * 2, the '+' was replaced
        c.expression shouldBe "10*2"
    }

    @Test
    fun `a leading operator is ignored`() {
        val c = AmountCalculator()
        c.onOperator('+')
        c.expression shouldBe ""
    }

    @Test
    fun `decimals are capped at two places per operand`() {
        val c = calc('1', '.', '2', '3', '4')
        c.expression shouldBe "1.23"
    }

    @Test
    fun `integer part is capped at nine digits`() {
        val c = AmountCalculator()
        repeat(12) { c.onDigit('9') }
        c.expression shouldBe "999999999"
    }

    @Test
    fun `a leading zero is replaced by the next digit`() {
        val c = calc('0', '5')
        c.expression shouldBe "5"
    }

    @Test
    fun `a dot with no integer part becomes zero-dot`() {
        val c = AmountCalculator()
        c.onDot()
        c.expression shouldBe "0."
        c.onDigit('5')
        c.resultCents() shouldBe 50L
    }

    @Test
    fun `division by zero yields no result`() {
        calc('5', '/', '0').resultCents() shouldBe null
    }

    @Test
    fun `an empty expression has no result`() {
        AmountCalculator().resultCents() shouldBe null
    }

    @Test
    fun `evaluateInPlace collapses the expression to its value`() {
        val c = calc('2', '+', '3')
        c.evaluateInPlace()
        c.expression shouldBe "5.00"
        // and it can keep operating from there
        c.onOperator('*')
        c.onDigit('2')
        c.resultCents() shouldBe 1000L
    }

    @Test
    fun `seeded amount is the starting expression`() {
        val c = AmountCalculator("12.50")
        c.resultCents() shouldBe 1250L
    }

    @Test
    fun `display maps operators to math glyphs`() {
        calc('2', '*', '3', '/', '4', '-', '1').displayExpression() shouldBe "2×3÷4−1"
    }
}
