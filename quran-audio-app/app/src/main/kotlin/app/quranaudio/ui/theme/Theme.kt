package app.quranaudio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import app.quranaudio.data.prefs.ThemeMode

// Palette: near-black base, deep navy secondary, soft blue/purple accent.
object Palette {
    val Black = Color(0xFF0A0A12)
    val Navy = Color(0xFF121633)
    val NavyDeep = Color(0xFF0E1128)
    val Card = Color(0xFF161A2E)
    val CardElevated = Color(0xFF1D2240)
    val Accent = Color(0xFF9AA7FF)
    val AccentDeep = Color(0xFF6E64E8)
    val Violet = Color(0xFF8B7BF0)
    val TextPrimary = Color(0xFFF4F5FA)
    val TextSecondary = Color(0xFFB4B8CC)
    val TextMuted = Color(0xFF8A8FA8)
    val Outline = Color(0xFF2A2F4A)
    val Error = Color(0xFFFF8A80)

    val LightBackground = Color(0xFFF6F6FB)
    val LightCard = Color(0xFFFFFFFF)
    val LightText = Color(0xFF14162B)
}

private val DarkColors = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Color(0xFF111433),
    primaryContainer = Palette.AccentDeep,
    onPrimaryContainer = Color.White,
    secondary = Palette.Violet,
    onSecondary = Color.White,
    secondaryContainer = Palette.CardElevated,
    onSecondaryContainer = Palette.TextPrimary,
    background = Palette.Black,
    onBackground = Palette.TextPrimary,
    surface = Palette.Black,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.Card,
    onSurfaceVariant = Palette.TextSecondary,
    surfaceContainerLowest = Palette.Black,
    surfaceContainerLow = Palette.NavyDeep,
    surfaceContainer = Palette.Card,
    surfaceContainerHigh = Palette.CardElevated,
    surfaceContainerHighest = Color(0xFF252B4D),
    outline = Palette.Outline,
    outlineVariant = Color(0xFF22263F),
    error = Palette.Error,
)

private val LightColors = lightColorScheme(
    primary = Palette.AccentDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E1FF),
    onPrimaryContainer = Color(0xFF1C1760),
    secondary = Color(0xFF5B52C9),
    background = Palette.LightBackground,
    onBackground = Palette.LightText,
    surface = Palette.LightBackground,
    onSurface = Palette.LightText,
    surfaceVariant = Color(0xFFEDEDF6),
    onSurfaceVariant = Color(0xFF4A4D63),
    surfaceContainerLow = Color(0xFFF0F0F8),
    surfaceContainer = Palette.LightCard,
    surfaceContainerHigh = Color(0xFFF2F2FA),
    surfaceContainerHighest = Color(0xFFE8E8F3),
    outline = Color(0xFFC9CADB),
)

/** Spacing scale used everywhere — avoid ad-hoc dp values in screens. */
@Immutable
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val screen = 20.dp
}

object Radii {
    val card = RoundedCornerShape(20.dp)
    val tile = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50)
    val small = RoundedCornerShape(12.dp)
}

/** Gradient stops for the subtle top-of-screen glow and featured cards. */
@Immutable
data class Gradients(val top: List<Color>, val hero: List<Color>)

val LocalGradients = staticCompositionLocalOf {
    Gradients(top = listOf(Color(0xFF1B1F52), Palette.Black), hero = listOf(Color(0xFF2B2F77), Color(0xFF4B3F9E)))
}

@Composable
fun QuranAudioTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colors: ColorScheme = if (dark) DarkColors else LightColors
    val gradients = if (dark) {
        Gradients(top = listOf(Color(0xFF1C2058), Color(0xFF12153A), Palette.Black), hero = listOf(Color(0xFF2B2F77), Color(0xFF4B3F9E)))
    } else {
        Gradients(top = listOf(Color(0xFFDCDCFF), Palette.LightBackground), hero = listOf(Color(0xFF5B52C9), Color(0xFF8B7BF0)))
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalGradients provides gradients) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}
