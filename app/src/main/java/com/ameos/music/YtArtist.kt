package com.ameos.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// The full YouTube Music artist page: picture, numbers, about text, top songs and the shelves
// (Albums, Singles & EPs, Featured on, Playlists by, Fans might also like...).
// Unofficial: if YouTube changes the layout the page just shows less (the old song list stays).
data class ArtistPage(
    val id: String,
    val pic: String?,
    val subscribers: String,
    val monthly: String,
    val about: String,
    val songs: List<Song>,
    val shelves: List<YtShelf>
)

object YtArtist {

    private fun looseMatch(k: String, key: String): Boolean =
        k.isNotEmpty() && key.isNotEmpty() && (k.contains(key) || key.contains(k))

    // artist name -> YouTube Music channel id (UC...), null when not found
    suspend fun find(name: String): String? = withContext(Dispatchers.IO) {
        val key = artistKey(name)
        val root = Account.searchAnon(name)
        var loose: String? = null
        val rows = ArrayList<JSONObject>()
        Account.collect(root, "musicResponsiveListItemRenderer", rows)
        for (r in rows) {
            val be = r.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") ?: continue
            val id = be.optString("browseId")
            if (!id.startsWith("UC")) continue
            val cols = r.optJSONArray("flexColumns") ?: continue
            val first = cols.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            val t = Account.runsText(first?.optJSONObject("text")?.optJSONArray("runs")).trim()
            val k = artistKey(t)
            if (k == key) return@withContext id
            if (loose == null && looseMatch(k, key)) loose = id
        }
        val cards = ArrayList<JSONObject>()
        Account.collect(root, "musicCardShelfRenderer", cards)
        for (c in cards) {
            val r0 = c.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0) ?: continue
            val id = r0.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId") ?: ""
            if (!id.startsWith("UC")) continue
            val k = artistKey(r0.optString("text"))
            if (k == key) return@withContext id
            if (loose == null && looseMatch(k, key)) loose = id
        }
        loose
    }

    suspend fun load(id: String, name: String): ArtistPage = withContext(Dispatchers.IO) {
        val root = Account.browseAnon(id)
        val head = (Account.firstValue(root, "musicImmersiveHeaderRenderer") as? JSONObject)
            ?: (Account.firstValue(root, "musicVisualHeaderRenderer") as? JSONObject)
        var pic: String? = null
        var subs = ""
        var monthly = ""
        var about = ""
        if (head != null) {
            pic = Account.coverOf(head)
            about = Account.runsText(head.optJSONObject("description")?.optJSONArray("runs")).trim()
            monthly = Account.runsText(head.optJSONObject("monthlyListenerCount")?.optJSONArray("runs")).trim()
            val sc = Account.firstValue(head, "subscriberCountText") as? JSONObject
            subs = Account.runsText(sc?.optJSONArray("runs")).trim()
            if (subs.isBlank() && sc != null) subs = sc.optString("simpleText").trim()
            if (subs.isNotBlank() && !subs.contains("subscriber", true)) subs = subs + " subscribers"
        }
        // top songs
        val songs = ArrayList<Song>()
        val lists = ArrayList<JSONObject>()
        Account.collect(root, "musicShelfRenderer", lists)
        if (lists.isNotEmpty()) {
            val items = lists[0].optJSONArray("contents")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val r = items.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    val s = Account.toSong(r) ?: continue
                    val a = s.artist.trim()
                    val bad = a.isBlank() || a.endsWith("plays", true) || a.endsWith("views", true)
                    songs.add(if (bad) s.copy(artist = name) else s)
                }
            }
        }
        val shelves = YtHome.shelvesFrom(root, 1)
        ArtistPage(id, pic, subs, monthly, about, songs, shelves)
    }
}
