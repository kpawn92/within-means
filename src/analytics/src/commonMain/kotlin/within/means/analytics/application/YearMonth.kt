package within.means.analytics.application

import kotlinx.datetime.LocalDate

/**
 * Internal year-month helper. Stored on responses and queries as the
 * canonical string format `YYYY-MM` (zero-padded month).
 */
internal data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {

    init {
        require(month in 1..12) { "month must be 1..12 (was $month)" }
    }

    val text: String get() = "$year-${month.toString().padStart(2, '0')}"

    /** Returns the previous month, wrapping the year boundary. */
    fun minus(months: Int): YearMonth {
        val total = year * 12 + (month - 1) - months
        return YearMonth(year = total / 12, month = (total % 12) + 1)
    }

    override fun compareTo(other: YearMonth): Int {
        val byYear = year.compareTo(other.year)
        return if (byYear != 0) byYear else month.compareTo(other.month)
    }

    companion object {
        fun parse(text: String): YearMonth {
            val parts = text.split("-")
            require(parts.size == 2) { "Invalid YearMonth: '$text' (expected YYYY-MM)" }
            return YearMonth(year = parts[0].toInt(), month = parts[1].toInt())
        }

        fun of(date: LocalDate): YearMonth = YearMonth(date.year, date.monthNumber)
    }
}
