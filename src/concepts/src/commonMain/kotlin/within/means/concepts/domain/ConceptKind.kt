package within.means.concepts.domain

/**
 * A concept belongs to either the expense or the income side. They never mix:
 * the chips offered while registering an expense are a different pool from the
 * ones offered for income. TRANSFER carries no concepts in the MVP (see
 * CONCEPTS-SPEC §10-C).
 */
enum class ConceptKind { EXPENSE, INCOME }
