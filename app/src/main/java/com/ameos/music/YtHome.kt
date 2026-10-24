package com.ameos.music

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// One card of a YouTube Music Home shelf.
// kind: "song" (id = watch url), "album" / "playlist" (id = browse id), "artist" (id = browse id)
data class YtCard(
    val kind: String,
    val id: String,
    val title: String,
    val sub: String,
    val artist: String,
    val cover: String?
)

data class YtShelf(val title: String, val sub: String, val cards: List<YtCard>)

data class YtPage(val shelves: List<YtShelf>, val next: String?)

// YouTube Music Home shelves (Listen again, Quick picks, Featured playlists, Trending...).
// Unofficial, like the sign-in: if YouTube changes the layout the shelves just stay empty.
object YtHome {

    private val typeWords = setOf(
        "song", "video", "single", "ep", "album", "playlist", "artist", "podcast", "episode", "profile"
    )

    // "Song • Nanku & Lambo • 2.1M views" -> "Nanku & Lambo"
    fun artistOf(sub: String): String {
        val parts = sub.split("•").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        val first = parts[0].lowercase()
        if (typeWords.contains(first)) return parts.getOrNull(1) ?: ""
        return parts[0]
    }

    suspend fun page(c: Context, token: String?): YtPage = withContext(Dispatchers.IO) {
        val root = Account.homeFeed(c, token)
        YtPage(shelvesFrom(root), Account.continuationOf(root))
    }

    private fun shelvesFrom(root: JSONObject): List<YtShelf> {
        val shelves = ArrayList<JSONObject>()
        Account.collect(root, "musicCarouselShelfRenderer", shelves)
        val out = ArrayList<YtShelf>()
        for (sh in shelves) {
            val head = sh.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            val title = Account.runsText(head?.optJSONObject("title")?.optJSONArray("runs")).trim()
            val sub = Account.runsText(head?.optJSONObject("strapline")?.optJSONArray("runs")).trim()
            val items: JSONArray = sh.optJSONArray("contents") ?: continue
            val cards = ArrayList<YtCard>()
            for (i in 0 until items.length()) {
                val el = items.optJSONObject(i) ?: continue
                val two = el.optJSONObject("musicTwoRowItemRenderer")
                val row = el.optJSONObject("musicResponsiveListItemRenderer")
                val card: YtCard? = if (two != null) twoRow(two) else if (row != null) listRow(row) else null
                if (card != null) cards.add(card)
                if (cards.size >= 20) break
            }
            if (title.isNotBlank() && cards.size >= 3) out.add(YtShelf(title, sub, cards))
        }
        return out
    }

    // big square item (album, playlist, artist, song / video)
    private fun twoRow(item: JSONObject): YtCard? {
        val title = Account.runsText(item.optJSONObject("title")?.optJSONArray("runs")).trim()
        if (title.isBlank()) return null
        val sub = Account.runsText(item.optJSONObject("subtitle")?.optJSONArray("runs")).trim()
        val cover = Account.coverOf(item)
        val nav = item.optJSONObject("navigationEndpoint") ?: return null
        val watch = nav.optJSONObject("watchEndpoint")
        val browse = nav.optJSONObject("browseEndpoint")
        val wpl = nav.optJSONObject("watchPlaylistEndpoint")
        if (watch != null) {
            val vid = watch.optString("videoId")
            if (vid.isBlank()) return null
            return YtCard(
                "song", "https://www.youtube.com/watch?v=" + vid, title, sub, artistOf(sub), cover
            )
        }
        if (browse != null) {
            val id = browse.optString("browseId")
            if (id.isBlank()) return null
            val pageType = (Account.firstValue(browse, "pageType") as? String) ?: ""
            val kind = when {
                pageType.contains("ARTIST") || pageType.contains("USER_CHANNEL") || id.startsWith("UC") -> "artist"
                pageType.contains("ALBUM") || id.startsWith("MPRE") -> "album"
                else -> "playlist"
            }
            return YtCard(kind, id, title, sub, artistOf(sub), cover)
        }
        if (wpl != null) {
            val pl = wpl.optString("playlistId")
            if (pl.isBlank()) return null
            return YtCard("playlist", "VL" + pl, title, sub, "", cover)
        }
        return null
    }

    // song row (Quick picks, Listen again)
    private fun listRow(item: JSONObject): YtCard? {
        val s = Account.toSong(item) ?: return null
        return YtCard("song", s.url, s.title, s.artist, s.artist, s.cover)
    }
}
