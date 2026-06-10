package within.means.android.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * True when the user (or the system) has disabled animations — mirrors the
 * web `prefers-reduced-motion`. We read Android's global animator duration
 * scale; 0 means "no animations", so callers should render final state
 * immediately instead of tweening.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}

/**
 * Animates an integer-cents value from its previous value up to [target]
 * (count-up). Returns [target] verbatim when motion is reduced.
 */
@Composable
fun countUpCents(target: Long, reducedMotion: Boolean): Long {
    if (reducedMotion) return target
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), tween(durationMillis = 600)) }
    return anim.value.toLong()
}

/**
 * Staggered "enter up": fades and lifts content into place, delayed by its
 * [index] so blocks cascade. No-op when motion is reduced.
 */
fun Modifier.enterUp(index: Int, reducedMotion: Boolean): Modifier = composed {
    if (reducedMotion) return@composed this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 55L)
        progress.animateTo(1f, tween(durationMillis = 320))
    }
    this
        .alpha(progress.value)
        .graphicsLayer { translationY = (1f - progress.value) * 40f }
}
