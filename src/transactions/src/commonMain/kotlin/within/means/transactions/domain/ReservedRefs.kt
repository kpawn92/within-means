package within.means.transactions.domain

import within.means.shared.domain.StringValueObject

/**
 * Free-form provenance hint when a transaction was created from somewhere
 * other than the manual form (an import, a recurring rule, an external
 * receipt). Always null in the MVP — the field exists so future flows can
 * back-fill it without a schema migration.
 */
class OriginRef(value: String) : StringValueObject(value.trim()) {
    init {
        require(this.value.isNotEmpty()) { "OriginRef cannot be blank if present" }
        require(this.value.length <= MAX_LENGTH) { "OriginRef cannot exceed $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 64
    }
}

/**
 * Pointer to the recurring-rule instance that produced this transaction.
 * Reserved for the post-MVP `recurring` context; always null in the MVP.
 */
class RecurringRef(value: String) : StringValueObject(value.trim()) {
    init {
        require(this.value.isNotEmpty()) { "RecurringRef cannot be blank if present" }
        require(this.value.length <= MAX_LENGTH) { "RecurringRef cannot exceed $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 64
    }
}

/**
 * Groups the N movements created together in a single batch capture ("vaciar la
 * cesta", CONCEPTS-SPEC §4.4 / D0.4): they share this opaque id so the list can
 * show them together and "undo the batch" deletes them as one. Purely a
 * presentation/undo grouping — each movement remains an independent aggregate.
 */
class BatchRef(value: String) : StringValueObject(value.trim()) {
    init {
        require(this.value.isNotEmpty()) { "BatchRef cannot be blank if present" }
        require(this.value.length <= MAX_LENGTH) { "BatchRef cannot exceed $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 64
    }
}

/**
 * Identifies the payer or origin entity of an income (salary employer,
 * client name, government scheme...). Only meaningful when the
 * transaction's type is INCOME; the aggregate enforces that consistency.
 */
class IncomeSource(value: String) : StringValueObject(value.trim()) {
    init {
        require(this.value.isNotEmpty()) { "IncomeSource cannot be blank if present" }
        require(this.value.length <= MAX_LENGTH) { "IncomeSource cannot exceed $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 64
    }
}
