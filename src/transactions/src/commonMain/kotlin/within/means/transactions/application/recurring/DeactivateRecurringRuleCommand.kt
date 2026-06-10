package within.means.transactions.application.recurring

import within.means.shared.domain.bus.command.Command

/**
 * Stops a recurring rule from producing further occurrences. Already-
 * materialized transactions are left untouched; only the template is
 * deactivated (soft — the row stays for history / auditing).
 */
data class DeactivateRecurringRuleCommand(val id: String) : Command
