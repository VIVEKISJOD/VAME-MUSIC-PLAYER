package com.ameos.music

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// The look of the app. Every colour comes from one hue (+ how colourful it is) and a mode:
// 0 = colour glass, 1 = AMOLED (pure black, flat), 2 = AMOLED glass (pure black + glass).
object Pal {
    var hue by mutableFloatStateOf(355f)
    var sat by mutableFloatStateOf(1f)
    var mode by mutableIntStateOf(0)

    val amoled: Boolean get() = mode != 0
    val glassy: Boolean get() = mode != 1

    private fun hsv(s: Float, v: Float, a: Float = 1f): Color =
        Color.hsv(hue.coerceIn(0f, 359.9f), (s * sat).coerceIn(0f, 1f), v.coerceIn(0f, 1f), a)

    val bgTop: Color get() = if (amoled) Color.Black else hsv(0.63f, 0.23f)
    val bgMid: Color get() = if (amoled) Color.Black else hsv(0.15f, 0.07f)
    val bgBot: Color get() = if (amoled) Color.Black else hsv(0.15f, 0.04f)
    val accent: Color get() = hsv(0.30f, 1f)
    val accentSoft: Color get() = hsv(0.22f, 1f)
    val accentDeep: Color get() = hsv(0.80f, 0.48f)
    val textDim: Color get() = hsv(0.08f, 0.86f)
    val muted: Color get() = hsv(0.06f, 0.55f)
    val surface: Color get() = if (mode == 1) Color(0xFF0E0E0E) else if (mode == 2) Color(0xFF111111) else hsv(0.20f, 0.17f)
    val surface2: Color get() = if (mode == 1) Color(0xFF181818) else if (mode == 2) Color(0xFF1C1C1C) else hsv(0.15f, 0.22f)
    val miniA: Color get() = hsv(0.80f, 0.36f, 0.82f)
    val miniB: Color get() = hsv(0.80f, 0.17f, 0.82f)
    val glassTop: Color get() = if (mode == 1) Color(0xFF121212) else if (mode == 2) Color(0x40FFFFFF) else hsv(0.38f, 0.165f, 0.8f)
    val glassBottom: Color get() = if (mode == 1) Color(0xFF0C0C0C) else if (mode == 2) Color(0x14FFFFFF) else hsv(0.33f, 0.07f, 0.8f)
    val panelTop: Color get() = if (mode == 1) Color(0xFF121212) else if (mode == 2) Color(0xF0242424) else hsv(0.25f, 0.22f, 0.94f)
    val panelBottom: Color get() = if (mode == 1) Color(0xFF0C0C0C) else if (mode == 2) Color(0xF0101010) else hsv(0.33f, 0.12f, 0.94f)

    fun load(c: Context) {
        val sp = c.getSharedPreferences("vame", Context.MODE_PRIVATE)
        hue = sp.getFloat("themeHue", 355f)
        sat = sp.getFloat("themeSat", 1f)
        mode = sp.getInt("themeMode", 0).coerceIn(0, 2)
    }

    fun save(c: Context) {
        c.getSharedPreferences("vame", Context.MODE_PRIVATE).edit()
            .putFloat("themeHue", hue).putFloat("themeSat", sat).putInt("themeMode", mode).apply()
    }
}
