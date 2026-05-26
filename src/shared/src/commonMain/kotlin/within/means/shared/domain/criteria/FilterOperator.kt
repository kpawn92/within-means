package within.means.shared.domain.criteria

enum class FilterOperator(val sql: String) {
    EQUALS("="),
    NOT_EQUALS("!="),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    CONTAINS("LIKE"),
    NOT_CONTAINS("NOT LIKE"),
    ;

    companion object {
        fun fromSymbol(value: String): FilterOperator =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.sql == value }
                ?: error("Unknown filter operator: $value")
    }
}
