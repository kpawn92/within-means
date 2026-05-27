package within.means.categories.application.rename

import within.means.shared.domain.bus.command.Command

data class RenameCategoryCommand(
    val categoryId: String,
    val newName: String,
) : Command
