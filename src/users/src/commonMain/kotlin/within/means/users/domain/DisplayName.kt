package within.means.users.domain

import within.means.shared.domain.StringValueObject

class DisplayName(value: String) : StringValueObject(value.trim()) {
    init {
        require(this.value.isNotEmpty()) { "DisplayName cannot be blank" }
        require(this.value.length <= MAX_LENGTH) { "DisplayName cannot exceed $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 64
    }
}
