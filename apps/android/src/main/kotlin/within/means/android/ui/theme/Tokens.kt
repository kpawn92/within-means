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
 * text — but finance semantics and the category palette must NOT follow the
 * wallpaper, so they live here as fixed values that still flip between light and dark.
 *
 * HARD BRAND RULE (see doc/within-means-app/HOME-DESIGN-SPEC.md §1):
 *   INCOME  = BLUE  ([income]/[incomeSoft])
 *   EXPENSE = RED   ([expense]/[expenseSoft])
 *   SAVINGS = olive brand ([savings])
 * Never use [income]/[expense] for anything that is not money-in / money-out.
 *
 * [pos]/[neg] are NOT income/expense — they are STATE tokens (ok / warning),
 * used for budget "within plan / attention", savings-rate deltas and destructive
 * actions. They stay green / terracotta on purpose so they don't clash with the
 * blue/red flow-of-money semantics.
 */
data class WmColorTokens(
    /** Income / money-in — BLUE. Brand rule, independent of dynamic primary. */
    val income: Color,
    /** Soft blue background for income emphasis (badges/chips). */
    val incomeSoft: Color,
    /** Filled blue container (hero card on a positive net balance). */
    val incomeContainer: Color,
    /** Content/text on [incomeContainer]. */
    val onIncomeContainer: Color,
    /** Expense / money-out — RED. Brand rule. */
    val expense: Color,
    /** Soft red background for expense emphasis. */
    val expenseSoft: Color,
    /** Filled red container (hero card on a negative net balance). */
    val expenseContainer: Color,
    /** Content/text on [expenseContainer]. */
    val onExpenseContainer: Color,
    /** State "warning / bad" — warm terracotta (NOT expense; e.g. budget attention, errors). */
    val neg: Color,
    /** Soft terracotta background for warning state. */
    val negSoft: Color,
    /** State "ok / good" — brand green (NOT income; e.g. within-plan, positive delta). */
    val pos: Color,
    /** Soft green background for ok state. */
    val posSoft: Color,
    /** Savings / transfer accent (olive-leaning brand). */
    val savings: Color,
    /** Inactive bar/track fill. */
    val track: Color,
)

private val LightTokens = WmColorTokens(
    income = Color(0xFF2F6FB3),
    incomeSoft = Color(0xFFE1ECF7),
    incomeContainer = Color(0xFF3F6FA6),
    onIncomeContainer = Color(0xFFF2F7FD),
    // Clean tonal red (not the old orange-brick): green≈blue channel so it reads
    // as a true, refined red that pairs with the income blue. Hex values aren't
    // sacred per HOME-DESIGN-SPEC §1 — only the blue-in / red-out convention is.
    expense = Color(0xFFBE3636),
    expenseSoft = Color(0xFFF7E6E3),
    expenseContainer = Color(0xFFA23F3D),
    onExpenseContainer = Color(0xFFFCF1EF),
    neg = Color(0xFFC25B47),
    negSoft = Color(0xFFF4E2DC),
    pos = Color(0xFF3F8F6B),
    posSoft = Color(0xFFE2F0E8),
    savings = Color(0xFF6E8B3D),
    track = Color(0xFFE6E3DB),
)

private val DarkTokens = WmColorTokens(
    income = Color(0xFF5FA8E0),
    incomeSoft = Color(0xFF1B2C3D),
    incomeContainer = Color(0xFF2C4A6B),
    onIncomeContainer = Color(0xFFDCE8F7),
    // Soft coral-red instead of the old neon #FF5449: legible on dark surfaces
    // without the harsh, cheap glow. Container is a muted deep red (not brick).
    expense = Color(0xFFEC9A92),
    expenseSoft = Color(0xFF3A2420),
    expenseContainer = Color(0xFF7E3A37),
    onExpenseContainer = Color(0xFFFFDAD4),
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
