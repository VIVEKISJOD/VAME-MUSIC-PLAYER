package com.ameos.music

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color

val VameSeed = Color(0xFFC8283D)
val VamePlayRed = Color(0xFFE0314A)

private val VameColorsUnused = darkColorScheme(
    primary = Color(0xFFFFB3B9),
    onPrimary = Color(0xFF560018),
    primaryContainer = VameSeed,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFE6BDC0),
    onSecondary = Color(0xFF44292C),
    background = Color(0xFF130709),
    onBackground = Color(0xFFF3DEDF),
    surface = Color(0xFF130709),
    onSurface = Color(0xFFF3DEDF),
    surfaceVariant = Color(0xFF2E1A1D),
    onSurfaceVariant = Color(0xFFD9C0C2),
    surfaceContainer = Color(0xFF1E0F12),
    surfaceContainerHigh = Color(0xFF28161A),
    outline = Color(0xFF9F8C8E),
    error = Color(0xFFFFB4AB)
)

// true inside the screens that are always dark (player, lyrics, artist page, standby, TV, recap)
val LocalDark = compositionLocalOf { false }

// a dark island inside the light app: white text, dark Material colours
@Composable
fun DarkScope(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Pal.accentSoft,
        onPrimary = Color(0xFF1B1315),
        background = Color(0xFF1C1B1B),
        onBackground = Color.White,
        surface = Color(0xFF1C1B1B),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF2B2A2A),
        onSurfaceVariant = Color(0xCCFFFFFF),
        surfaceContainer = Color(0xE61E1D1D),
        surfaceContainerHigh = Color(0xE6282727),
        outline = Color(0x88FFFFFF),
        error = Color(0xFFFFB4AB)
    )
    CompositionLocalProvider(LocalDark provides true) {
        MaterialTheme(colorScheme = colors, typography = Typography()) { content() }
    }
}

@Composable
fun VameTheme(content: @Composable () -> Unit) {
    val colors = if (Pal.light) lightColorScheme(
        primary = Pal.accent,
        onPrimary = Color.White,
        primaryContainer = Pal.accent.copy(alpha = 0.18f),
        onPrimaryContainer = Pal.fg,
        secondary = Pal.textDim,
        onSecondary = Color.White,
        background = Pal.bgMid,
        onBackground = Pal.fg,
        surface = Pal.bgMid,
        onSurface = Pal.fg,
        surfaceVariant = Pal.surface2,
        onSurfaceVariant = Pal.textDim,
        surfaceContainer = Color(0xF2F6F6FA),
        surfaceContainerHigh = Color(0xF2EEEEF4),
        outline = Pal.muted,
        error = Color(0xFFB3261E)
    ) else darkColorScheme(
        primary = Pal.accent,
        onPrimary = Color(0xFF1B1315),
        primaryContainer = Pal.accentDeep,
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Pal.textDim,
        onSecondary = Color(0xFF1B1315),
        background = Pal.bgMid,
        onBackground = Color(0xFFF3DEDF),
        surface = Pal.bgMid,
        onSurface = Color(0xFFF3DEDF),
        surfaceVariant = Pal.surface2,
        onSurfaceVariant = Pal.textDim,
        surfaceContainer = Pal.surface.copy(alpha = if (Pal.mode == 1) 1f else 0.88f),
        surfaceContainerHigh = Pal.surface2.copy(alpha = if (Pal.mode == 1) 1f else 0.9f),
        outline = Pal.muted,
        error = Color(0xFFFFB4AB)
    )
    // Settings > Interface size scales every dp / sp in the app
    val base = LocalDensity.current
    val scaled = Density(base.density * UiOpts.sizeScale, base.fontScale)
    CompositionLocalProvider(LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography()
        ) {
            // on a TV every button that the remote lands on gets a white focus ring
            CompositionLocalProvider(
                LocalIndication provides (if (UiOpts.tv) TvFocusIndication else LocalIndication.current)
            ) {
                content()
            }
        }
    }
}
