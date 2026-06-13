package within.means.android.capture

import within.means.categories.domain.Category
import within.means.categories.domain.EngelGroup
import within.means.concepts.domain.ConceptKey

/**
 * Guesses the category a brand-new concept should infer (CONCEPTS-SPEC §8.3),
 * so "gasolina" lands in Transporte and "cerveza" in Comida without asking. Pure
 * local heuristic — no network, no jargon. If nothing matches it returns null
 * and the caller falls back to "Otros".
 *
 * Order, most-specific first:
 *  1. **name match** — a category whose (normalized) name shares a word with the
 *     concept ("Cerveza" vs a "Cerveza" category, or "bus" vs "Bus");
 *  2. **synonym → Engel group** — a seeded keyword maps to a spend group, then
 *     the first category in that group wins ("gasolina" → TRANSPORT → Transporte).
 *
 * Lives in apps/android because it spans `concepts` and `categories`.
 */
class ConceptCategorySuggester {

    /** [categories] should already be filtered to the relevant kind (EXPENSE/INCOME). */
    fun suggest(label: String, categories: List<Category>): String? {
        val words = runCatching { ConceptKey.of(label).value }.getOrNull()
            ?.split(" ")?.filter { it.isNotEmpty() }
            ?: return null
        if (words.isEmpty()) return null

        // 1. Name match against any category.
        categories.firstOrNull { cat ->
            val nameWords = normalizedWords(cat.name.value)
            words.any { it in nameWords }
        }?.let { return it.id.value }

        // 2. Synonym → Engel group → first category in that group.
        val group = words.firstNotNullOfOrNull { SYNONYMS[it] } ?: return null
        return categories.firstOrNull { it.classifiers.engelGroup == group }?.id?.value
    }

    private fun normalizedWords(text: String): Set<String> =
        runCatching { ConceptKey.of(text).value }.getOrNull()
            ?.split(" ")?.filter { it.isNotEmpty() }?.toSet()
            ?: emptySet()

    companion object {
        /** Seeded keyword → spend group. Keys are already in normalized (key) form. */
        private val SYNONYMS: Map<String, EngelGroup> = buildMap {
            listOf(
                "cerveza", "vino", "cafe", "pan", "leche", "comida", "restaurante",
                "super", "mercado", "fruta", "verdura", "carne", "pizza", "almuerzo",
                "cena", "desayuno", "snack", "bebida", "agua",
            ).forEach { put(it, EngelGroup.FOOD) }

            listOf(
                "bus", "metro", "taxi", "uber", "cabify", "gasolina", "combustible",
                "tren", "peaje", "parking", "transporte", "coche", "carro", "moto",
                "bici", "vuelo", "avion", "billete", "pasaje",
            ).forEach { put(it, EngelGroup.TRANSPORT) }

            listOf(
                "alquiler", "hipoteca", "luz", "gas", "internet", "casa", "hogar",
                "limpieza", "detergente", "muebles", "comunidad",
            ).forEach { put(it, EngelGroup.HOUSING) }

            listOf(
                "medico", "farmacia", "medicina", "dentista", "salud", "gym",
                "gimnasio", "optica", "psicologo",
            ).forEach { put(it, EngelGroup.HEALTH) }

            listOf(
                "libro", "curso", "colegio", "universidad", "formacion", "educacion",
                "matricula", "academia",
            ).forEach { put(it, EngelGroup.EDUCATION) }

            listOf(
                "cine", "netflix", "spotify", "juego", "ocio", "viaje", "concierto",
                "bar", "fiesta", "hobby", "suscripcion",
            ).forEach { put(it, EngelGroup.LEISURE) }
        }
    }
}
