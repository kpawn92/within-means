package within.means.concepts.domain

import within.means.shared.domain.StringValueObject

/**
 * The normalized identity of a concept used to aggregate ("¿cuánto gasté en
 * X?"). Two labels that differ only in case, accents, emoji or spacing collapse
 * to the same key, so `"Cerveza"`, `"cerveza"`, `"  CERVEZA "` and `"Cerveza 🍺"`
 * all sum together.
 *
 * Normalization (Kotlin-common safe, no `java.text.Normalizer`):
 *  1. trim + lowercase
 *  2. map accented Latin vowels and `ç` to their base letter — but **keep `ñ`**
 *     ("tildes", per the spec, not the eñe: `año` ≠ `ano`)
 *  3. drop anything that is not a letter, digit or space (removes emoji,
 *     punctuation, symbols)
 *  4. collapse runs of whitespace to a single space, then trim
 *
 * Variantes léxicas (`birra` vs `cerveza`) **no** se fusionan aquí; eso es un
 * merge manual post-MVP (CONCEPTS-SPEC §10-D).
 */
class ConceptKey private constructor(value: String) : StringValueObject(value) {

    companion object {
        private val ACCENTS: Map<Char, Char> = mapOf(
            'á' to 'a', 'à' to 'a', 'â' to 'a', 'ä' to 'a', 'ã' to 'a', 'å' to 'a',
            'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
            'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
            'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'ö' to 'o', 'õ' to 'o',
            'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
            'ç' to 'c',
            'ý' to 'y', 'ÿ' to 'y',
        )

        fun of(label: String): ConceptKey {
            val lowered = label.trim().lowercase()
            val sb = StringBuilder(lowered.length)
            for (raw in lowered) {
                val c = ACCENTS[raw] ?: raw
                when {
                    c.isLetterOrDigit() -> sb.append(c)
                    c.isWhitespace() -> sb.append(' ')
                    // anything else (emoji, punctuation, symbols) is dropped
                }
            }
            val normalized = sb.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
            require(normalized.isNotEmpty()) { "ConceptKey is empty after normalizing '$label'" }
            return ConceptKey(normalized)
        }
    }
}
