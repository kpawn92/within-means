package within.means.concepts.application.rename

import within.means.shared.domain.bus.command.Command

data class RenameConceptCommand(
    val conceptId: String,
    val newLabel: String,
) : Command
