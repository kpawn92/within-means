package within.means.android.ui.error

import co.touchlab.kermit.Logger

/**
 * Stage of the flow where the error happened. Used to pick the right
 * user-facing wording. The technical exception is always logged with
 * Kermit so we don't lose it.
 */
enum class ErrorContext {
    UNLOCK_DATABASE,
    BOOTSTRAP_USER,
    UPDATE_PREFERENCES,
    LOAD_USER,
    GENERIC,
}

/**
 * Translates any [Throwable] into a short, friendly Spanish message
 * suitable for end users. Logs the technical details for developers.
 */
fun Throwable.toUserMessage(context: ErrorContext): String {
    Logger.withTag("UserError").e(this) { "context=$context" }

    return when (context) {
        ErrorContext.UNLOCK_DATABASE -> when {
            isKnownCipherError() -> "No se pudo descifrar la base de datos. Comprueba tu PIN."
            else -> "Hubo un problema abriendo los datos. Inténtalo de nuevo."
        }
        ErrorContext.BOOTSTRAP_USER ->
            "No se pudo preparar tu perfil. Inténtalo de nuevo."
        ErrorContext.UPDATE_PREFERENCES -> when (this) {
            is IllegalArgumentException -> message ?: "Revisa los datos introducidos."
            else -> "No se pudieron guardar tus preferencias. Inténtalo de nuevo."
        }
        ErrorContext.LOAD_USER ->
            "No se pudieron cargar tus datos. Inténtalo de nuevo."
        ErrorContext.GENERIC ->
            "Algo no salió como esperábamos. Inténtalo de nuevo."
    }
}

private fun Throwable.isKnownCipherError(): Boolean {
    val msg = (message ?: "") + " " + (cause?.message ?: "")
    return msg.contains("file is not a database", ignoreCase = true) ||
        msg.contains("decryption failed", ignoreCase = true) ||
        msg.contains("sqlite", ignoreCase = true)
}
