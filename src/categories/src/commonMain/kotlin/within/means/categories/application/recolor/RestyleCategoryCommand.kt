package within.means.categories.application.recolor

import within.means.shared.domain.bus.command.Command

data class RestyleCategoryCommand(
    val categoryId: String,
    val newColor: String,
    val newIcon: String,
) : Command
