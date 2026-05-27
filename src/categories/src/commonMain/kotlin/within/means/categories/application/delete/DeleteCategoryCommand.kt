package within.means.categories.application.delete

import within.means.shared.domain.bus.command.Command

data class DeleteCategoryCommand(val categoryId: String) : Command
