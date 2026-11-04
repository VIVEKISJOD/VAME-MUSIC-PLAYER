package com.ameos.music

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// The look of the app. Every colour comes from one hue (+ how colourful it is) and a mode:
// 0 = colour glass, 1 = AMOLED (pure black, flat), 2 = AMOLED glass (pure black + glass).
// step64: the app is LIGHT by default (soft grey pages, dark text, one orange accent, glass dock).
// `light` switches the whole app between the light and the dark look; the dark look still uses `mode`.
object Pal {
    var hue by mutableFloatStateOf(20f)
    var sat by mutableFloatStateOf(1f)
    var mode by mutableIntStateOf(0)
    var light by mutableStateOf(true)

    // Settings > Glass (all saved)
    var glassMat by mutableIntStateOf(2)          // 0 Fastest, 1 Balanced, 2 Quality
    var glassColour by mutableFloatStateOf(0.70f) // how much colour the glass takes (0..1)
    var glassFrost by mutableFloatStateOf(4f)     // frost in dp (0..24)
    var glassTint by mutableFloatStateOf(-1f)     // tint hue 0..360, -1 = none
    var glassFilm by mutableFloatStateOf(0.50f)   // white film over the glass (0..1)
    var glassActive by mutableFloatStateOf(0.26f) // strength of the selected tab highlight (0..1)

    val amoled: Boolean get() = !light && mode != 0
    val glassy: Boolean get() = light || mode != 1

    // text / icon colour on pages, and the colour for things that sit on a filled `fg` button
    val fg: Color get() = if (light) Color(0xFF16161A) else Color.White
    val onFg: Color get() = if (light) Color.White else Color(0xFF16161A)

    private fun hsv(s: Float, v: Float, a: Float = 1f): Color =
        Color.hsv(hue.coerceIn(0f, 359.9f), (s * sat).coerceIn(0f, 1f), v.coerceIn(0f, 1f), a)

    val bgTop: Color get() = if (light) Color(0xFFF2F2F7) else if (amoled) Color.Black else hsv(0.63f, 0.23f)
    val bgMid: Color get() = if (light) Color(0xFFF0F0F5) else if (amoled) Color.Black else hsv(0.15f, 0.07f)
    val bgBot: Color get() = if (light) Color(0xFFEEEEF3) else if (amoled) Color.Black else hsv(0.15f, 0.04f)
    val accent: Color get() = if (light) hsv(0.80f, 0.84f) else hsv(0.30f, 1f)
    val accentSoft: Color get() = if (light) hsv(0.55f, 0.95f) else hsv(0.22f, 1f)
    val accentDeep: Color get() = if (light) hsv(0.70f, 0.70f) else hsv(0.80f, 0.48f)
    val textDim: Color get() = if (light) Color(0xFF16161A).copy(alpha = 0.62f) else hsv(0.08f, 0.86f)
    val muted: Color get() = if (light) Color(0xFF16161A).copy(alpha = 0.45f) else hsv(0.06f, 0.55f)
    val surface: Color get() =
        if (light) Color(0xFFE3E3E9)
        else if (mode == 1) Color(0xFF0E0E0E) else if (mode == 2) Color(0xFF111111) else hsv(0.20f, 0.17f)
    val surface2: Color get() =
        if (light) Color(0xFFD9D9E0)
        else if (mode == 1) Color(0xFF181818) else if (mode == 2) Color(0xFF1C1C1C) else hsv(0.15f, 0.22f)
    val miniA: Color get() = hsv(0.80f, 0.36f, 0.82f)
    val miniB: Color get() = hsv(0.80f, 0.17f, 0.82f)
    val glassTop: Color get() =
        if (light) Color(0xCCFFFFFF)
        else if (mode == 1) Color(0xFF121212) else if (mode == 2) Color(0x40FFFFFF) else hsv(0.38f, 0.165f, 0.8f)
    val glassBottom: Color get() =
        if (light) Color(0x99FFFFFF)
        else if (mode == 1) Color(0xFF0C0C0C) else if (mode == 2) Color(0x14FFFFFF) else hsv(0.33f, 0.07f, 0.8f)
    val panelTop: Color get() =
        if (light) Color(0xF4F7F7FB)
        else if (mode == 1) Color(0xFF121212) else if (mode == 2) Color(0xF0242424) else hsv(0.25f, 0.22f, 0.94f)
    val panelBottom: Color get() =
        if (light) Color(0xF4EEEEF4)
        else if (mode == 1) Color(0xFF0C0C0C) else if (mode == 2) Color(0xF0101010) else hsv(0.33f, 0.12f, 0.94f)

    // colour of the glass tint (Settings > Glass > Tint), or null when it is off
    val tintColor: Color? get() =
        if (glassTint < 0f) null else Color.hsv(glassTint.coerceIn(0f, 359.9f), 0.62f, 1f)

    private const val LOOK_VERSION = 64

    fun load(c: Context) {
        val sp = c.getSharedPreferences("vame", Context.MODE_PRIVATE)
        if (sp.getInt("lookVersion", 0) < LOOK_VERSION) {
            // first start of the new look: light + orange. Everything else stays as it was.
            sp.edit().putInt("lookVersion", LOOK_VERSION)
                .putBoolean("themeLight", true).putFloat("themeHue", 20f).putFloat("themeSat", 1f)
                .putInt("themeMode", 0).apply()
        }
        hue = sp.getFloat("themeHue", 20f)
        sat = sp.getFloat("themeSat", 1f)
        mode = sp.getInt("themeMode", 0).coerceIn(0, 2)
        light = sp.getBoolean("themeLight", true)
        glassMat = sp.getInt("glassMat", 2).coerceIn(0, 2)
        glassColour = sp.getFloat("glassColour", 0.70f).coerceIn(0f, 1f)
        glassFrost = sp.getFloat("glassFrost", 4f).coerceIn(0f, 24f)
        glassTint = sp.getFloat("glassTint", -1f)
        glassFilm = sp.getFloat("glassFilm", 0.50f).coerceIn(0f, 1f)
        glassActive = sp.getFloat("glassActive", 0.26f).coerceIn(0f, 1f)
    }

    fun save(c: Context) {
        c.getSharedPreferences("vame", Context.MODE_PRIVATE).edit()
            .putFloat("themeHue", hue).putFloat("themeSat", sat).putInt("themeMode", mode)
            .putBoolean("themeLight", light)
            .putInt("glassMat", glassMat).putFloat("glassColour", glassColour)
            .putFloat("glassFrost", glassFrost).putFloat("glassTint", glassTint)
            .putFloat("glassFilm", glassFilm).putFloat("glassActive", glassActive)
            .apply()
    }

    fun resetGlass() {
        glassMat = 2
        glassColour = 0.70f
        glassFrost = 4f
        glassTint = -1f
        glassFilm = 0.50f
        glassActive = 0.26f
    }
}
