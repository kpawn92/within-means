package within.means.transactions.domain

import within.means.shared.domain.ValueObject

/**
 * The concepts attached to a movement, referenced by opaque id (the
 * `concepts` context is never imported — same decoupling rationale as
 * [CategoryRef]). 0..N, no duplicates, order = relevance (the first one is the
 * concept that drove the inferred category; see CONCEPTS-SPEC §6.2).
 */
class ConceptRefs(val ids: List<String>) : ValueObject {
    init {
        require(ids.size <= MAX) { "A movement cannot carry more than $MAX concepts (got ${ids.size})" }
        require(ids.distinct().size == ids.size) { "ConceptRefs cannot contain duplicates: $ids" }
        require(ids.none { it.isBlank() }) { "ConceptRefs cannot contain blank ids" }
    }

    val isEmpty: Boolean get() = ids.isEmpty()

    override fun equals(other: Any?): Boolean =
        this === other || (other is ConceptRefs && ids == other.ids)

    override fun hashCode(): Int = ids.hashCode()

    override fun toString(): String = "ConceptRefs($ids)"

    companion object {
        const val MAX = 8
        val EMPTY = ConceptRefs(emptyList())

        fun of(vararg ids: String) = ConceptRefs(ids.toList())
    }
}
