package within.means.shared.infrastructure.persistence

import within.means.shared.domain.criteria.Criteria
import within.means.shared.domain.criteria.Filter
import within.means.shared.domain.criteria.FilterOperator

data class TranslatedSql(val sql: String, val args: List<String>)

/**
 * Translates a domain Criteria into parametrized SQL with a whitelist of allowed columns.
 * Values are always passed as `?` bind parameters; never concatenated into the SQL string.
 */
object SqlCriteriaTranslator {

    fun translate(
        criteria: Criteria,
        table: String,
        allowedFields: Set<String>,
    ): TranslatedSql {
        require(table.matches(IDENTIFIER_REGEX)) { "Invalid table name: '$table'" }

        val sb = StringBuilder()
        sb.append("SELECT * FROM ").append(table)

        val args = mutableListOf<String>()

        if (criteria.hasFilters) {
            sb.append(" WHERE ")
            val clauses = criteria.filters.filters.map { filter ->
                requireAllowed(filter.field.value, allowedFields, "filter")
                args.add(adaptValue(filter))
                "${filter.field.value} ${filter.operator.sql} ?"
            }
            sb.append(clauses.joinToString(" AND "))
        }

        if (criteria.hasOrder) {
            val orderField = criteria.order.field!!.value
            requireAllowed(orderField, allowedFields, "order")
            sb.append(" ORDER BY ").append(orderField).append(' ').append(criteria.order.type.sql)
        }

        criteria.limit?.let { sb.append(" LIMIT ").append(it) }
        criteria.offset?.let { sb.append(" OFFSET ").append(it) }

        return TranslatedSql(sb.toString(), args.toList())
    }

    private fun adaptValue(filter: Filter): String = when (filter.operator) {
        FilterOperator.CONTAINS, FilterOperator.NOT_CONTAINS -> "%${filter.value.value}%"
        else -> filter.value.value
    }

    private fun requireAllowed(field: String, allowed: Set<String>, kind: String) {
        require(field in allowed) { "Disallowed $kind field: '$field'" }
    }

    private val IDENTIFIER_REGEX = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
}
