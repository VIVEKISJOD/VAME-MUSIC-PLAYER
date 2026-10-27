package com.ameos.music

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class LyricLine(val timeMs: Long, val text: String)
data class LyricsResult(val lines: List<LyricLine>, val synced: Boolean, val source: String = "")

object LyricsRepo {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val stamp = Regex("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]")
    private val wordStamp = Regex("<\\d+:\\d+(?:[.:]\\d+)?>")

    // provider choice in Settings / the lyrics screen: 0 = Auto, 1 = LRCLIB, 2 = NetEase
    val providerNames = listOf("Auto", "LRCLIB", "NetEase", "Kugou", "Lyrics.ovh")
    val providerNotes = listOf(
        "Tries LRCLIB, NetEase, Kugou and Lyrics.ovh one by one, and keeps looking in the background while the song plays. Synced lyrics win over plain text.",
        "Open lyrics database. Line-synced lyrics for most songs.",
        "NetEase Cloud Music. Good for Hindi, Punjabi and Asian songs.",
        "Kugou Music. Another big source for Asian and Hindi songs, line-synced.",
        "Plain text lyrics only (no timing). Last chance for rare songs."
    )

    // translation languages (name, Google code)
    // index 0 = Off. Settings keep one choice for Hindi songs and one for all other songs.
    val langNames = listOf("Off", "English", "Hindi", "Spanish", "French")
    val langCodes = listOf("", "en", "hi", "es", "fr")

    // attempt 0 = the normal clean search. Higher attempts change the search words
    // (shorter title, no artist, the whole artist text) so a song that was missed can still be found.
    private fun variant(title: String, artist: String, attempt: Int): Pair<String, String> {
        val t0 = cleanTitle(title)
        val a0 = cleanArtist(artist)
        val shortT = t0.split(" - ", " | ", " ft.", " feat.", " Feat.", " ft ", " feat ")
            .first().trim().ifBlank { t0 }
        return when (attempt) {
            0 -> t0 to a0
            1 -> shortT to a0
            2 -> shortT to ""
            3 -> shortT to artist.replace(" - Topic", "").replace("VEVO", "").trim()
            4 -> t0.replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim() to a0
            else -> title.trim() to ""
        }
    }

    suspend fun fetch(
        title: String,
        artist: String,
        durationSec: Long,
        provider: Int = 0,
        exclude: Set<String> = emptySet(),
        attempt: Int = 0
    ): LyricsResult = withContext(Dispatchers.IO) {
        val (t, a) = variant(title, artist, attempt)
        val order = when (provider) {
            1 -> listOf("LRCLIB")
            2 -> listOf("NetEase")
            3 -> listOf("Kugou")
            4 -> listOf("Lyrics.ovh")
            else -> listOf("LRCLIB", "NetEase", "Kugou", "Lyrics.ovh")
        }.filter { it !in exclude }
        var plain: LyricsResult? = null
        for (name in order) {
            val r: LyricsResult? = try {
                when (name) {
                    "NetEase" -> netease(t, a, durationSec)
                    "Kugou" -> kugou(t, a, durationSec)
                    "Lyrics.ovh" -> ovh(t, a)
                    else -> lrclib(t, a, durationSec)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (r == null || r.lines.isEmpty()) continue
            if (r.synced) return@withContext r.copy(source = name)
            if (plain == null) plain = r.copy(source = name)
        }
        plain ?: LyricsResult(emptyList(), false, "")
    }

    // ---------------- LRCLIB ----------------

    private fun lrclib(t: String, a: String, durationSec: Long): LyricsResult {
        if (a.isNotBlank() && durationSec > 0L) {
            val exact = try {
                lrclibGet(t, a, durationSec)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (exact != null) return exact
        }
        var list = if (a.isBlank()) emptyList() else search(listOf("track_name" to t, "artist_name" to a))
        if (list.isEmpty()) list = search(listOf("q" to "$a $t".trim()))
        return pick(list, durationSec)
    }

    // LRCLIB exact match (title + artist + length). Null when it is not found.
    private fun lrclibGet(t: String, a: String, durationSec: Long): LyricsResult? {
        val b = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", t)
            .addQueryParameter("artist_name", a)
            .addQueryParameter("duration", durationSec.toString())
            .build()
        val req = Request.Builder().url(b).header("User-Agent", "VAME Music (personal project)").build()
        val body = client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() ?: "" else "" }
        if (body.isBlank()) return null
        val o = JSONObject(body)
        val synced = str(o, "syncedLyrics")
        if (synced.isNotBlank()) {
            val lines = parseLrc(synced)
            if (lines.isNotEmpty()) return LyricsResult(lines, true)
        }
        return null
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

    // ---------------- NetEase (unofficial public endpoints) ----------------

    private fun neteaseGet(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .header("Referer", "https://music.163.com/")
            .build()
        return client.newCall(req).execute().use { it.body?.string() ?: "" }
    }

    private fun netease(t: String, a: String, durationSec: Long): LyricsResult {
        val b = "https://music.163.com/api/search/get/web".toHttpUrl().newBuilder()
            .addQueryParameter("s", "$t $a".trim())
            .addQueryParameter("type", "1")
            .addQueryParameter("offset", "0")
            .addQueryParameter("limit", "6")
            .build()
        val body = neteaseGet(b.toString())
        if (body.isBlank()) return LyricsResult(emptyList(), false)
        val songs = JSONObject(body).optJSONObject("result")?.optJSONArray("songs")
            ?: return LyricsResult(emptyList(), false)
        val cands = ArrayList<JSONObject>()
        for (i in 0 until songs.length()) cands.add(songs.getJSONObject(i))
        // songs whose length is close to ours first
        val sorted = cands.sortedBy {
            val d = it.optLong("duration", 0L) / 1000.0
            if (d <= 0.0 || durationSec <= 0L) 999.0 else abs(d - durationSec)
        }
        var plain: LyricsResult? = null
        for (c in sorted.take(3)) {
            val id = c.optLong("id", 0L)
            if (id == 0L) continue
            val lb = neteaseGet("https://music.163.com/api/song/lyric?id=$id&lv=1&kv=1&tv=-1")
            if (lb.isBlank()) continue
            val lrc = JSONObject(lb).optJSONObject("lrc")?.optString("lyric", "") ?: ""
            if (lrc.isBlank()) continue
            val lines = parseLrc(lrc).filter { it.text.isNotBlank() }
            if (lines.isNotEmpty()) return LyricsResult(lines, true)
            if (plain == null) {
                val pl = lrc.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (pl.isNotEmpty()) plain = LyricsResult(pl.map { LyricLine(0L, it) }, false)
            }
        }
        return plain ?: LyricsResult(emptyList(), false)
    }

    // ---------------- Kugou (unofficial public endpoints) ----------------

    private fun plainGet(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .build()
        return client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() ?: "" else "" }
    }

    private fun kugou(t: String, a: String, durationSec: Long): LyricsResult {
        val empty = LyricsResult(emptyList(), false)
        val kw = "$t $a".trim()
        val sb = "https://mobilecdn.kugou.com/api/v3/search/song".toHttpUrl().newBuilder()
            .addQueryParameter("format", "json")
            .addQueryParameter("keyword", kw)
            .addQueryParameter("page", "1")
            .addQueryParameter("pagesize", "5")
            .build()
        val sBody = plainGet(sb.toString())
        if (sBody.isBlank()) return empty
        val info = JSONObject(sBody).optJSONObject("data")?.optJSONArray("info") ?: return empty
        val cands = ArrayList<JSONObject>()
        for (i in 0 until info.length()) cands.add(info.getJSONObject(i))
        val sorted = cands.sortedBy {
            val d = it.optLong("duration", 0L).toDouble()
            if (d <= 0.0 || durationSec <= 0L) 999.0 else abs(d - durationSec)
        }
        for (c in sorted.take(3)) {
            val hash = c.optString("hash", "")
            if (hash.isBlank()) continue
            val lb = "https://lyrics.kugou.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("ver", "1")
                .addQueryParameter("man", "yes")
                .addQueryParameter("client", "pc")
                .addQueryParameter("keyword", kw)
                .addQueryParameter("duration", (c.optLong("duration", 0L) * 1000L).toString())
                .addQueryParameter("hash", hash)
                .build()
            val lBody = plainGet(lb.toString())
            if (lBody.isBlank()) continue
            val arr = JSONObject(lBody).optJSONArray("candidates") ?: continue
            if (arr.length() == 0) continue
            val first = arr.getJSONObject(0)
            val id = first.optString("id", "")
            val key = first.optString("accesskey", "")
            if (id.isBlank() || key.isBlank()) continue
            val db = "https://lyrics.kugou.com/download".toHttpUrl().newBuilder()
                .addQueryParameter("ver", "1")
                .addQueryParameter("client", "pc")
                .addQueryParameter("id", id)
                .addQueryParameter("accesskey", key)
                .addQueryParameter("fmt", "lrc")
                .addQueryParameter("charset", "utf8")
                .build()
            val dBody = plainGet(db.toString())
            if (dBody.isBlank()) continue
            val content = JSONObject(dBody).optString("content", "")
            if (content.isBlank()) continue
            val text = String(android.util.Base64.decode(content, android.util.Base64.DEFAULT), Charsets.UTF_8)
                .removePrefix("\uFEFF")
            val lines = parseLrc(text).filter { it.text.isNotBlank() }
            if (lines.isNotEmpty()) return LyricsResult(lines, true)
        }
        return empty
    }

    // ---------------- Lyrics.ovh (plain text only) ----------------

    private fun ovh(t: String, a: String): LyricsResult {
        val empty = LyricsResult(emptyList(), false)
        if (a.isBlank() || t.isBlank()) return empty
        val url = "https://api.lyrics.ovh/v1".toHttpUrl().newBuilder()
            .addPathSegment(a)
            .addPathSegment(t)
            .build()
        val body = plainGet(url.toString())
        if (body.isBlank()) return empty
        val text = JSONObject(body).optString("lyrics", "").replace("\r", "").trim()
        if (text.isBlank()) return empty
        val lines = text.lines().map { LyricLine(0L, it.trim()) }
        return LyricsResult(lines, false)
    }

    // ---------------- helpers ----------------

    private fun parseLrc(text: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        for (raw in text.lines()) {
            val matches = stamp.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val lyric = raw.substring(matches.last().range.last + 1)
                .replace(wordStamp, "")
                .trim()
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

// Line-by-line translation with the public Google Translate web endpoint (no key, unofficial).
object LyricsTranslate {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // language code Google thinks the text is in ("hi", "en", ...), "" if unknown
    suspend fun detect(sample: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://translate.googleapis.com/translate_a/single".toHttpUrl().newBuilder()
                .addQueryParameter("client", "gtx")
                .addQueryParameter("sl", "auto")
                .addQueryParameter("tl", "en")
                .addQueryParameter("dt", "t")
                .addQueryParameter("q", sample)
                .build()
            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            val body = client.newCall(req).execute().use { it.body?.string() ?: "" }
            if (body.isBlank()) "" else JSONArray(body).optString(2, "")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ""
        }
    }

    private fun one(text: String, code: String): String {
        val url = "https://translate.googleapis.com/translate_a/single".toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("sl", "auto")
            .addQueryParameter("tl", code)
            .addQueryParameter("dt", "t")
            .addQueryParameter("q", text)
            .build()
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        val body = client.newCall(req).execute().use { it.body?.string() ?: "" }
        if (body.isBlank()) return ""
        val root = JSONArray(body)
        val parts = root.optJSONArray(0) ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.optJSONArray(i) ?: continue
            if (!p.isNull(0)) sb.append(p.optString(0, ""))
        }
        return sb.toString().trim()
    }

    // same size as the input, "" where a line is empty or failed
    suspend fun run(lines: List<String>, code: String): List<String> = withContext(Dispatchers.IO) {
        val gate = Semaphore(6)
        coroutineScope {
            lines.map { line ->
                async {
                    if (line.isBlank()) {
                        ""
                    } else {
                        gate.withPermit {
                            try {
                                one(line, code)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                ""
                            }
                        }
                    }
                }
            }.awaitAll()
        }
    }
}
