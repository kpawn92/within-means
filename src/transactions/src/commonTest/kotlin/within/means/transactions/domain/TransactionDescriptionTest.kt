package within.means.transactions.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TransactionDescriptionTest {

    @Test
    fun trims_surrounding_whitespace() {
        TransactionDescription("   compra   ").value shouldBe "compra"
    }

    @Test
    fun accepts_empty_string() {
        TransactionDescription("").value shouldBe ""
        TransactionDescription.EMPTY.value shouldBe ""
    }

    @Test
    fun rejects_values_longer_than_max_length() {
        val tooLong = "x".repeat(TransactionDescription.MAX_LENGTH + 1)
        shouldThrow<IllegalArgumentException> { TransactionDescription(tooLong) }
    }
}
