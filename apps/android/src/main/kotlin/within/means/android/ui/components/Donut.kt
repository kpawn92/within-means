package within.means.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A single donut segment: a value and the colour to paint it. */
data class DonutSegment(val value: Float, val color: Color)

/**
 * Ring chart with rounded segment caps and a hole. Segments are drawn
 * proportionally to their share of the total, starting at 12 o'clock.
 * [content] is centered in the hole (e.g. a percentage label).
 */
@Composable
fun WmDonut(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    thickness: Dp = 16.dp,
    gapDegrees: Float = 3f,
    trackColor: Color? = null,
    content: @Composable () -> Unit = {},
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = thickness.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            trackColor?.let {
                drawArc(
                    color = it,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx),
                )
            }
            if (total <= 0f) return@Canvas

            val hasGaps = segments.count { it.value > 0f } > 1
            var start = -90f
            segments.forEach { seg ->
                if (seg.value <= 0f) return@forEach
                val full = seg.value / total * 360f
                val gap = if (hasGaps) gapDegrees else 0f
                val sweep = (full - gap).coerceAtLeast(0.5f)
                drawArc(
                    color = seg.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
                start += full
            }
        }
        content()
    }
}
