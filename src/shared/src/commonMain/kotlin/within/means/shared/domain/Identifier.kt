package within.means.shared.domain

abstract class Identifier(value: String) : StringValueObject(value) {
    init {
        require(UUID_REGEX.matches(value)) { "Invalid UUID: '$value'" }
    }

    companion object {
        private val UUID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )
    }
}
