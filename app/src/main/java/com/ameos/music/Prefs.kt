package com.ameos.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ArtistItem(val name: String, val cover: String?)

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("vame", Context.MODE_PRIVATE)

    fun title(c: Context): String = sp(c).getString("title", "Music") ?: "Music"
    fun setTitle(c: Context, v: String) = sp(c).edit().putString("title", v).apply()

    fun endless(c: Context): Boolean = sp(c).getBoolean("endless", true)
    fun setEndless(c: Context, v: Boolean) = sp(c).edit().putBoolean("endless", v).apply()

    fun officialOnly(c: Context): Boolean = sp(c).getBoolean("officialOnly", true)
    fun setOfficialOnly(c: Context, v: Boolean) = sp(c).edit().putBoolean("officialOnly", v).apply()

    fun stopOnSwipe(c: Context): Boolean = sp(c).getBoolean("stopOnSwipe", true)
    fun setStopOnSwipe(c: Context, v: Boolean) = sp(c).edit().putBoolean("stopOnSwipe", v).apply()

    fun hinglish(c: Context): Boolean = sp(c).getBoolean("hinglish", false)
    fun setHinglish(c: Context, v: Boolean) = sp(c).edit().putBoolean("hinglish", v).apply()

    fun quality(c: Context): Int = sp(c).getInt("quality", 0)
    fun setQuality(c: Context, v: Int) = sp(c).edit().putInt("quality", v).apply()

    fun speed(c: Context): Float = sp(c).getFloat("speed", 1f)
    fun setSpeed(c: Context, v: Float) = sp(c).edit().putFloat("speed", v).apply()

    fun skipSilence(c: Context): Boolean = sp(c).getBoolean("skipSilence", false)
    fun setSkipSilence(c: Context, v: Boolean) = sp(c).edit().putBoolean("skipSilence", v).apply()

    fun finishSong(c: Context): Boolean = sp(c).getBoolean("finishSong", true)
    fun setFinishSong(c: Context, v: Boolean) = sp(c).edit().putBoolean("finishSong", v).apply()

    fun keepAlive(c: Context): Boolean = sp(c).getBoolean("keepAlive", true)
    fun setKeepAlive(c: Context, v: Boolean) = sp(c).edit().putBoolean("keepAlive", v).apply()

    fun downloadQuality(c: Context): Int = sp(c).getInt("downloadQuality", 0)
    fun setDownloadQuality(c: Context, v: Int) = sp(c).edit().putInt("downloadQuality", v).apply()

    // songs saved for offline listening (the files are in Downloads.kt)
    fun downloads(c: Context): List<Song> {
        val raw = sp(c).getString("downloads", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Song(
                    title = o.optString("t"),
                    artist = o.optString("a"),
                    url = o.optString("u"),
                    cover = if (o.isNull("c")) null else o.optString("c"),
                    durationSec = o.optLong("d")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDownloads(c: Context, list: List<Song>) {
        val arr = JSONArray()
        for (s in list) {
            val o = JSONObject()
            o.put("t", s.title)
            o.put("a", s.artist)
            o.put("u", s.url)
            o.put("c", s.cover ?: JSONObject.NULL)
            o.put("d", s.durationSec)
            arr.put(o)
        }
        sp(c).edit().putString("downloads", arr.toString()).apply()
    }

    fun onboarded(c: Context): Boolean = sp(c).getBoolean("onboarded", false)
    fun setOnboarded(c: Context, v: Boolean) = sp(c).edit().putBoolean("onboarded", v).apply()

    fun genres(c: Context): Set<String> =
        (sp(c).getString("genres", "") ?: "").split("|").filter { it.isNotBlank() }.toSet()

    fun setGenres(c: Context, v: Set<String>) =
        sp(c).edit().putString("genres", v.joinToString("|")).apply()

    fun likes(c: Context): List<Song> {
        val raw = sp(c).getString("likes", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Song(
                    title = o.optString("t"),
                    artist = o.optString("a"),
                    url = o.optString("u"),
                    cover = if (o.isNull("c")) null else o.optString("c"),
                    durationSec = o.optLong("d")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLikes(c: Context, list: List<Song>) {
        val arr = JSONArray()
        for (s in list) {
            val o = JSONObject()
            o.put("t", s.title)
            o.put("a", s.artist)
            o.put("u", s.url)
            o.put("c", s.cover ?: JSONObject.NULL)
            o.put("d", s.durationSec)
            arr.put(o)
        }
        sp(c).edit().putString("likes", arr.toString()).apply()
    }

    // played history (Home > Last played), saved so it survives closing the app
    fun history(c: Context): List<Song> {
        val raw = sp(c).getString("history", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Song(
                    title = o.optString("t"),
                    artist = o.optString("a"),
                    url = o.optString("u"),
                    cover = if (o.isNull("c")) null else o.optString("c"),
                    durationSec = o.optLong("d")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHistory(c: Context, list: List<Song>) {
        val arr = JSONArray()
        for (s in list) {
            val o = JSONObject()
            o.put("t", s.title)
            o.put("a", s.artist)
            o.put("u", s.url)
            o.put("c", s.cover ?: JSONObject.NULL)
            o.put("d", s.durationSec)
            arr.put(o)
        }
        sp(c).edit().putString("history", arr.toString()).apply()
    }

    // liked artists (only when you tap the heart on an artist page)
    fun likedArtists(c: Context): List<ArtistItem> {
        val raw = sp(c).getString("likedArtists", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ArtistItem(
                    name = o.optString("n"),
                    cover = if (o.isNull("c")) null else o.optString("c")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLikedArtists(c: Context, list: List<ArtistItem>) {
        val arr = JSONArray()
        for (a in list) {
            val o = JSONObject()
            o.put("n", a.name)
            o.put("c", a.cover ?: JSONObject.NULL)
            arr.put(o)
        }
        sp(c).edit().putString("likedArtists", arr.toString()).apply()
    }

    // lyrics text size: 0 small, 1 medium, 2 large
    fun lyricsSize(c: Context): Int = sp(c).getInt("lyricsSize", 1)
    fun setLyricsSize(c: Context, v: Int) = sp(c).edit().putInt("lyricsSize", v).apply()

    // step44: lyrics text size in percent (50..160), lyrics provider, translation
    fun lyricsPct(c: Context): Int = sp(c).getInt("lyricsPct", when (lyricsSize(c)) { 0 -> 80; 2 -> 120; else -> 100 })
    fun setLyricsPct(c: Context, v: Int) = sp(c).edit().putInt("lyricsPct", v).apply()
    fun lyricsProvider(c: Context): Int = sp(c).getInt("lyricsProvider", 0)
    fun setLyricsProvider(c: Context, v: Int) = sp(c).edit().putInt("lyricsProvider", v).apply()
    fun lyricsTransOn(c: Context): Boolean = sp(c).getBoolean("lyricsTransOn", false)
    fun setLyricsTransOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("lyricsTransOn", v).apply()
    fun lyricsTransLang(c: Context): Int = sp(c).getInt("lyricsTransLang", 0)
    fun setLyricsTransLang(c: Context, v: Int) = sp(c).edit().putInt("lyricsTransLang", v).apply()

    fun keepScreenLyrics(c: Context): Boolean = sp(c).getBoolean("keepScreenLyrics", true)
    fun setKeepScreenLyrics(c: Context, v: Boolean) = sp(c).edit().putBoolean("keepScreenLyrics", v).apply()

    fun artistLatest(c: Context): Boolean = sp(c).getBoolean("artistLatest", true)
    fun setArtistLatest(c: Context, v: Boolean) = sp(c).edit().putBoolean("artistLatest", v).apply()

    fun artistAlbumCard(c: Context): Boolean = sp(c).getBoolean("artistAlbumCard", true)
    fun setArtistAlbumCard(c: Context, v: Boolean) = sp(c).edit().putBoolean("artistAlbumCard", v).apply()

    // top songs shown on the artist page: 0 = 10, 1 = 25, 2 = all
    fun artistTop(c: Context): Int = sp(c).getInt("artistTop", 1)
    fun setArtistTop(c: Context, v: Int) = sp(c).edit().putInt("artistTop", v).apply()

    fun boxRight(c: Context): Boolean = sp(c).getBoolean("boxRight", true)
    fun boxBottom(c: Context): Int = sp(c).getInt("boxBottom", 24)
    fun setBox(c: Context, right: Boolean, bottom: Int) =
        sp(c).edit().putBoolean("boxRight", right).putInt("boxBottom", bottom).apply()

    fun homeLang(c: Context): Int = sp(c).getInt("homeLang", 0)
    fun setHomeLang(c: Context, v: Int) = sp(c).edit().putInt("homeLang", v).apply()

    fun startTab(c: Context): Int = sp(c).getInt("startTab", 0)
    fun setStartTab(c: Context, v: Int) = sp(c).edit().putInt("startTab", v).apply()

    fun lyricsLeft(c: Context): Boolean = sp(c).getBoolean("lyricsLeft", false)
    fun setLyricsLeft(c: Context, v: Boolean) = sp(c).edit().putBoolean("lyricsLeft", v).apply()

    fun panelWide(c: Context): Boolean = sp(c).getBoolean("panelWide", false)
    fun setPanelWide(c: Context, v: Boolean) = sp(c).edit().putBoolean("panelWide", v).apply()

    // your name on the Library stats cards
    fun userName(c: Context): String = sp(c).getString("userName", "") ?: ""
    fun setUserName(c: Context, v: String) = sp(c).edit().putString("userName", v).apply()

    // albums saved to Library
    fun savedAlbums(c: Context): List<Album> {
        val raw = sp(c).getString("savedAlbums", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Album(
                    title = o.optString("t"),
                    artist = o.optString("a"),
                    url = o.optString("u"),
                    cover = if (o.isNull("c")) null else o.optString("c")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSavedAlbums(c: Context, list: List<Album>) {
        val arr = JSONArray()
        for (a in list) {
            val o = JSONObject()
            o.put("t", a.title)
            o.put("a", a.artist)
            o.put("u", a.url)
            o.put("c", a.cover ?: JSONObject.NULL)
            arr.put(o)
        }
        sp(c).edit().putString("savedAlbums", arr.toString()).apply()
    }
}
