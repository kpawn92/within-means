package within.means.shared.domain.criteria

import within.means.shared.domain.StringValueObject

class OrderField(value: String) : StringValueObject(value) {
    init {
        require(value.isNotBlank()) { "OrderField cannot be blank" }
    }
}

enum class OrderType(val sql: String) {
    ASC("ASC"),
    DESC("DESC"),
    NONE(""),
    ;

    val isNone: Boolean get() = this == NONE
}

data class Order(val field: OrderField?, val type: OrderType) {
    val isNone: Boolean get() = type.isNone

    companion object {
        fun none(): Order = Order(field = null, type = OrderType.NONE)
        fun asc(field: String): Order = Order(OrderField(field), OrderType.ASC)
        fun desc(field: String): Order = Order(OrderField(field), OrderType.DESC)
    }
}
