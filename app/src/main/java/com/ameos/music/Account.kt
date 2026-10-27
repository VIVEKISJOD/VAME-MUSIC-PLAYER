package com.ameos.music

import android.content.Context
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// A playlist from the person's YouTube Music library (shown in Library, not imported).
data class YtPlaylist(val id: String, val title: String, val subtitle: String, val cover: String?)

data class YtAccountInfo(val name: String, val photo: String?)

// OPTIONAL Google sign-in (unofficial, cookie login like Metrolist / InnerTune).
// The cookie is only used to READ the person's YouTube Music library.
// Songs still play through NewPipeExtractor (no ads, no account needed).
object Account {

    const val UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    const val LOGIN_URL = "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube" +
        "&continue=https%3A%2F%2Fmusic.youtube.com%2F"

    private const val ORIGIN = "https://music.youtube.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // ---------- saved login (private to this app) ----------

    private fun sp(c: Context) = c.getSharedPreferences("account", Context.MODE_PRIVATE)

    fun cookie(c: Context): String = sp(c).getString("cookie", "") ?: ""

    fun signedIn(c: Context): Boolean = cookie(c).isNotBlank()

    fun saveCookie(c: Context, v: String) = sp(c).edit().putString("cookie", v).apply()

    fun name(c: Context): String = sp(c).getString("name", "") ?: ""

    fun photo(c: Context): String? {
        val p = sp(c).getString("photo", "") ?: ""
        return if (p.isBlank()) null else p
    }

    fun saveAccount(c: Context, a: YtAccountInfo) {
        sp(c).edit().putString("name", a.name).putString("photo", a.photo ?: "").apply()
    }

    fun signOut(c: Context) {
        sp(c).edit().clear().apply()
        clearMap(c)
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
        }
    }

    // library list saved on the phone so Library shows it at once (and offline)
    fun library(c: Context): List<YtPlaylist> {
        val raw = sp(c).getString("library", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                YtPlaylist(
                    id = o.optString("i"),
                    title = o.optString("t"),
                    subtitle = o.optString("s"),
                    cover = if (o.isNull("c")) null else o.optString("c")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLibrary(c: Context, list: List<YtPlaylist>) {
        val arr = JSONArray()
        for (p in list) {
            val o = JSONObject()
            o.put("i", p.id)
            o.put("t", p.title)
            o.put("s", p.subtitle)
            o.put("c", p.cover ?: JSONObject.NULL)
            arr.put(o)
        }
        sp(c).edit().putString("library", arr.toString()).apply()
    }

    // ---------- talking to YouTube Music ----------

    private fun cookieValue(cookie: String, name: String): String? {
        for (part in cookie.split(";")) {
            val p = part.trim()
            if (p.startsWith(name + "=")) return p.substring(name.length + 1)
        }
        return null
    }

    // SAPISIDHASH: the proof YouTube wants together with the cookie
    private fun authHeader(cookie: String): String? {
        val sapisid = cookieValue(cookie, "SAPISID")
            ?: cookieValue(cookie, "__Secure-3PAPISID")
            ?: return null
        val ts = System.currentTimeMillis() / 1000
        val input = ts.toString() + " " + sapisid + " " + ORIGIN
        val md = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        val sb = StringBuilder()
        for (b in md) sb.append(String.format("%02x", b))
        return "SAPISIDHASH " + ts + "_" + sb.toString()
    }

    private fun version(): String =
        "1." + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) + ".01.00"

    private fun context(): JSONObject {
        val cl = JSONObject()
        cl.put("clientName", "WEB_REMIX")
        cl.put("clientVersion", version())
        cl.put("hl", "en")
        val ctx = JSONObject()
        ctx.put("client", cl)
        return ctx
    }

    private fun post(c: Context, endpoint: String, body: JSONObject): JSONObject {
        val ck = cookie(c)
        val auth = authHeader(ck)
            ?: throw IllegalStateException("Login adhura hai (SAPISID nahi mila). Dobara sign in karo.")
        val req = Request.Builder()
            .url(ORIGIN + "/youtubei/v1/" + endpoint + "?prettyPrint=false")
            .post(body.toString().toRequestBody(jsonType))
            .header("User-Agent", UA)
            .header("Accept", "*/*")
            .header("Cookie", ck)
            .header("Authorization", auth)
            .header("Origin", ORIGIN)
            .header("Referer", ORIGIN + "/")
            .header("X-Goog-AuthUser", "0")
            .header("X-Origin", ORIGIN)
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", version())
            .build()
        return client.newCall(req).execute().use { r ->
            val text = r.body?.string() ?: ""
            if (r.code == 401 || r.code == 403) {
                throw IllegalStateException("Login expire ho gaya. Settings > Account mein dobara sign in karo.")
            }
            if (!r.isSuccessful) throw IllegalStateException("YouTube HTTP " + r.code)
            JSONObject(text)
        }
    }

    private fun browse(c: Context, browseId: String): JSONObject {
        val b = JSONObject()
        b.put("context", context())
        b.put("browseId", browseId)
        return post(c, "browse", b)
    }

    private fun browseMore(c: Context, token: String): JSONObject {
        val b = JSONObject()
        b.put("context", context())
        b.put("continuation", token)
        return post(c, "browse", b)
    }

    // same as post(), but without a login (YouTube Music Home works for everyone)
    private fun postAnon(endpoint: String, body: JSONObject): JSONObject {
        val req = Request.Builder()
            .url(ORIGIN + "/youtubei/v1/" + endpoint + "?prettyPrint=false")
            .post(body.toString().toRequestBody(jsonType))
            .header("User-Agent", UA)
            .header("Accept", "*/*")
            .header("Origin", ORIGIN)
            .header("Referer", ORIGIN + "/")
            .header("X-Origin", ORIGIN)
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", version())
            .build()
        return client.newCall(req).execute().use { r ->
            val text = r.body?.string() ?: ""
            if (!r.isSuccessful) throw IllegalStateException("YouTube HTTP " + r.code)
            JSONObject(text)
        }
    }

    // YouTube Music Home page (first page, or the next one when a token is given).
    // Signed in = personal shelves (Listen again, From your library...), else the general ones.
    fun homeFeed(c: Context, token: String?): JSONObject {
        val b = JSONObject()
        b.put("context", context())
        if (token == null) b.put("browseId", "FEmusic_home") else b.put("continuation", token)
        return if (signedIn(c)) post(c, "browse", b) else postAnon("browse", b)
    }

    // YouTube Music search / browse without a login (artist pages)
    internal fun searchAnon(query: String): JSONObject {
        val b = JSONObject()
        b.put("context", context())
        b.put("query", query)
        return postAnon("search", b)
    }

    internal fun browseAnon(browseId: String): JSONObject {
        val b = JSONObject()
        b.put("context", context())
        b.put("browseId", browseId)
        return postAnon("browse", b)
    }

    // ---------- reading the answer (YouTube changes this format, so we search for keys) ----------

    // first value of a key anywhere inside the node (document order)
    internal fun firstValue(node: Any?, key: String): Any? {
        if (node is JSONObject) {
            if (node.has(key)) return node.opt(key)
            val keys = node.keys()
            while (keys.hasNext()) {
                val r = firstValue(node.opt(keys.next()), key)
                if (r != null) return r
            }
        } else if (node is JSONArray) {
            for (i in 0 until node.length()) {
                val r = firstValue(node.opt(i), key)
                if (r != null) return r
            }
        }
        return null
    }

    // every object stored under a key anywhere inside the node (document order)
    internal fun collect(node: Any?, key: String, out: MutableList<JSONObject>) {
        if (node is JSONObject) {
            val v = node.optJSONObject(key)
            if (v != null) out.add(v)
            val keys = node.keys()
            while (keys.hasNext()) {
                collect(node.opt(keys.next()), key, out)
            }
        } else if (node is JSONArray) {
            for (i in 0 until node.length()) collect(node.opt(i), key, out)
        }
    }

    internal fun runsText(runs: JSONArray?): String {
        if (runs == null) return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            val o = runs.optJSONObject(i)
            if (o != null) sb.append(o.optString("text"))
        }
        return sb.toString()
    }

    private fun bigCover(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (!(url.contains("googleusercontent.com") || url.contains("ggpht.com"))) return url
        return url.replace(Regex("=(?:w\\d+-h\\d+|s\\d+)[^/]*$"), "=w600-h600-l90-rj")
    }

    // the biggest picture inside an item
    internal fun coverOf(item: JSONObject): String? {
        val arr = firstValue(item, "thumbnails") as? JSONArray ?: return null
        if (arr.length() == 0) return null
        val last = arr.optJSONObject(arr.length() - 1) ?: return null
        return bigCover(last.optString("url"))
    }

    // "3:45" or "1:02:10" -> seconds
    private fun parseDuration(t: String): Long {
        var total = 0L
        for (p in t.trim().split(":")) {
            val n = p.trim().toLongOrNull() ?: return 0L
            total = total * 60 + n
        }
        return total
    }

    internal fun continuationOf(root: JSONObject): String? {
        val a = ArrayList<JSONObject>()
        collect(root, "nextContinuationData", a)
        for (o in a) {
            val t = o.optString("continuation")
            if (t.isNotBlank()) return t
        }
        val b = ArrayList<JSONObject>()
        collect(root, "continuationCommand", b)
        for (o in b) {
            val t = o.optString("token")
            if (t.isNotBlank()) return t
        }
        return null
    }

    // one row of a song list -> Song (null when the song is not playable)
    internal fun toSong(item: JSONObject): Song? {
        val videoId = firstValue(item, "videoId") as? String ?: return null
        if (videoId.isBlank()) return null
        val cols = item.optJSONArray("flexColumns") ?: return null
        fun colRuns(i: Int): JSONArray? =
            cols.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
        val title = colRuns(0)?.optJSONObject(0)?.optString("text") ?: ""
        if (title.isBlank()) return null
        // artist = the runs that link to an artist / channel page; else the first run
        val runs = colRuns(1)
        val names = ArrayList<String>()
        if (runs != null) {
            for (i in 0 until runs.length()) {
                val r = runs.optJSONObject(i) ?: continue
                val s = r.toString()
                if (s.contains("MUSIC_PAGE_TYPE_ARTIST") || s.contains("MUSIC_PAGE_TYPE_USER_CHANNEL")) {
                    val n = r.optString("text")
                    if (n.isNotBlank()) names.add(n)
                }
            }
            if (names.isEmpty()) {
                val first = runs.optJSONObject(0)?.optString("text") ?: ""
                if (first.isNotBlank()) names.add(first)
            }
        }
        val dur = item.optJSONArray("fixedColumns")
            ?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optString("text") ?: ""
        return Song(
            title = title,
            artist = names.joinToString(", "),
            url = "https://www.youtube.com/watch?v=" + videoId,
            cover = coverOf(item),
            durationSec = parseDuration(dur)
        )
    }

    private fun songsFrom(root: JSONObject, firstPage: Boolean): List<Song> {
        var scope: Any = root
        if (firstPage) {
            // only the real song list, not the "Suggestions" shelf under own playlists
            val shelves = ArrayList<JSONObject>()
            collect(root, "musicPlaylistShelfRenderer", shelves)
            if (shelves.isNotEmpty()) scope = shelves[0]
        }
        val items = ArrayList<JSONObject>()
        collect(scope, "musicResponsiveListItemRenderer", items)
        val out = ArrayList<Song>()
        for (row in items) {
            val s = toSong(row)
            if (s != null) out.add(s)
        }
        return out
    }

    // ---------- public: what the screens use ----------

    suspend fun fetchAccount(c: Context): YtAccountInfo? = withContext(Dispatchers.IO) {
        try {
            val b = JSONObject()
            b.put("context", context())
            val j = post(c, "account/account_menu", b)
            val header = firstValue(j, "activeAccountHeaderRenderer") as? JSONObject
            if (header == null) {
                null
            } else {
                val name = runsText(header.optJSONObject("accountName")?.optJSONArray("runs"))
                val photoArr = header.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
                val photo = photoArr?.optJSONObject(0)?.optString("url")
                if (name.isBlank()) null else YtAccountInfo(name, if (photo.isNullOrBlank()) null else photo)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    // the person's YouTube Music library playlists
    suspend fun libraryPlaylists(c: Context): List<YtPlaylist> = withContext(Dispatchers.IO) {
        val out = ArrayList<YtPlaylist>()
        var root = browse(c, "FEmusic_liked_playlists")
        var pages = 0
        while (true) {
            val rows = ArrayList<JSONObject>()
            collect(root, "musicTwoRowItemRenderer", rows)
            collect(root, "musicResponsiveListItemRenderer", rows)
            for (row in rows) {
                val id = row.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId") ?: ""
                if (!id.startsWith("VL") || id.length <= 2 || id == "VLSE") continue
                if (out.any { it.id == id }) continue
                var title = runsText(row.optJSONObject("title")?.optJSONArray("runs"))
                var sub = runsText(row.optJSONObject("subtitle")?.optJSONArray("runs"))
                if (title.isBlank()) {
                    val cols = row.optJSONArray("flexColumns")
                    fun col(i: Int): String = runsText(
                        cols?.optJSONObject(i)
                            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.optJSONObject("text")
                            ?.optJSONArray("runs")
                    )
                    title = col(0)
                    if (sub.isBlank()) sub = col(1)
                }
                if (title.isBlank()) continue
                out.add(YtPlaylist(id, title, sub, coverOf(row)))
            }
            pages++
            val next = continuationOf(root)
            if (next == null || pages >= 3) break
            root = browseMore(c, next)
        }
        out
    }

    // artists the person follows on YouTube Music
    suspend fun libraryArtists(c: Context): List<ArtistItem> = withContext(Dispatchers.IO) {
        val out = ArrayList<ArtistItem>()
        var root = browse(c, "FEmusic_library_corpus_artists")
        var pages = 0
        while (true) {
            val rows = ArrayList<JSONObject>()
            collect(root, "musicResponsiveListItemRenderer", rows)
            for (row in rows) {
                val cols = row.optJSONArray("flexColumns")
                val name = runsText(
                    cols?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                )
                if (name.isBlank()) continue
                if (out.any { it.name == name }) continue
                out.add(ArtistItem(name, coverOf(row)))
            }
            pages++
            val next = continuationOf(root)
            if (next == null || pages >= 3) break
            root = browseMore(c, next)
        }
        out
    }

    // songs of a library playlist (id like "VLPLxxxx"); "VLLM" = Liked music
    suspend fun playlistSongs(c: Context, browseId: String, maxItems: Int = 300): List<Song> =
        withContext(Dispatchers.IO) {
            val out = ArrayList<Song>()
            var root = browse(c, browseId)
            out.addAll(songsFrom(root, true))
            var pages = 0
            while (out.size < maxItems && pages < 12) {
                val next = continuationOf(root) ?: break
                root = browseMore(c, next)
                val more = songsFrom(root, false)
                if (more.isEmpty()) break
                out.addAll(more)
                pages++
            }
            out.distinctBy { it.url }
        }

    // ---------- saving VAME playlists to YouTube Music (write access) ----------

    fun syncOn(c: Context): Boolean = sp(c).getBoolean("syncPlaylists", true)

    fun saveSyncOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("syncPlaylists", v).apply()

    private fun mapSp(c: Context) = c.getSharedPreferences("ytmap", Context.MODE_PRIVATE)

    fun ytIdOf(c: Context, localId: String): String? {
        val v = mapSp(c).getString(localId, "") ?: ""
        return if (v.isBlank()) null else v
    }

    fun mapId(c: Context, localId: String, ytId: String) = mapSp(c).edit().putString(localId, ytId).apply()

    fun unmapId(c: Context, localId: String) = mapSp(c).edit().remove(localId).apply()

    fun mappedIds(c: Context): Set<String> {
        val out = HashSet<String>()
        for (e in mapSp(c).all.values) {
            val s = e as? String
            if (s != null && s.isNotBlank()) out.add(s)
        }
        return out
    }

    fun clearMap(c: Context) = mapSp(c).edit().clear().apply()

    fun videoId(url: String): String? {
        val i = url.indexOf("v=")
        if (i < 0) return null
        val id = url.substring(i + 2).substringBefore("&")
        return if (id.isBlank()) null else id
    }

    // makes a private playlist on the account, returns its id (blank if YouTube gave none)
    fun createPlaylist(c: Context, title: String, videoIds: List<String>): String {
        val b = JSONObject()
        b.put("context", context())
        b.put("title", title)
        b.put("description", "Made in VAME")
        b.put("privacyStatus", "PRIVATE")
        val arr = JSONArray()
        for (v in videoIds) arr.put(v)
        b.put("videoIds", arr)
        val r = post(c, "playlist/create", b)
        return r.optString("playlistId", "")
    }

    private fun editPlaylist(c: Context, ytId: String, action: JSONObject) {
        val b = JSONObject()
        b.put("context", context())
        b.put("playlistId", ytId.removePrefix("VL"))
        val actions = JSONArray()
        actions.put(action)
        b.put("actions", actions)
        post(c, "browse/edit_playlist", b)
    }

    fun addVideo(c: Context, ytId: String, videoId: String) {
        val o = JSONObject()
        o.put("action", "ACTION_ADD_VIDEO")
        o.put("addedVideoId", videoId)
        o.put("dedupeOption", "DEDUPE_OPTION_SKIP")
        editPlaylist(c, ytId, o)
    }

    fun renamePlaylist(c: Context, ytId: String, name: String) {
        val o = JSONObject()
        o.put("action", "ACTION_SET_PLAYLIST_NAME")
        o.put("playlistName", name)
        editPlaylist(c, ytId, o)
    }

    fun deletePlaylist(c: Context, ytId: String) {
        val b = JSONObject()
        b.put("context", context())
        b.put("playlistId", ytId.removePrefix("VL"))
        post(c, "playlist/delete", b)
    }

    // thumbs up on YouTube Music (used by "Move to another app" > YouTube Music)
    fun likeVideo(c: Context, videoId: String) {
        val b = JSONObject()
        b.put("context", context())
        val t = JSONObject()
        t.put("videoId", videoId)
        b.put("target", t)
        post(c, "like/like", b)
    }
}
