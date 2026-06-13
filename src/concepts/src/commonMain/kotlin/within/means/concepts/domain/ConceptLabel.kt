package within.means.concepts.domain

import within.means.shared.domain.StringValueObject

/**
 * What the user sees and typed, verbatim (modulo trimming): "Cerveza",
 * "Bus casa→trabajo", "Papá". Emoji and accents are allowed here — they are
 * stripped only for the [ConceptKey] used to aggregate.
 */
class ConceptLabel(value: String) : StringValueObject(value.trim()) {
    init {
        require(this.value.isNotEmpty()) { "ConceptLabel cannot be blank" }
        require(this.value.length <= MAX_LENGTH) { "ConceptLabel cannot exceed $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 40
    }
}
