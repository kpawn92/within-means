package within.means.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * App theme. Per SPEC §7-D we keep Material You dynamic color for chrome on
 * Android 12+ (falling back to the warm brand scheme below), and layer the
 * fixed finance/category tokens ([LocalWmColors]) on top so income/expense
 * semantics stay stable regardless of the user's wallpaper.
 */
@Composable
fun WithinMeansTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val light = lightColorScheme(
        primary = Color(0xFF3F8F6B),
        onPrimary = Color(0xFFFAFEF7),
        primaryContainer = Color(0xFFE2F0E8),
        onPrimaryContainer = Color(0xFF12281D),
        secondary = Color(0xFF526350),
        background = Color(0xFFF7F6F1),
        onBackground = Color(0xFF34322F),
        surface = Color(0xFFFEFDFB),
        onSurface = Color(0xFF34322F),
        surfaceVariant = Color(0xFFEFEDE6),
        onSurfaceVariant = Color(0xFF5E5A54),
        surfaceContainer = Color(0xFFEFEDE6),
        surfaceContainerHigh = Color(0xFFE6E3DB),
        outline = Color(0xFFC8C3B9),
        outlineVariant = Color(0xFFDEDAD1),
    )
    val dark = darkColorScheme(
        primary = Color(0xFF5FBE8E),
        onPrimary = Color(0xFF13241A),
        primaryContainer = Color(0xFF22402F),
        onPrimaryContainer = Color(0xFF7FD0A4),
        secondary = Color(0xFFB9CCB6),
        background = Color(0xFF161814),
        onBackground = Color(0xFFF0EFEB),
        surface = Color(0xFF1F221D),
        onSurface = Color(0xFFF0EFEB),
        surfaceVariant = Color(0xFF272A24),
        onSurfaceVariant = Color(0xFFCBC9C2),
        surfaceContainer = Color(0xFF272A24),
        surfaceContainerHigh = Color(0xFF30332C),
        outline = Color(0xFF3E4239),
        outlineVariant = Color(0xFF30332C),
    )
    val isDark = darkTheme
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        supportsDynamic && isDark -> dynamicDarkColorScheme(context)
        supportsDynamic -> dynamicLightColorScheme(context)
        isDark -> dark
        else -> light
    }
    // Keep the transparent system bars (set up via enableEdgeToEdge) legible: dark
    // icons on the light theme, light icons on the dark theme — tracking the in-app
    // theme, not the system one, since the user can force either.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        SideEffect {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalWmColors provides wmColorTokens(isDark)) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
