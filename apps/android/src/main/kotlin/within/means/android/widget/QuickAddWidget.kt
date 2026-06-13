package within.means.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import within.means.android.QuickActions

/**
 * Home-screen "Añadir rápido" widget (CONCEPTS-SPEC §4.2-2): two big buttons that
 * deep-link into the concept-enabled editor with the type preset. Built with
 * Jetpack Glance (Compose for widgets) + Material You via [GlanceTheme] (dynamic
 * colour on API 31+, brand/Material fallback below).
 *
 * The widget never touches the encrypted DB — it only starts the (locked) app,
 * which routes to QuickAdd once unlocked.
 */
class QuickAddWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetBody(context) }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

@Composable
private fun WidgetBody(context: Context) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Añadir rápido",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium),
            )
            Spacer(GlanceModifier.height(10.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                ActionTile(context, "−  Gasto", QuickActions.NEW_EXPENSE, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(8.dp))
                ActionTile(context, "+  Ingreso", QuickActions.NEW_INCOME, GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun ActionTile(context: Context, label: String, action: String, modifier: GlanceModifier) {
    val intent = Intent(action).setClassName(context, "within.means.android.MainActivity")
    Box(
        modifier = modifier
            .background(GlanceTheme.colors.primary)
            .padding(vertical = 12.dp)
            .clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(color = GlanceTheme.colors.onPrimary, fontWeight = FontWeight.Medium))
    }
}
