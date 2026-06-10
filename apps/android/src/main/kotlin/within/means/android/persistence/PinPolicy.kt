package within.means.android.persistence

/**
 * Single source of truth for the PIN length. Onboarding (where the PIN is
 * set) and Unlock (where it is entered) MUST agree — a mismatch would derive
 * a different SQLCipher passphrase and lock the user out of their own data.
 *
 * The passphrase derivation itself (see [PassphraseProvider]) is length-
 * agnostic, so changing this only changes how many digits the UI asks for.
 */
object PinPolicy {
    const val LENGTH = 4
}
