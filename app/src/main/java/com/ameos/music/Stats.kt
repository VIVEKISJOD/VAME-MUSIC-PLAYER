package com.ameos.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// How long each song was really listened to (for the Library stats card and Recap).
class SongStat(
    val url: String,
    var title: String,
    var artist: String,
    var cover: String?,
    var sec: Long,
    var plays: Int
)

class StatsData(
    var totalSec: Long,
    var totalPlays: Int,
    var since: Long,
    val songs: LinkedHashMap<String, SongStat>
)

data class ArtistStat(val name: String, val sec: Long, val cover: String?)

data class RecapData(
    val minutes: Long,
    val plays: Int,
    val since: Long,
    val topSongs: List<SongStat>,
    val topArtists: List<ArtistStat>
)

object StatsStore {
    private fun sp(c: Context) = c.getSharedPreferences("vame", Context.MODE_PRIVATE)

    fun load(c: Context): StatsData {
        val raw = sp(c).getString("stats", null)
        val songs = LinkedHashMap<String, SongStat>()
        var total = 0L
        var plays = 0
        var since = 0L
        if (raw != null) {
            try {
                val o = JSONObject(raw)
                total = o.optLong("ts")
                plays = o.optInt("tp")
                since = o.optLong("since")
                val arr = o.optJSONArray("songs") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    val u = s.optString("u")
                    songs[u] = SongStat(
                        u, s.optString("t"), s.optString("a"),
                        if (s.isNull("c")) null else s.optString("c"),
                        s.optLong("s"), s.optInt("p")
                    )
                }
            } catch (e: Exception) {
            }
        }
        if (since == 0L) since = System.currentTimeMillis()
        return StatsData(total, plays, since, songs)
    }

    fun save(c: Context, d: StatsData) {
        // keep the 400 most listened songs so the file stays small
        val keep = d.songs.values.sortedByDescending { it.sec }.take(400)
        val arr = JSONArray()
        for (s in keep) {
            val o = JSONObject()
            o.put("u", s.url)
            o.put("t", s.title)
            o.put("a", s.artist)
            o.put("c", s.cover ?: JSONObject.NULL)
            o.put("s", s.sec)
            o.put("p", s.plays)
            arr.put(o)
        }
        val root = JSONObject()
        root.put("ts", d.totalSec)
        root.put("tp", d.totalPlays)
        root.put("since", d.since)
        root.put("songs", arr)
        sp(c).edit().putString("stats", root.toString()).apply()
    }

    fun cleanArtist(a: String): String = a.removeSuffix(" - Topic").trim()

    fun recap(d: StatsData): RecapData {
        val songs = d.songs.values.filter { it.sec >= 5 }.sortedByDescending { it.sec }
        val byArtist = LinkedHashMap<String, Pair<Long, String?>>()
        for (s in songs) {
            val n = cleanArtist(s.artist).ifBlank { "Unknown" }
            val old = byArtist[n]
            if (old == null) byArtist[n] = Pair(s.sec, s.cover)
            else byArtist[n] = Pair(old.first + s.sec, old.second)
        }
        val artists = byArtist.entries
            .map { ArtistStat(it.key, it.value.first, it.value.second) }
            .sortedByDescending { it.sec }
        return RecapData(d.totalSec / 60, d.totalPlays, d.since, songs.take(5), artists.take(8))
    }
}
