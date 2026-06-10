package within.means.transactions.application.recurring

import within.means.shared.domain.bus.command.Command

/**
 * Edits an existing recurring rule's details. Type and start date are fixed;
 * already-materialized transactions are untouched (past occurrences are
 * immutable), the new values apply to future occurrences.
 */
data class UpdateRecurringRuleCommand(
    val id: String,
    val amountCents: Long,
    val categoryId: String,
    val description: String = "",
    val incomeSource: String? = null,
    val frequency: String,
) : Command
