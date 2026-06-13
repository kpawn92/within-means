package within.means.concepts.application.record_usage

import within.means.shared.domain.bus.command.Command

/**
 * Bumps a concept's usage so it floats up the chip row. [atIso] is the moment
 * it was used (the movement's instant); null means "now".
 */
data class RecordConceptUsageCommand(
    val conceptId: String,
    val atIso: String? = null,
) : Command
