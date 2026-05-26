package within.means.shared.domain.criteria

import within.means.shared.domain.StringValueObject

class FilterField(value: String) : StringValueObject(value) {
    init {
        require(value.isNotBlank()) { "FilterField cannot be blank" }
    }
}

class FilterValue(value: String) : StringValueObject(value)

data class Filter(
    val field: FilterField,
    val operator: FilterOperator,
    val value: FilterValue,
)
