package within.means.users.domain

enum class Locale(val code: String, val displayName: String) {
    ES(code = "es", displayName = "Español"),
    EN(code = "en", displayName = "English"),
    ;

    companion object {
        fun ofCode(code: String): Locale =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: error("Unknown locale code: $code")
    }
}
