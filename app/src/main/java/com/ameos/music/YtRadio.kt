package com.ameos.music

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// step72: the queue YouTube Music builds from one song ("radio"). Unofficial: if YouTube changes
// the format the list just comes back empty and VAME falls back to its own related songs.
object YtRadio {

    class Result(val songs: List<Song>, val token: String?)

    private fun secondsOf(t: String): Long {
        var total = 0L
        for (p in t.trim().split(":")) {
            val n = p.trim().toLongOrNull() ?: return 0L
            total = total * 60 + n
        }
        return total
    }

    private fun songOf(r: JSONObject): Song? {
        val id = r.optString("videoId")
        if (id.isBlank()) return null
        val title = Account.runsText(r.optJSONObject("title")?.optJSONArray("runs")).trim()
        if (title.isBlank()) return null
        val runs: JSONArray? = r.optJSONObject("longBylineText")?.optJSONArray("runs")
            ?: r.optJSONObject("shortBylineText")?.optJSONArray("runs")
        val names = ArrayList<String>()
        if (runs != null) {
            for (i in 0 until runs.length()) {
                val o = runs.optJSONObject(i) ?: continue
                val s = o.toString()
                if (s.contains("MUSIC_PAGE_TYPE_ARTIST") || s.contains("MUSIC_PAGE_TYPE_USER_CHANNEL")) {
                    val n = o.optString("text")
                    if (n.isNotBlank()) names.add(n)
                }
            }
            if (names.isEmpty()) {
                val first = runs.optJSONObject(0)?.optString("text") ?: ""
                if (first.isNotBlank()) names.add(first)
            }
        }
        val dur = r.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
        return Song(
            title = title,
            artist = names.joinToString(", "),
            url = "https://www.youtube.com/watch?v=" + id,
            cover = Account.coverOf(r),
            durationSec = secondsOf(dur)
        )
    }

    // videoId = first call; token = the next page of the same radio
    suspend fun fetch(c: Context, videoId: String?, token: String?): Result = withContext(Dispatchers.IO) {
        val root = Account.nextCall(c, videoId, token)
        val panels = ArrayList<JSONObject>()
        Account.collect(root, "playlistPanelRenderer", panels)
        Account.collect(root, "playlistPanelContinuation", panels)
        val scope = panels.firstOrNull() ?: return@withContext Result(emptyList(), null)
        val items = scope.optJSONArray("contents") ?: JSONArray()
        val out = ArrayList<Song>()
        val seen = HashSet<String>()
        for (i in 0 until items.length()) {
            val el = items.optJSONObject(i) ?: continue
            val r = el.optJSONObject("playlistPanelVideoRenderer")
                ?: el.optJSONObject("playlistPanelVideoWrapperRenderer")
                    ?.optJSONObject("primaryRenderer")?.optJSONObject("playlistPanelVideoRenderer")
                ?: continue
            val s = songOf(r) ?: continue
            if (seen.add(s.url)) out.add(s)
        }
        var tok: String? = null
        val radio = ArrayList<JSONObject>()
        Account.collect(scope, "nextRadioContinuationData", radio)
        for (o in radio) {
            val t = o.optString("continuation")
            if (t.isNotBlank()) {
                tok = t
                break
            }
        }
        if (tok == null) tok = Account.continuationOf(scope)
        Result(out, tok)
    }
}
