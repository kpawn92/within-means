package within.means.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fixed design tokens that Material 3's dynamic color scheme does not provide.
 *
 * Per SPEC §7-D we keep Material You (dynamic) for "chrome" — surfaces, primary,
 * text — but finance semantics (income green / expense terracotta) and the
 * category palette must NOT follow the wallpaper, so they live here as fixed
 * values that still flip between light and dark.
 */
data class WmColorTokens(
    /** Expense / negative — warm terracotta, never pure red. */
    val neg: Color,
    /** Soft terracotta background for negative emphasis. */
    val negSoft: Color,
    /** Income / positive — brand green, independent of dynamic primary. */
    val pos: Color,
    /** Soft green background for positive emphasis. */
    val posSoft: Color,
    /** Savings / transfer accent (olive-leaning brand). */
    val savings: Color,
    /** Inactive bar/track fill. */
    val track: Color,
)

private val LightTokens = WmColorTokens(
    neg = Color(0xFFC25B47),
    negSoft = Color(0xFFF4E2DC),
    pos = Color(0xFF3F8F6B),
    posSoft = Color(0xFFE2F0E8),
    savings = Color(0xFF6E8B3D),
    track = Color(0xFFE6E3DB),
)

private val DarkTokens = WmColorTokens(
    neg = Color(0xFFD98162),
    negSoft = Color(0xFF3A271F),
    pos = Color(0xFF5FBE8E),
    posSoft = Color(0xFF22402F),
    savings = Color(0xFF9CB35E),
    track = Color(0xFF30332C),
)

fun wmColorTokens(dark: Boolean): WmColorTokens = if (dark) DarkTokens else LightTokens

val LocalWmColors = staticCompositionLocalOf { LightTokens }

/**
 * The fixed 13-colour category palette from the design. Category records store
 * a hex string; [categoryColor] parses it, falling back to a palette entry.
 */
val WmCategoryPalette: List<Color> = listOf(
    Color(0xFF3F8F6B), Color(0xFFC8783C), Color(0xFFC0504D), Color(0xFF5B7FB4),
    Color(0xFF8A6BB1), Color(0xFFD4A12E), Color(0xFF4FA3A3), Color(0xFFB5556E),
    Color(0xFF6E8B3D), Color(0xFFA36A4F), Color(0xFF717A8C), Color(0xFFC77FA6),
    Color(0xFF3E7A8C),
)

/** Parses a `#RRGGBB` / `#AARRGGBB` hex string; falls back to the first palette colour. */
fun categoryColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return WmCategoryPalette.first()
    val clean = hex.removePrefix("#")
    return runCatching {
        val v = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000 or v)
            8 -> Color(v)
            else -> WmCategoryPalette.first()
        }
    }.getOrDefault(WmCategoryPalette.first())
}

/** Shared corner radii. Cards use [lg]; the hero card [hero]; FAB [fab]. */
object WmRadii {
    val sm: Dp = 10.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val hero: Dp = 28.dp
    val fab: Dp = 18.dp
    val pill: Dp = 999.dp
}

/** Content padding tokens (20dp lateral gutter in the design). */
object WmSpacing {
    val gutter: Dp = 20.dp
    val card: Dp = 18.dp
    val block: Dp = 16.dp
}

/** Convenience accessor mirroring `MaterialTheme.colorScheme`. */
object WmTheme {
    val colors: WmColorTokens
        @Composable @ReadOnlyComposable get() = LocalWmColors.current
}
