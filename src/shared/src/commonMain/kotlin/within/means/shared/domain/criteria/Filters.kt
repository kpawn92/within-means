package within.means.shared.domain.criteria

data class Filters(val filters: List<Filter>) {
    val isEmpty: Boolean get() = filters.isEmpty()
    val isNotEmpty: Boolean get() = filters.isNotEmpty()

    companion object {
        fun none(): Filters = Filters(emptyList())
        fun of(vararg filters: Filter): Filters = Filters(filters.toList())
    }
}
