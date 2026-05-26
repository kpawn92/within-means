package within.means.shared.domain.criteria

data class Criteria(
    val filters: Filters,
    val order: Order = Order.none(),
    val limit: Int? = null,
    val offset: Int? = null,
) {
    init {
        require(limit == null || limit >= 0) { "limit must be >= 0" }
        require(offset == null || offset >= 0) { "offset must be >= 0" }
    }

    val hasFilters: Boolean get() = filters.isNotEmpty
    val hasOrder: Boolean get() = !order.isNone
    val hasPagination: Boolean get() = limit != null || offset != null
}
