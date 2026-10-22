package com.ameos.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Reads a playlist link from YouTube / YouTube Music / Spotify.
object Importer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isSpotify(link: String): Boolean = link.contains("spotify", ignoreCase = true)

    // shared text like "Check this out https://..." -> just the link
    fun extractLink(text: String): String {
        val m = Regex("https?://\\S+").find(text)
        return (m?.value ?: text).trim()
    }

    // one song per line; removes "1. " numbering and bullets
    fun textLines(text: String): List<String> =
        text.lines()
            .map { it.trim().replace(Regex("^(\\d+[.)]|[-*\\u2022])\\s*"), "").trim() }
            .filter { it.length >= 2 }
            .distinct()

    private val ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    // spotify.link short links jump to the real open.spotify.com link
    private fun followShort(link: String): String {
        val req = Request.Builder().url(link).header("User-Agent", ua).build()
        return client.newCall(req).execute().use { r -> r.request.url.toString() }
    }

    // any youtube / music.youtube link with list=ID -> normal playlist link
    fun youtubeUrl(link: String): String? {
        val m = Regex("[?&]list=([A-Za-z0-9_-]+)").find(link) ?: return null
        return "https://www.youtube.com/playlist?list=" + m.groupValues[1]
    }

    class SpotifyList(val name: String, val queries: List<String>)

    // Spotify public playlist -> list of "title artist" search texts
    suspend fun spotify(link: String): SpotifyList = withContext(Dispatchers.IO) {
        var full = link
        if (link.contains("spotify.link") || link.contains("spotify.app.link")) {
            full = try {
                followShort(link)
            } catch (e: Exception) {
                link
            }
        }
        val idMatch = Regex("(playlist|album)[/:]([A-Za-z0-9]+)").find(full)
            ?: throw IllegalStateException("Spotify playlist / album link sahi nahi hai")
        val kind = idMatch.groupValues[1]
        val id = idMatch.groupValues[2]
        val req = Request.Builder()
            .url("https://open.spotify.com/embed/" + kind + "/" + id)
            .header("User-Agent", ua)
            .build()
        val html = client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("Spotify HTTP " + r.code)
            r.body?.string() ?: ""
        }
        val m = Regex(
            "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>",
            RegexOption.DOT_MATCHES_ALL
        ).find(html) ?: throw IllegalStateException("Spotify ne list nahi di (playlist public honi chahiye)")
        val root = JSONObject(m.groupValues[1])
        val holder = findHolder(root)
            ?: throw IllegalStateException("Spotify list ka format samajh nahi aaya")
        val arr = holder.getJSONArray("trackList")
        val out = ArrayList<String>()
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i) ?: continue
            val title = t.optString("title").trim()
            val artist = t.optString("subtitle").replace('\u00a0', ' ').trim()
            if (title.isNotEmpty()) out.add((title + " " + artist).trim())
        }
        if (out.isEmpty()) throw IllegalStateException("Is Spotify playlist mein songs nahi mile")
        var name = holder.optString("name")
        if (name.isBlank()) name = holder.optString("title")
        if (name.isBlank()) name = "Spotify playlist"
        SpotifyList(name, out)
    }

    // finds the JSON object that contains "trackList"
    private fun findHolder(node: Any?): JSONObject? {
        if (node is JSONObject) {
            if (node.optJSONArray("trackList") != null) return node
            val keys = node.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val r = findHolder(node.opt(k))
                if (r != null) return r
            }
        } else if (node is JSONArray) {
            for (i in 0 until node.length()) {
                val r = findHolder(node.opt(i))
                if (r != null) return r
            }
        }
        return null
    }
}
