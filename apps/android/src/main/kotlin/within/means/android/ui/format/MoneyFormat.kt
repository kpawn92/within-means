package within.means.android.ui.format

/**
 * Formats `cents` as a US-style currency string: comma thousands
 * separators, dot decimal, negative sign prefix when applicable.
 * The currency symbol itself is rendered separately by the caller.
 *
 *   12345  → "123.45"
 *   120000 → "1,200.00"
 *   -1550  → "-15.50"
 *   0      → "0.00"
 */
fun formatMoney(cents: Long): String {
    val negative = cents < 0L
    val abs = if (negative) -cents else cents
    val whole = abs / 100L
    val frac = (abs % 100L).toString().padStart(2, '0')
    val grouped = whole.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    val sign = if (negative) "-" else ""
    return "$sign$grouped.$frac"
}

/** Maps an ISO-4217-ish base-currency code to its glyph; defaults to `$`. */
fun currencySymbol(code: String?): String = when (code?.uppercase()) {
    "EUR" -> "€"
    "GBP" -> "£"
    "CUP" -> "$"
    else -> "$"
}

/**
 * Display amount with currency symbol and optional sign, mirroring the design:
 * big hero figures drop the decimals (`decimals = false`).
 *
 *   formatAmount(120000, "$")                  → "$1,200.00"
 *   formatAmount(120000, "$", decimals=false)  → "$1,200"
 *   formatAmount(-1550, "$", signed=true)      → "−$15.50"
 */
fun formatAmount(
    cents: Long,
    symbol: String = "$",
    signed: Boolean = false,
    decimals: Boolean = true,
): String {
    val negative = cents < 0L
    val abs = if (negative) -cents else cents
    val body = if (decimals) formatMoney(abs) else {
        val whole = abs / 100L
        whole.toString().reversed().chunked(3).joinToString(",").reversed()
    }
    val sign = when {
        signed && negative -> "−"
        signed -> "+"
        negative -> "−"
        else -> ""
    }
    return "$sign$symbol$body"
}
