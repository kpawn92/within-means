package within.means.concepts.application.create

import within.means.shared.domain.bus.command.Command

data class CreateConceptCommand(
    val id: String? = null,
    val kind: String,
    val label: String,
    val defaultCategoryId: String? = null,
) : Command
