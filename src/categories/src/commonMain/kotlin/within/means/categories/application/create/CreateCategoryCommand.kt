package within.means.categories.application.create

import within.means.shared.domain.bus.command.Command

data class CreateCategoryCommand(
    val id: String? = null,
    val kind: String,
    val name: String,
    val color: String,
    val icon: String,
    val nature: String? = null,
    val essentiality: String? = null,
    val productive: Boolean = false,
    val engelGroup: String? = null,
    val parentId: String? = null,
) : Command
