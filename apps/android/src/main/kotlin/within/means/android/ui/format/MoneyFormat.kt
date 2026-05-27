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
