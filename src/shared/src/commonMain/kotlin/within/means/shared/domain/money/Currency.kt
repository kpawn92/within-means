package within.means.shared.domain.money

enum class Currency(val code: String, val decimalPlaces: Int, val symbol: String) {
    USD(code = "USD", decimalPlaces = 2, symbol = "$"),
    EUR(code = "EUR", decimalPlaces = 2, symbol = "€"),
    CUP(code = "CUP", decimalPlaces = 2, symbol = "₱"),
    ;

    companion object {
        fun ofCode(code: String): Currency =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: error("Unknown currency code: $code")
    }
}
