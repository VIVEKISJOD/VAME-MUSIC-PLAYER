package com.ameos.music

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AmeosSeed = Color(0xFFC8283D)
val AmeosPlayRed = Color(0xFFE0314A)

private val AmeosColors = darkColorScheme(
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
    MaterialTheme(
        colorScheme = AmeosColors,
        typography = Typography(),
        content = content
    )
}
