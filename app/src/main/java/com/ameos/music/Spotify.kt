package com.ameos.music

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

data class SpPlaylist(val id: String, val name: String, val cover: String?, val total: Int, val mine: Boolean)

data class SpTrack(val title: String, val artist: String, val uri: String, val cover: String?)

// Spotify Web API with the official login (PKCE, no password is stored, only a token).
// The person makes a free "app" on developer.spotify.com once and pastes its Client ID in VAME.
// Everything here is blocking network code: call it inside withContext(Dispatchers.IO).
object Spotify {

    const val REDIRECT = "vame://spotify-callback"

    private const val SCOPES = "playlist-read-private playlist-read-collaborative " +
        "playlist-modify-private playlist-modify-public user-library-read user-library-modify " +
        "user-follow-read user-follow-modify"

    class ApiError(val code: Int, msg: String) : Exception(msg)

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // ---------- saved login (private to this app) ----------

    private fun sp(c: Context) = c.getSharedPreferences("spotify", Context.MODE_PRIVATE)

    fun clientId(c: Context): String = (sp(c).getString("clientId", "") ?: "").trim()

    fun saveClientId(c: Context, v: String) = sp(c).edit().putString("clientId", v.trim()).apply()

    fun signedIn(c: Context): Boolean = (sp(c).getString("refresh", "") ?: "").isNotBlank()

    fun userName(c: Context): String = sp(c).getString("name", "") ?: ""

    fun userId(c: Context): String = sp(c).getString("uid", "") ?: ""

    fun userPhoto(c: Context): String? {
        val p = sp(c).getString("photo", "") ?: ""
        return if (p.isBlank()) null else p
    }

    // forgets the login, keeps the Client ID
    fun signOut(c: Context) {
        sp(c).edit()
            .remove("access").remove("refresh").remove("expires")
            .remove("name").remove("uid").remove("photo").remove("verifier")
            .apply()
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    // ---------- login (PKCE) ----------

    private fun newVerifier(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val r = SecureRandom()
        val sb = StringBuilder()
        for (i in 0 until 64) sb.append(chars[r.nextInt(chars.length)])
        return sb.toString()
    }

    private fun challenge(v: String): String {
        val md = MessageDigest.getInstance("SHA-256").digest(v.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(md, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    // the page the login WebView opens
    fun authUrl(c: Context): String {
        val v = newVerifier()
        sp(c).edit().putString("verifier", v).apply()
        return "https://accounts.spotify.com/authorize?response_type=code" +
            "&client_id=" + enc(clientId(c)) +
            "&redirect_uri=" + enc(REDIRECT) +
            "&scope=" + enc(SCOPES) +
            "&code_challenge_method=S256&code_challenge=" + challenge(v) +
            "&show_dialog=true"
    }

    private fun errText(t: String): String {
        return try {
            val o = JSONObject(t)
            val e = o.opt("error")
            if (e is JSONObject) {
                e.optString("message")
            } else {
                val d = o.optString("error_description", "")
                if (d.isNotBlank()) d else o.optString("error")
            }
        } catch (x: Exception) {
            t.take(120)
        }
    }

    private fun tokenCall(body: RequestBody): JSONObject {
        val req = Request.Builder().url("https://accounts.spotify.com/api/token").post(body).build()
        return client.newCall(req).execute().use { r ->
            val t = r.body?.string() ?: ""
            if (!r.isSuccessful) throw IllegalStateException("Spotify login HTTP " + r.code + ": " + errText(t))
            JSONObject(t)
        }
    }

    private fun saveToken(c: Context, o: JSONObject) {
        val e = sp(c).edit()
        e.putString("access", o.optString("access_token"))
        val rt = o.optString("refresh_token", "")
        if (rt.isNotBlank()) e.putString("refresh", rt)
        e.putLong("expires", System.currentTimeMillis() + (o.optLong("expires_in", 3600L) - 60L) * 1000L)
        e.apply()
    }

    // the code from the redirect -> tokens
    fun exchange(c: Context, code: String) {
        val verifier = sp(c).getString("verifier", "") ?: ""
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT)
            .add("client_id", clientId(c))
            .add("code_verifier", verifier)
            .build()
        saveToken(c, tokenCall(body))
    }

    private fun accessToken(c: Context): String {
        val exp = sp(c).getLong("expires", 0L)
        val tok = sp(c).getString("access", "") ?: ""
        if (tok.isNotBlank() && System.currentTimeMillis() < exp) return tok
        val rt = sp(c).getString("refresh", "") ?: ""
        if (rt.isBlank()) throw IllegalStateException("Spotify connect nahi hai. Dobara Connect karo.")
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", rt)
            .add("client_id", clientId(c))
            .build()
        saveToken(c, tokenCall(body))
        return sp(c).getString("access", "") ?: ""
    }

    // ---------- one API call (waits and retries when Spotify says "slow down") ----------

    private fun call(c: Context, method: String, path: String, json: JSONObject? = null): JSONObject {
        var attempt = 0
        while (true) {
            val url = if (path.startsWith("http")) path else "https://api.spotify.com/v1" + path
            val b = Request.Builder().url(url).header("Authorization", "Bearer " + accessToken(c))
            val body: RequestBody? = when {
                json != null -> json.toString().toRequestBody(jsonType)
                method == "GET" -> null
                else -> "".toRequestBody(jsonType)
            }
            b.method(method, body)
            val res = client.newCall(b.build()).execute().use { r ->
                Triple(r.code, r.header("Retry-After"), r.body?.string() ?: "")
            }
            val code = res.first
            if (code == 429 && attempt < 4) {
                attempt++
                val wait = (res.second?.toIntOrNull() ?: 2).coerceIn(1, 20)
                Thread.sleep(wait * 1000L)
                continue
            }
            if (code in 200..299) {
                if (res.third.isBlank()) return JSONObject()
                return try {
                    JSONObject(res.third)
                } catch (e: Exception) {
                    JSONObject()
                }
            }
            throw ApiError(code, "Spotify HTTP " + code + ": " + errText(res.third))
        }
    }

    fun fetchMe(c: Context) {
        val o = call(c, "GET", "/me")
        val imgs = o.optJSONArray("images")
        var photo = ""
        if (imgs != null && imgs.length() > 0) photo = imgs.optJSONObject(0)?.optString("url") ?: ""
        val id = o.optString("id")
        var name = o.optString("display_name", "")
        if (name.isBlank() || name == "null") name = id
        sp(c).edit().putString("name", name).putString("uid", id).putString("photo", photo).apply()
    }

    // ---------- reading ----------

    private fun firstImage(o: JSONObject): String? {
        val imgs = o.optJSONArray("images") ?: return null
        if (imgs.length() == 0) return null
        val u = imgs.optJSONObject(0)?.optString("url") ?: return null
        return if (u.isBlank()) null else u
    }

    fun playlists(c: Context): List<SpPlaylist> {
        val me = userId(c)
        val out = ArrayList<SpPlaylist>()
        var path: String? = "/me/playlists?limit=50"
        var guard = 0
        while (path != null && guard < 12) {
            guard++
            val cur: String = path
            val o = call(c, "GET", cur)
            val arr = o.optJSONArray("items") ?: break
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                val id = p.optString("id")
                if (id.isBlank()) continue
                // Spotify renamed "tracks" to "items" in 2026, read both
                val tr = p.optJSONObject("items") ?: p.optJSONObject("tracks")
                val total = tr?.optInt("total", 0) ?: 0
                val owner = p.optJSONObject("owner")?.optString("id") ?: ""
                val collab = p.optBoolean("collaborative", false)
                out.add(SpPlaylist(id, p.optString("name"), firstImage(p), total, owner == me || collab))
            }
            val next = o.optString("next", "")
            path = if (next.isBlank() || next == "null") null else next
        }
        return out
    }

    private fun readTracks(o: JSONObject, out: MutableList<SpTrack>) {
        val arr = o.optJSONArray("items") ?: return
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            val t = e.optJSONObject("item") ?: e.optJSONObject("track") ?: continue
            val uri = t.optString("uri")
            if (!uri.startsWith("spotify:track:")) continue
            val names = ArrayList<String>()
            val ar = t.optJSONArray("artists")
            if (ar != null) {
                for (j in 0 until ar.length()) {
                    val n = ar.optJSONObject(j)?.optString("name") ?: ""
                    if (n.isNotBlank()) names.add(n)
                }
            }
            val album = t.optJSONObject("album")
            out.add(SpTrack(t.optString("name"), names.joinToString(", "), uri, if (album != null) firstImage(album) else null))
        }
    }

    // songs of one playlist (Spotify only allows this for playlists the person owns or shares)
    fun playlistTracks(c: Context, id: String, max: Int = 500): List<SpTrack> {
        val out = ArrayList<SpTrack>()
        var path: String? = "/playlists/" + id + "/items?limit=100"
        var guard = 0
        var triedOld = false
        while (path != null && guard < 15 && out.size < max) {
            guard++
            val cur: String = path
            val o: JSONObject = try {
                call(c, "GET", cur)
            } catch (e: ApiError) {
                if (!triedOld && out.isEmpty() && (e.code == 404 || e.code == 400)) {
                    triedOld = true
                    path = "/playlists/" + id + "/tracks?limit=100"
                    continue
                }
                throw e
            }
            readTracks(o, out)
            val next = o.optString("next", "")
            path = if (next.isBlank() || next == "null") null else next
        }
        return out
    }

    // ---------- finding a song on Spotify ----------

    private val junkBracket = Regex(
        "\\s*[(\\[][^)\\]]*(official|video|audio|lyric|visuali|hd|4k|remaster|full song|mv)[^)\\]]*[)\\]]",
        RegexOption.IGNORE_CASE
    )
    private val junkWords = Regex(
        "\\b(official music video|official video|official audio|lyric video|lyrical video|full video)\\b",
        RegexOption.IGNORE_CASE
    )

    private fun cleanTitle(t: String): String {
        var s = t.replace(junkBracket, "").replace(junkWords, "")
        s = s.replace("\"", " ").replace(Regex("\\s+"), " ").trim()
        return if (s.isBlank()) t.replace("\"", " ").trim() else s
    }

    private fun cleanArtist(a: String): String {
        var s = artistName(a)
        s = s.replace(Regex("(?i)\\bvevo\\b"), "").trim()
        val cut = Regex("(?i)\\s*(,|&|\\bx\\b|\\bfeat\\.?\\b|\\bft\\.?\\b|\\band\\b)\\s*").find(s)
        if (cut != null && cut.range.first > 0) s = s.substring(0, cut.range.first)
        return s.replace("\"", " ").trim()
    }

    private fun searchUri(c: Context, q: String, type: String): String? {
        val o = call(c, "GET", "/search?type=" + type + "&limit=3&q=" + enc(q))
        val box = o.optJSONObject(type + "s") ?: return null
        val items = box.optJSONArray("items") ?: return null
        for (i in 0 until items.length()) {
            val x = items.optJSONObject(i) ?: continue
            val u = x.optString("uri")
            if (u.startsWith("spotify:")) return u
        }
        return null
    }

    // best matching Spotify track for a song (null if Spotify has nothing like it)
    fun findTrack(c: Context, title: String, artist: String): String? {
        val full = cleanTitle(title)
        val a = cleanArtist(artist)
        val tries = ArrayList<String>()
        val dash = full.indexOf(" - ")
        if (dash > 0) {
            tries.add("track:\"" + full.substring(dash + 3).trim() + "\" artist:\"" + a + "\"")
        }
        tries.add("track:\"" + full + "\"" + (if (a.isNotBlank()) " artist:\"" + a + "\"" else ""))
        tries.add((full + " " + a).trim())
        for (q in tries) {
            val u = searchUri(c, q, "track")
            if (u != null) return u
        }
        return null
    }

    fun findArtist(c: Context, name: String): String? {
        val n = cleanArtist(name)
        if (n.isBlank()) return null
        return searchUri(c, "artist:\"" + n + "\"", "artist") ?: searchUri(c, n, "artist")
    }

    fun findAlbum(c: Context, title: String, artist: String): String? {
        val t = cleanTitle(title)
        val a = cleanArtist(artist)
        return searchUri(c, "album:\"" + t + "\"" + (if (a.isNotBlank()) " artist:\"" + a + "\"" else ""), "album")
            ?: searchUri(c, (t + " " + a).trim(), "album")
    }

    // looks up many songs, 4 at the same time. Only a lost login stops it, other misses are skipped.
    suspend fun matchSongs(c: Context, songs: List<Song>, progress: (Int, Int) -> Unit): List<String> {
        val total = songs.size
        val found = arrayOfNulls<String>(total)
        var done = 0
        for (chunk in songs.withIndex().chunked(4)) {
            progress(done, total)
            coroutineScope {
                chunk.map { iv ->
                    async(Dispatchers.IO) {
                        found[iv.index] = try {
                            findTrack(c, iv.value.title, iv.value.artist)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: ApiError) {
                            if (e.code == 401 || e.code == 403) throw e
                            null
                        }
                    }
                }.awaitAll()
            }
            done += chunk.size
        }
        progress(total, total)
        return found.filterNotNull().distinct()
    }

    // same for artists / albums: a list of search jobs that each return a URI or null
    suspend fun matchMany(items: List<() -> String?>, progress: (Int, Int) -> Unit): List<String> {
        val total = items.size
        val found = arrayOfNulls<String>(total)
        var done = 0
        for (chunk in items.withIndex().chunked(4)) {
            progress(done, total)
            coroutineScope {
                chunk.map { iv ->
                    async(Dispatchers.IO) {
                        found[iv.index] = try {
                            iv.value()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: ApiError) {
                            if (e.code == 401 || e.code == 403) throw e
                            null
                        }
                    }
                }.awaitAll()
            }
            done += chunk.size
        }
        progress(total, total)
        return found.filterNotNull().distinct()
    }

    // ---------- writing ----------

    fun createPlaylist(c: Context, name: String): String {
        val b = JSONObject()
        b.put("name", name.take(100))
        b.put("public", false)
        b.put("description", "Copied from VAME")
        val o = try {
            call(c, "POST", "/me/playlists", b)
        } catch (e: ApiError) {
            if (e.code == 404 || e.code == 405) call(c, "POST", "/users/" + userId(c) + "/playlists", b) else throw e
        }
        val id = o.optString("id")
        if (id.isBlank()) throw IllegalStateException("Spotify ne playlist nahi banayi")
        return id
    }

    fun addTracks(c: Context, playlistId: String, uris: List<String>) {
        for (chunk in uris.chunked(100)) {
            val b = JSONObject()
            val arr = JSONArray()
            for (u in chunk) arr.put(u)
            b.put("uris", arr)
            try {
                call(c, "POST", "/playlists/" + playlistId + "/items", b)
            } catch (e: ApiError) {
                if (e.code == 404 || e.code == 405) {
                    call(c, "POST", "/playlists/" + playlistId + "/tracks", b)
                } else {
                    throw e
                }
            }
        }
    }

    // saves tracks / albums or follows artists (all URIs in one call must be the same kind)
    fun saveToLibrary(c: Context, uris: List<String>) {
        for (chunk in uris.chunked(40)) {
            try {
                call(c, "PUT", "/me/library?uris=" + enc(chunk.joinToString(",")))
            } catch (e: ApiError) {
                if (e.code == 400 || e.code == 403 || e.code == 404 || e.code == 405) {
                    legacySave(c, chunk)
                } else {
                    throw e
                }
            }
        }
    }

    // the older endpoints (apps made before 2026 may still use them)
    private fun legacySave(c: Context, chunk: List<String>) {
        val kind = chunk.first().split(":").getOrNull(1) ?: "track"
        val ids = chunk.map { it.substringAfterLast(":") }
        if (kind == "artist") {
            call(c, "PUT", "/me/following?type=artist&ids=" + enc(ids.joinToString(",")))
        } else {
            val b = JSONObject()
            b.put("ids", JSONArray(ids))
            call(c, "PUT", if (kind == "album") "/me/albums" else "/me/tracks", b)
        }
    }
}

// What the Spotify page shows (state) and the long jobs behind its buttons.
// Jobs live here (not in a screen) so they keep running when the phone is turned.
object SpotifyHub {
    var open by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var status by mutableStateOf("")
    var signedIn by mutableStateOf(false)
    var clientId by mutableStateOf("")
    var name by mutableStateOf("")
    var photo by mutableStateOf<String?>(null)
    var showLogin by mutableStateOf(false)
    var webUrl by mutableStateOf<String?>(null)
    val playlists = mutableStateListOf<SpPlaylist>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    fun refreshState(c: Context) {
        clientId = Spotify.clientId(c)
        signedIn = Spotify.signedIn(c)
        name = Spotify.userName(c)
        photo = Spotify.userPhoto(c)
    }

    fun saveClientId(c: Context, v: String) {
        Spotify.saveClientId(c, v)
        clientId = Spotify.clientId(c)
    }

    fun cancel() {
        job?.cancel()
        busy = false
        status = "Stopped"
    }

    // runs one job: shows the first text, then the text the job returns (or the error)
    private fun run(start: String, block: suspend () -> String) {
        if (busy) return
        busy = true
        status = start
        job = scope.launch {
            try {
                status = block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = "Spotify: " + (e.message ?: e.javaClass.simpleName)
            }
            busy = false
        }
    }

    fun disconnect(c: Context) {
        Spotify.signOut(c)
        playlists.clear()
        refreshState(c)
        status = "Disconnected from Spotify"
    }

    // called by the login WebView with the code from the redirect
    fun onCode(c: Context, code: String) {
        showLogin = false
        run("Spotify se connect ho raha hoon...") {
            withContext(Dispatchers.IO) {
                Spotify.exchange(c, code)
                Spotify.fetchMe(c)
            }
            refreshState(c)
            val list = withContext(Dispatchers.IO) { Spotify.playlists(c) }
            playlists.clear()
            playlists.addAll(list)
            "Connected as " + name + ". " + list.size + " playlists mili."
        }
    }

    fun onLoginError(msg: String) {
        showLogin = false
        status = "Spotify login: " + msg
    }

    fun loadPlaylists(c: Context) {
        run("Spotify playlists la raha hoon...") {
            val list = withContext(Dispatchers.IO) { Spotify.playlists(c) }
            playlists.clear()
            playlists.addAll(list)
            list.size.toString() + " playlists mili"
        }
    }

    // Spotify playlist -> a new VAME playlist (songs are found on YouTube by name)
    fun importToVame(c: Context, vm: MainViewModel, p: SpPlaylist) {
        run("\"" + p.name + "\" padh raha hoon...") {
            val tracks = withContext(Dispatchers.IO) { Spotify.playlistTracks(c, p.id, 500) }
            if (tracks.isEmpty()) {
                throw IllegalStateException("Is playlist ke songs nahi mile (Spotify sirf apni playlists padhne deta hai)")
            }
            val songs = findOnYoutube(tracks.map { it.title + " " + it.artist }) { n, t ->
                status = "Songs dhoondh raha hoon: " + n + "/" + t
            }
            if (songs.isEmpty()) throw IllegalStateException("Koi song YouTube par nahi mila")
            vm.addImportedPlaylist(p.name, songs)
            "Done: VAME playlist \"" + p.name + "\" (" + songs.size + " of " + tracks.size + " songs found)."
        }
    }

    // "title artist" texts -> songs from YouTube (8 at the same time)
    suspend fun findOnYoutube(queries: List<String>, progress: (Int, Int) -> Unit): List<Song> {
        val total = queries.size
        val found = arrayOfNulls<Song>(total)
        var done = 0
        for (chunk in queries.withIndex().chunked(8)) {
            progress(done, total)
            coroutineScope {
                chunk.map { iv ->
                    async {
                        found[iv.index] = try {
                            MusicRepo.search(iv.value).firstOrNull()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll()
            }
            done += chunk.size
        }
        return found.filterNotNull().distinctBy { it.url }
    }
}
