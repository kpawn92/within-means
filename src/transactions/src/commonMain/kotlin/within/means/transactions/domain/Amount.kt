package within.means.transactions.domain

import within.means.shared.domain.ValueObject

/**
 * Money amount of a transaction, stored as cents of the user's base
 * currency. The MVP is mono-currency so we don't carry the currency code
 * on every row — that lives on the `UserProfile`. Multidivisa is post-MVP.
 *
 * Structural invariant: cents must be strictly positive. The sign of a
 * transaction (income vs expense) is conveyed by `TransactionType`, not
 * by negative amounts — that keeps reports unambiguous.
 */
class Amount(val cents: Long) : ValueObject {

    init {
        require(cents > 0L) { "Amount must be strictly positive (got $cents)" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Amount) return false
        return cents == other.cents
    }

    override fun hashCode(): Int = cents.hashCode()

    override fun toString(): String = "Amount($cents)"
}
