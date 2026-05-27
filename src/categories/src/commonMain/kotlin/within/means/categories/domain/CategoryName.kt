package within.means.categories.domain

import within.means.shared.domain.StringValueObject

class CategoryName(value: String) : StringValueObject(value.trim()) {
    init {
        require(this.value.isNotEmpty()) { "CategoryName cannot be blank" }
        require(this.value.length <= MAX_LENGTH) { "CategoryName cannot exceed $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 48
    }
}
