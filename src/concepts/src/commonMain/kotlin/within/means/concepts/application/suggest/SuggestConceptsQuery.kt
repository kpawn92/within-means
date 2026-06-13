package within.means.concepts.application.suggest

import within.means.shared.domain.bus.query.Query

/**
 * Feeds the QuickAdd chip row and the "¿En qué?" autocomplete. [kind] picks the
 * expense/income pool; [prefix] (optional) narrows by normalized key; [limit]
 * caps how many chips to show. Results come most-used first.
 */
data class SuggestConceptsQuery(
    val kind: String,
    val prefix: String? = null,
    val limit: Int = DEFAULT_LIMIT,
) : Query {
    companion object {
        const val DEFAULT_LIMIT = 12
    }
}

/** All concepts of a kind, most-used first (no prefix cap). */
data class ListAllConceptsQuery(val kind: String) : Query
