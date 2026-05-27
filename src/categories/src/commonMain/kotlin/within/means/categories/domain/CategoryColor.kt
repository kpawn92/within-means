package within.means.categories.domain

import within.means.shared.domain.StringValueObject

/**
 * Stores the color as a normalized 7-character hex string: `#RRGGBB`.
 * Validates format and uppercases for stable equality.
 */
class CategoryColor(value: String) : StringValueObject(normalize(value)) {

    companion object {
        private val HEX_REGEX = Regex("^#[0-9A-Fa-f]{6}$")

        private fun normalize(raw: String): String {
            val trimmed = raw.trim()
            require(HEX_REGEX.matches(trimmed)) { "Invalid hex color: '$raw' (expected #RRGGBB)" }
            return "#" + trimmed.substring(1).uppercase()
        }
    }
}
