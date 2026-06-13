package within.means.concepts.application.recategorize

import within.means.shared.domain.bus.command.Command

/** Sets (or clears, with null) the category a concept infers from then on. */
data class SetConceptDefaultCategoryCommand(
    val conceptId: String,
    val defaultCategoryId: String?,
) : Command
