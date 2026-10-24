package com.ameos.music

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Extra look-and-feel switches from Settings. Like Pal, these are plain state values that any
// screen can read, and they are saved in the same SharedPreferences file ("vame").
object UiOpts {
    // interface size: 0 small, 1 normal, 2 large
    var uiSize by mutableIntStateOf(1)

    // glass strength: 0 soft, 1 normal, 2 strong
    var glass by mutableIntStateOf(1)

    // fewer animations (screens appear at once)
    var reduceMotion by mutableStateOf(false)

    // names under the bottom bar icons
    var tabLabels by mutableStateOf(true)

    // tablet (sideways): small player = box (false) or pill (true), and the pill width 0..2
    var tabletPill by mutableStateOf(false)
    var pillWidth by mutableIntStateOf(1)

    // standby (phone sideways): open the big landscape player by itself while music plays
    var standby by mutableStateOf(true)
    var standbyLyrics by mutableStateOf(false)
    var standbyAwake by mutableStateOf(true)
    var standbyHideBars by mutableStateOf(true)

    // Home: which shelves are shown
    var homeQuick by mutableStateOf(true)
    var homeArtists by mutableStateOf(true)
    var homeShelves by mutableStateOf(true)
    var homeLastAlbums by mutableStateOf(true)

    // not saved: true while the player was opened by turning the phone sideways
    var standbyByRotate by mutableStateOf(false)

    val sizeScale: Float
        get() = when (uiSize) {
            0 -> 0.92f
            2 -> 1.1f
            else -> 1f
        }

    val pillDp: Int
        get() = when (pillWidth) {
            0 -> 360
            2 -> 540
            else -> 440
        }

    private fun sp(c: Context) = c.getSharedPreferences("vame", Context.MODE_PRIVATE)

    fun load(c: Context) {
        val p = sp(c)
        uiSize = p.getInt("uiSize", 1).coerceIn(0, 2)
        glass = p.getInt("glassStrength", 1).coerceIn(0, 2)
        reduceMotion = p.getBoolean("reduceMotion", false)
        tabLabels = p.getBoolean("tabLabels", true)
        tabletPill = p.getBoolean("tabletPill", false)
        pillWidth = p.getInt("pillWidth", 1).coerceIn(0, 2)
        standby = p.getBoolean("standby", true)
        standbyLyrics = p.getBoolean("standbyLyrics", false)
        standbyAwake = p.getBoolean("standbyAwake", true)
        standbyHideBars = p.getBoolean("standbyHideBars", true)
        homeQuick = p.getBoolean("homeQuick", true)
        homeArtists = p.getBoolean("homeArtists", true)
        homeShelves = p.getBoolean("homeShelves", true)
        homeLastAlbums = p.getBoolean("homeLastAlbums", true)
    }

    fun save(c: Context) {
        sp(c).edit()
            .putInt("uiSize", uiSize)
            .putInt("glassStrength", glass)
            .putBoolean("reduceMotion", reduceMotion)
            .putBoolean("tabLabels", tabLabels)
            .putBoolean("tabletPill", tabletPill)
            .putInt("pillWidth", pillWidth)
            .putBoolean("standby", standby)
            .putBoolean("standbyLyrics", standbyLyrics)
            .putBoolean("standbyAwake", standbyAwake)
            .putBoolean("standbyHideBars", standbyHideBars)
            .putBoolean("homeQuick", homeQuick)
            .putBoolean("homeArtists", homeArtists)
            .putBoolean("homeShelves", homeShelves)
            .putBoolean("homeLastAlbums", homeLastAlbums)
            .apply()
    }
}
