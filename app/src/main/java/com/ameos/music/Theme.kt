package com.ameos.music

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color

val AmeosSeed = Color(0xFFC8283D)
val AmeosPlayRed = Color(0xFFE0314A)

private val AmeosColorsUnused = darkColorScheme(
    primary = Color(0xFFFFB3B9),
    onPrimary = Color(0xFF560018),
    primaryContainer = AmeosSeed,
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

@Composable
fun AmeosTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
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
        surfaceContainer = Pal.surface,
        surfaceContainerHigh = Pal.surface2,
        outline = Pal.muted,
        error = Color(0xFFFFB4AB)
    )
    // Settings > Interface size scales every dp / sp in the app
    val base = LocalDensity.current
    val scaled = Density(base.density * UiOpts.sizeScale, base.fontScale)
    CompositionLocalProvider(LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography(),
            content = content
        )
    }
}
