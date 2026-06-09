package within.means.android.ui.format

import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private val MONTHS_SHORT = listOf(
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
)
private val DAYS_SHORT = mapOf(
    DayOfWeek.MONDAY to "lun", DayOfWeek.TUESDAY to "mar", DayOfWeek.WEDNESDAY to "mié",
    DayOfWeek.THURSDAY to "jue", DayOfWeek.FRIDAY to "vie", DayOfWeek.SATURDAY to "sáb",
    DayOfWeek.SUNDAY to "dom",
)

/** Parses an ISO date string (`YYYY-MM-DD`), tolerating a trailing time. */
fun parseIsoDate(raw: String): LocalDate? =
    runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()

/**
 * Relative day label used to group the transactions list:
 * "Hoy", "Ayer", "mié 4" (within the week), or "4 jun" beyond that.
 */
fun relativeDayLabel(
    raw: String,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
): String {
    val date = parseIsoDate(raw) ?: return raw
    val diff = today.toEpochDays() - date.toEpochDays()
    return when {
        diff == 0 -> "Hoy"
        diff == 1 -> "Ayer"
        diff in 2..6 -> "${DAYS_SHORT[date.dayOfWeek]} ${date.dayOfMonth}"
        else -> "${date.dayOfMonth} ${MONTHS_SHORT.getOrElse(date.monthNumber - 1) { "" }}"
    }
}
