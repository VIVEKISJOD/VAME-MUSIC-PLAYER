package com.ameos.music

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
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
    var lyricsBlur by mutableStateOf(0)   // 0 = blur, 1 = light blur, 2 = no blur
    var standbyAwake by mutableStateOf(true)
    var standbyHideBars by mutableStateOf(true)
    var tabStandbyBtn by mutableStateOf(true)
    var standby12h by mutableStateOf(false)

    // Home: which shelves are shown
    var homeQuick by mutableStateOf(true)
    var homeArtists by mutableStateOf(true)
    var homeShelves by mutableStateOf(true)
    var homeLastAlbums by mutableStateOf(true)

    // step36: Home shelves from YouTube Music, soft vibration on taps, cover colour on every page
    var homeYt by mutableStateOf(true)
    var haptics by mutableStateOf(true)
    var pageTint by mutableStateOf(true)

    // step45: TV mode. tvMode 0 = Auto (on by itself on Android TV), 1 = On, 2 = Off. tv = the result.
    var tvMode by mutableIntStateOf(0)
    var tv by mutableStateOf(false)

    fun isTvDevice(c: Context): Boolean {
        val um = c.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (um != null && um.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
        val pm = c.packageManager
        return pm.hasSystemFeature("android.software.leanback") ||
            pm.hasSystemFeature("android.hardware.type.television")
    }

    fun applyTv(c: Context) {
        tv = when (tvMode) {
            1 -> true
            2 -> false
            else -> isTvDevice(c)
        }
    }

    // not saved: true while the player was opened by turning the phone sideways
    var standbyByRotate by mutableStateOf(false)

    // on a TV everything is a little bigger (10-foot viewing)
    val sizeScale: Float
        get() {
            val base = when (uiSize) {
                0 -> 0.92f
                2 -> 1.1f
                else -> 1f
            }
            return if (tv) base * 1.15f else base
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
        lyricsBlur = p.getInt("lyricsBlur", 0)
        standbyAwake = p.getBoolean("standbyAwake", true)
        standbyHideBars = p.getBoolean("standbyHideBars", true)
        tabStandbyBtn = p.getBoolean("tabStandbyBtn", true)
        standby12h = p.getBoolean("standby12h", false)
        homeQuick = p.getBoolean("homeQuick", true)
        homeArtists = p.getBoolean("homeArtists", true)
        homeShelves = p.getBoolean("homeShelves", true)
        homeLastAlbums = p.getBoolean("homeLastAlbums", true)
        homeYt = p.getBoolean("homeYt", true)
        haptics = p.getBoolean("haptics", true)
        pageTint = p.getBoolean("pageTint", true)
        tvMode = p.getInt("tvMode", 0).coerceIn(0, 2)
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
            .putInt("lyricsBlur", lyricsBlur)
            .putBoolean("standbyAwake", standbyAwake)
            .putBoolean("standbyHideBars", standbyHideBars)
            .putBoolean("tabStandbyBtn", tabStandbyBtn)
            .putBoolean("standby12h", standby12h)
            .putBoolean("homeQuick", homeQuick)
            .putBoolean("homeArtists", homeArtists)
            .putBoolean("homeShelves", homeShelves)
            .putBoolean("homeLastAlbums", homeLastAlbums)
            .putBoolean("homeYt", homeYt)
            .putBoolean("haptics", haptics)
            .putBoolean("pageTint", pageTint)
            .putInt("tvMode", tvMode)
            .apply()
    }
}
