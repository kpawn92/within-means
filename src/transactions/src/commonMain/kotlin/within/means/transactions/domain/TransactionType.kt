package within.means.transactions.domain

/**
 * The kinds of movement the app tracks.
 *
 * `TRANSFER` models money the user sets aside ("Ahorro"): it is neither an
 * income nor an expense in net-worth terms, just a move out of the spendable
 * pool. Without an `accounts` context there is no explicit source / target —
 * the design treats it as a single "savings" bucket.
 */
enum class TransactionType { INCOME, EXPENSE, TRANSFER }
