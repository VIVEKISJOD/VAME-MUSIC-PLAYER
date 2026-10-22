package com.ameos.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class LyricLine(val timeMs: Long, val text: String)
data class LyricsResult(val lines: List<LyricLine>, val synced: Boolean)

object LyricsRepo {
    private val client = OkHttpClient.Builder()
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val stamp = Regex("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]")

    suspend fun fetch(title: String, artist: String, durationSec: Long): LyricsResult =
        withContext(Dispatchers.IO) {
            val t = cleanTitle(title)
            val a = cleanArtist(artist)
            var list = search(listOf("track_name" to t, "artist_name" to a))
            if (list.isEmpty()) list = search(listOf("q" to "$a $t"))
            pick(list, durationSec)
        }

    private fun search(params: List<Pair<String, String>>): List<JSONObject> {
        val b = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
        for ((k, v) in params) b.addQueryParameter(k, v)
        val req = Request.Builder()
            .url(b.build())
            .header("User-Agent", "VAME Music (personal project)")
            .build()
        val body = client.newCall(req).execute().use { it.body?.string() ?: "[]" }
        val arr = JSONArray(body)
        val out = ArrayList<JSONObject>()
        for (i in 0 until arr.length()) out.add(arr.getJSONObject(i))
        return out
    }

    private fun str(o: JSONObject, key: String): String =
        if (o.isNull(key)) "" else o.optString(key, "")

    private fun pick(list: List<JSONObject>, durationSec: Long): LyricsResult {
        val synced = list.filter { str(it, "syncedLyrics").isNotBlank() }
        val best = synced.firstOrNull { abs(it.optDouble("duration", 0.0) - durationSec) <= 6 }
            ?: synced.firstOrNull()
        if (best != null) {
            val lines = parseLrc(str(best, "syncedLyrics"))
            if (lines.isNotEmpty()) return LyricsResult(lines, true)
        }
        val plain = list.firstOrNull { str(it, "plainLyrics").isNotBlank() }
        if (plain != null) {
            val lines = str(plain, "plainLyrics").lines().map { LyricLine(0L, it.trim()) }
            return LyricsResult(lines, false)
        }
        return LyricsResult(emptyList(), false)
    }

    private fun parseLrc(text: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        for (raw in text.lines()) {
            val matches = stamp.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val lyric = raw.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
                out.add(LyricLine(min * 60000 + sec * 1000 + ms, lyric))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    private fun cleanTitle(s: String): String =
        s.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()

    private fun cleanArtist(s: String): String =
        s.replace(" - Topic", "").replace("VEVO", "")
            .split(",", "&").first().trim()
}
