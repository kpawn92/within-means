package within.means.android.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import within.means.android.QuickActions
import within.means.android.R
import within.means.concepts.application.ConceptsResponse
import within.means.concepts.application.suggest.SuggestConceptsQuery
import within.means.shared.domain.bus.query.QueryBus

/**
 * Publishes the user's most-used expense concepts as dynamic app shortcuts
 * ("Nuevo · Cerveza"), so the launcher long-press offers one-tap capture of the
 * usual things (CONCEPTS-SPEC §4.2-1). Each deep-links into the concept-enabled
 * editor with the type and concept preselected.
 *
 * Best-effort: any failure (no concepts yet, shortcut limit) is swallowed —
 * dynamic shortcuts are a nicety, never a hard dependency.
 */
object QuickActionShortcuts {

    private const val MAX = 3

    suspend fun refresh(context: Context, queryBus: QueryBus) {
        val concepts = runCatching {
            queryBus.ask<SuggestConceptsQuery, ConceptsResponse>(
                SuggestConceptsQuery(kind = "EXPENSE", limit = MAX)
            )
        }.getOrNull()?.items ?: return

        val shortcuts = concepts.take(MAX).mapIndexed { rank, c ->
            val intent = Intent(QuickActions.NEW_EXPENSE).apply {
                setClassName(context, "within.means.android.MainActivity")
                putExtra(QuickActions.EXTRA_CONCEPT, c.label)
            }
            ShortcutInfoCompat.Builder(context, "concept_${c.id}")
                .setShortLabel(c.label)
                .setLongLabel("Nuevo · ${c.label}")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_expense))
                .setIntent(intent)
                .setRank(rank)
                .build()
        }

        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }
}
