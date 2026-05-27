package within.means.categories.application.reclassify

import within.means.shared.domain.bus.command.Command

data class ReclassifyCategoryCommand(
    val categoryId: String,
    val nature: String? = null,
    val essentiality: String? = null,
    val productive: Boolean = false,
    val engelGroup: String? = null,
) : Command
