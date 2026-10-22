package com.ameos.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(
    val id: String,
    val name: String,
    val cover: String?,
    val songs: List<Song>
) {
    // custom cover if the user picked one, otherwise the cover of the first song
    fun shownCover(): String? = cover ?: songs.firstOrNull()?.cover
}

object PlaylistStore {
    private fun sp(c: Context) = c.getSharedPreferences("vame", Context.MODE_PRIVATE)

    fun load(c: Context): List<Playlist> {
        val raw = sp(c).getString("playlists", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val songs = o.getJSONArray("s")
                Playlist(
                    id = o.optString("id"),
                    name = o.optString("n"),
                    cover = if (o.isNull("c")) null else o.optString("c"),
                    songs = (0 until songs.length()).map { j ->
                        val so = songs.getJSONObject(j)
                        Song(
                            title = so.optString("t"),
                            artist = so.optString("a"),
                            url = so.optString("u"),
                            cover = if (so.isNull("c")) null else so.optString("c"),
                            durationSec = so.optLong("d")
                        )
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(c: Context, list: List<Playlist>) {
        val arr = JSONArray()
        for (p in list) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("n", p.name)
            o.put("c", p.cover ?: JSONObject.NULL)
            val songs = JSONArray()
            for (s in p.songs) {
                val so = JSONObject()
                so.put("t", s.title)
                so.put("a", s.artist)
                so.put("u", s.url)
                so.put("c", s.cover ?: JSONObject.NULL)
                so.put("d", s.durationSec)
                songs.put(so)
            }
            o.put("s", songs)
            arr.put(o)
        }
        sp(c).edit().putString("playlists", arr.toString()).apply()
    }
}
