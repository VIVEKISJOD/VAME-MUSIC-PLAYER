package com.ameos.music

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// The look of the app (step71). `look` picks one of four: Lime (black + lime green, flat cards),
// Nova (deep purple glass), Light (soft grey + orange) or Custom (your own hue / strength with the old
// modes: 0 = colour glass, 1 = AMOLED flat, 2 = AMOLED glass). Everything reads its colours from here.
object Pal {
    var hue by mutableFloatStateOf(20f)
    var sat by mutableFloatStateOf(1f)
    var mode by mutableIntStateOf(0)

    // step71: the look. 0 = Lime (black + lime, flat cards), 1 = Nova (purple glass), 2 = Light, 3 = Custom
    // (the old dark looks: `mode` 0 colour glass, 1 AMOLED, 2 AMOLED glass, with `hue` / `sat`).
    var look by mutableIntStateOf(0)

    // old name: true = the light look. Setting it to false leaves the light look (to Lime).
    var light: Boolean
        get() = look == 2
        set(v) {
            look = if (v) 2 else if (look == 2) 0 else look
        }

    // Settings > Glass (all saved)
    var glassMat by mutableIntStateOf(2)          // 0 Fastest, 1 Balanced, 2 Quality
    var glassColour by mutableFloatStateOf(0.70f) // how much colour the glass takes (0..1)
    var glassFrost by mutableFloatStateOf(4f)     // frost in dp (0..24)
    var glassTint by mutableFloatStateOf(-1f)     // tint hue 0..360, -1 = none
    var glassFilm by mutableFloatStateOf(0.50f)   // white film over the glass (0..1)
    var glassActive by mutableFloatStateOf(0.26f) // strength of the selected tab highlight (0..1)
    var glassBlur by mutableFloatStateOf(14f)     // real blur in dp behind the dock / mini player (Android 12+)
    var glassReal by mutableStateOf(true)         // real blur on / off

    val lime: Boolean get() = look == 0
    val nova: Boolean get() = look == 1
    val amoled: Boolean get() = look == 0 || (look == 3 && mode != 0)
    // flat = solid dark cards without a glass sheen (Lime, and the Custom AMOLED look)
    val flat: Boolean get() = look == 0 || (look == 3 && mode == 1)
    val glassy: Boolean get() = !flat

    // text / icon colour on pages, and the colour for things that sit on a filled `fg` button
    val fg: Color get() = if (light) Color(0xFF16161A) else Color.White
    val onFg: Color get() = if (light) Color.White else Color(0xFF16161A)

    // text / icon colour on a filled accent button (dark on lime, white on purple / orange)
    val onAccent: Color get() = when (look) {
        0 -> Color(0xFF0C1002)
        3 -> Color(0xFF1B1315)
        else -> Color.White
    }

    private fun hsv(s: Float, v: Float, a: Float = 1f): Color =
        Color.hsv(hue.coerceIn(0f, 359.9f), (s * sat).coerceIn(0f, 1f), v.coerceIn(0f, 1f), a)

    val bgTop: Color get() = when (look) {
        0 -> Color(0xFF121312)
        1 -> Color(0xFF1D1245)
        2 -> Color(0xFFF2F2F7)
        else -> if (amoled) Color.Black else hsv(0.63f, 0.23f)
    }
    val bgMid: Color get() = when (look) {
        0 -> Color(0xFF0B0B0C)
        1 -> Color(0xFF120B2E)
        2 -> Color(0xFFF0F0F5)
        else -> if (amoled) Color.Black else hsv(0.15f, 0.07f)
    }
    val bgBot: Color get() = when (look) {
        0 -> Color(0xFF0A0B09)
        1 -> Color(0xFF0A0719)
        2 -> Color(0xFFEEEEF3)
        else -> if (amoled) Color.Black else hsv(0.15f, 0.04f)
    }
    val accent: Color get() = when (look) {
        0 -> Color(0xFFC6F135)
        1 -> Color(0xFF8E6BFF)
        2 -> hsv(0.80f, 0.84f)
        else -> hsv(0.30f, 1f)
    }
    val accentSoft: Color get() = when (look) {
        0 -> Color(0xFFDDF98A)
        1 -> Color(0xFFBBA6FF)
        2 -> hsv(0.55f, 0.95f)
        else -> hsv(0.22f, 1f)
    }
    val accentDeep: Color get() = when (look) {
        0 -> Color(0xFF55691A)
        1 -> Color(0xFF4B2FB5)
        2 -> hsv(0.70f, 0.70f)
        else -> hsv(0.80f, 0.48f)
    }
    val textDim: Color get() = when (look) {
        0 -> Color(0xFFA5A5AB)
        1 -> Color(0xFFBDB4DD)
        2 -> Color(0xFF16161A).copy(alpha = 0.62f)
        else -> hsv(0.08f, 0.86f)
    }
    val muted: Color get() = when (look) {
        0 -> Color(0xFF76767C)
        1 -> Color(0xFF8A80B4)
        2 -> Color(0xFF16161A).copy(alpha = 0.45f)
        else -> hsv(0.06f, 0.55f)
    }
    val surface: Color get() = when (look) {
        0 -> Color(0xFF171719)
        1 -> Color(0xFF1D1540)
        2 -> Color(0xFFE3E3E9)
        else -> if (mode == 1) Color(0xFF0E0E0E) else if (mode == 2) Color(0xFF111111) else hsv(0.20f, 0.17f)
    }
    val surface2: Color get() = when (look) {
        0 -> Color(0xFF222225)
        1 -> Color(0xFF2A1E5C)
        2 -> Color(0xFFD9D9E0)
        else -> if (mode == 1) Color(0xFF181818) else if (mode == 2) Color(0xFF1C1C1C) else hsv(0.15f, 0.22f)
    }
    val miniA: Color get() = hsv(0.80f, 0.36f, 0.82f)
    val miniB: Color get() = hsv(0.80f, 0.17f, 0.82f)
    val glassTop: Color get() = when (look) {
        0 -> Color(0xF21B1B1E)
        1 -> Color(0x45A58BFF)
        2 -> Color(0xCCFFFFFF)
        else -> if (mode == 1) Color(0xFF121212) else if (mode == 2) Color(0x40FFFFFF) else hsv(0.38f, 0.165f, 0.8f)
    }
    val glassBottom: Color get() = when (look) {
        0 -> Color(0xF2141416)
        1 -> Color(0x1F7C5CFF)
        2 -> Color(0x99FFFFFF)
        else -> if (mode == 1) Color(0xFF0C0C0C) else if (mode == 2) Color(0x14FFFFFF) else hsv(0.33f, 0.07f, 0.8f)
    }
    val panelTop: Color get() = when (look) {
        0 -> Color(0xF51D1D20)
        1 -> Color(0xF2302168)
        2 -> Color(0xF4F7F7FB)
        else -> if (mode == 1) Color(0xFF121212) else if (mode == 2) Color(0xF0242424) else hsv(0.25f, 0.22f, 0.94f)
    }
    val panelBottom: Color get() = when (look) {
        0 -> Color(0xF5141416)
        1 -> Color(0xF2170F3A)
        2 -> Color(0xF4EEEEF4)
        else -> if (mode == 1) Color(0xFF0C0C0C) else if (mode == 2) Color(0xF0101010) else hsv(0.33f, 0.12f, 0.94f)
    }
    // soft glow in the bottom corner of every page (Lime: lime, Nova: purple)
    val glow: Color get() = when (look) {
        0 -> Color(0x26C6F135)
        1 -> Color(0x668E6BFF)
        else -> Color.Transparent
    }

    // colour of the glass tint (Settings > Glass > Tint), or null when it is off
    val tintColor: Color? get() =
        if (glassTint < 0f) null else Color.hsv(glassTint.coerceIn(0f, 359.9f), 0.62f, 1f)

    private const val LOOK_VERSION = 71

    fun load(c: Context) {
        val sp = c.getSharedPreferences("vame", Context.MODE_PRIVATE)
        if (sp.getInt("lookVersion", 0) < LOOK_VERSION) {
            // first start of the new look (step71): Lime. Everything else stays as it was.
            sp.edit().putInt("lookVersion", LOOK_VERSION)
                .putInt("themeLook", 0).putFloat("themeHue", 20f).putFloat("themeSat", 1f)
                .putInt("themeMode", 0).apply()
        }
        hue = sp.getFloat("themeHue", 20f)
        sat = sp.getFloat("themeSat", 1f)
        mode = sp.getInt("themeMode", 0).coerceIn(0, 2)
        look = sp.getInt("themeLook", 0).coerceIn(0, 3)
        glassMat = sp.getInt("glassMat", 2).coerceIn(0, 2)
        glassColour = sp.getFloat("glassColour", 0.70f).coerceIn(0f, 1f)
        glassFrost = sp.getFloat("glassFrost", 4f).coerceIn(0f, 24f)
        glassTint = sp.getFloat("glassTint", -1f)
        glassFilm = sp.getFloat("glassFilm", 0.50f).coerceIn(0f, 1f)
        glassActive = sp.getFloat("glassActive", 0.26f).coerceIn(0f, 1f)
        glassBlur = sp.getFloat("glassBlur", 14f).coerceIn(0f, 30f)
        glassReal = sp.getBoolean("glassReal", true)
    }

    fun save(c: Context) {
        c.getSharedPreferences("vame", Context.MODE_PRIVATE).edit()
            .putFloat("themeHue", hue).putFloat("themeSat", sat).putInt("themeMode", mode)
            .putInt("themeLook", look)
            .putInt("glassMat", glassMat).putFloat("glassColour", glassColour)
            .putFloat("glassFrost", glassFrost).putFloat("glassTint", glassTint)
            .putFloat("glassFilm", glassFilm).putFloat("glassActive", glassActive)
            .putFloat("glassBlur", glassBlur).putBoolean("glassReal", glassReal)
            .apply()
    }

    fun resetGlass() {
        glassMat = 2
        glassColour = 0.70f
        glassFrost = 4f
        glassTint = -1f
        glassFilm = 0.50f
        glassActive = 0.26f
        glassBlur = 14f
        glassReal = true
    }
}
