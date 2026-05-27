package within.means.transactions.domain

import within.means.shared.domain.StringValueObject

/**
 * Free-form note on a transaction. May be empty (a registered amount with
 * a known category often needs no extra context) but is capped to keep
 * lists rendering predictably.
 */
class TransactionDescription(value: String) : StringValueObject(value.trim()) {
    init {
        require(this.value.length <= MAX_LENGTH) {
            "TransactionDescription cannot exceed $MAX_LENGTH characters"
        }
    }

    companion object {
        const val MAX_LENGTH = 140
        val EMPTY = TransactionDescription("")
    }
}
