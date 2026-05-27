package within.means.categories.domain

import within.means.shared.domain.StringValueObject

/**
 * Identifier of an icon, e.g. "shopping_cart", "home", "directions_car".
 * The mapping to a concrete drawable is a UI concern.
 */
class CategoryIcon(value: String) : StringValueObject(value.trim().lowercase()) {
    init {
        require(this.value.isNotEmpty()) { "CategoryIcon cannot be blank" }
        require(VALID_REGEX.matches(this.value)) { "Invalid icon id: '$value'" }
    }

    companion object {
        private val VALID_REGEX = Regex("^[a-z][a-z0-9_]{0,31}$")
    }
}
