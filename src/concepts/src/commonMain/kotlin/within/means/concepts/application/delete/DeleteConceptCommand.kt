package within.means.concepts.application.delete

import within.means.shared.domain.bus.command.Command

data class DeleteConceptCommand(
    val conceptId: String,
) : Command
