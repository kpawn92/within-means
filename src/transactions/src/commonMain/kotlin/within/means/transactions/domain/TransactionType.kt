package within.means.transactions.domain

/**
 * The two kinds of movement tracked in the MVP. `TRANSFER` is intentionally
 * out of scope until the `accounts` bounded context lands post-MVP — a
 * transfer requires explicit source / target accounts to be meaningful.
 */
enum class TransactionType { INCOME, EXPENSE }
