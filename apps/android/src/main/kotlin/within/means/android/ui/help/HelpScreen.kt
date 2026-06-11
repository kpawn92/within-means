package within.means.android.ui.help

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import within.means.android.ui.components.WmCard
import within.means.android.ui.components.WmEyebrow
import within.means.android.ui.theme.WmRadii

/**
 * Help / "Aprende" view. See doc/within-means-app/HELP-EDUCATION-SPEC.md.
 *
 * Simplicity first (regla #0): the default surface is just header + the 4-step
 * guided route + a single closed disclosure + footer. The libraries (B/C/§5)
 * live collapsed inside the disclosure and never expand on load.
 *
 * Minimalist but warm (§4.5): no shadows, hairline separators, neutral palette,
 * the brand accent only on actions, generous breathing room, gentle motion.
 */
@Composable
fun HelpScreen(
    onBack: () -> Unit = {},
    onAction: (HelpAction) -> Unit = {},
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = WmSpacingGutter)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.size(4.dp))

            // Header — light, no hero.
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.size(4.dp))
                Column {
                    WmEyebrow("Aprende")
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Mueve tu dinero con intención",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // §4.2 — the guided route: the only thing one *needs* to read.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Empieza por aquí",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                WmCard(modifier = Modifier.fillMaxWidth(), contentPadding = 4.dp) {
                    Column {
                        guidedRoute.forEachIndexed { index, step ->
                            if (index > 0) HairlineDivider()
                            GuideStepRow(step = step, onClick = { onAction(step.action) })
                        }
                    }
                }
            }

            // §4.3 — everything else, opt-in, closed by default.
            DeepenDisclosure(onAction = onAction)

            // §4.4 — gentle disclaimer.
            Text(
                "Esto es una guía para acompañarte, no un consejo financiero profesional. " +
                    "Tú conoces tu vida mejor que nadie.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Spacer(Modifier.size(8.dp))
        }
    }
}

/* ---------- Guided route ---------- */

@Composable
private fun GuideStepRow(step: GuideStep, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "stepScale")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(WmRadii.md))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EmojiBadge(step.emoji)
        Column(Modifier.weight(1f)) {
            Text(
                step.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                step.oneLiner,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = step.action.label,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/* ---------- "¿Quieres profundizar?" disclosure ---------- */

@Composable
private fun DeepenDisclosure(onAction: (HelpAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (open) 180f else 0f, label = "deepenChevron")
    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WmRadii.md))
                .clickable { open = !open }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "¿Quieres profundizar?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Opcional. Lee tus números y técnicas para mover mejor el dinero.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (open) "Ocultar" else "Mostrar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevron),
            )
        }

        if (open) {
            lessonLibraries.forEach { library ->
                LessonLibrarySection(library = library, onAction = onAction)
            }
        }
    }
}

@Composable
private fun LessonLibrarySection(library: LessonLibrary, onAction: (HelpAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text(
                library.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                library.subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        library.lessons.forEach { lesson ->
            LessonCard(lesson = lesson, onAction = onAction)
        }
    }
}

/* ---------- Lesson accordion card ---------- */

@Composable
private fun LessonCard(lesson: Lesson, onAction: (HelpAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "lessonChevron")
    WmCard(modifier = Modifier.fillMaxWidth().animateContentSize(), contentPadding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WmRadii.sm))
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                EmojiBadge(lesson.emoji)
                Column(Modifier.weight(1f)) {
                    Text(
                        lesson.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        lesson.oneLiner,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Ocultar" else "Leer más",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevron),
                )
            }

            if (expanded) {
                HairlineDivider()
                LessonBlock("Qué es", lesson.whatItIs)
                LessonBlock("Por qué importa", lesson.whyItMatters)
                LessonBlock("En la app", lesson.inTheApp)
                LessonBlock("La idea", lesson.rule)
                lesson.alsoKnownAs?.let {
                    Text(
                        "También llamado: $it",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                lesson.action?.let { action ->
                    LessonActionButton(action = action, onClick = { onAction(action) })
                }
            }
        }
    }
}

@Composable
private fun LessonBlock(label: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            body,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Discreet tonal action — invites, doesn't push (§4.5-7). */
@Composable
private fun LessonActionButton(action: HelpAction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(WmRadii.pill))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            action.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/* ---------- Small shared bits ---------- */

@Composable
private fun EmojiBadge(emoji: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 18.sp)
    }
}

@Composable
private fun HairlineDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.height(1.dp),
    )
}

private val WmSpacingGutter = 20.dp
