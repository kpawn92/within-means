package within.means.shared.domain.money

import within.means.shared.domain.ValueObject

class Money(val cents: Long, val currency: Currency) : ValueObject, Comparable<Money> {

    val isZero: Boolean get() = cents == 0L
    val isPositive: Boolean get() = cents > 0L
    val isNegative: Boolean get() = cents < 0L

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(cents + other.cents, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(cents - other.cents, currency)
    }

    operator fun times(factor: Int): Money = Money(cents * factor, currency)

    operator fun times(factor: Long): Money = Money(cents * factor, currency)

    operator fun unaryMinus(): Money = Money(-cents, currency)

    fun abs(): Money = if (isNegative) -this else this

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return cents.compareTo(other.cents)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Cannot operate on amounts in different currencies: $currency vs ${other.currency}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Money) return false
        return cents == other.cents && currency == other.currency
    }

    override fun hashCode(): Int = 31 * cents.hashCode() + currency.hashCode()

    override fun toString(): String = "Money($cents ${currency.code})"

    companion object {
        fun zero(currency: Currency): Money = Money(0L, currency)
    }
}
