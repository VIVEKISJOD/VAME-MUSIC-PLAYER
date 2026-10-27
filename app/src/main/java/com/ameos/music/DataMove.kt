package com.ameos.music

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// "Move to another app": copies playlists, liked songs, followed artists and saved albums to
// Spotify or YouTube Music, or saves them in a file any other app / transfer tool can read.
object DataMove {
    var open by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var status by mutableStateOf("")
    val log = mutableStateListOf<String>()

    var optPlaylists by mutableStateOf(true)
    var optLiked by mutableStateOf(true)
    var optArtists by mutableStateOf(true)
    var optAlbums by mutableStateOf(true)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private fun say(s: String) {
        log.add(s)
        status = s
    }

    fun cancel() {
        job?.cancel()
        busy = false
        say("Stopped")
    }

    private fun start(first: String, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        log.clear()
        say(first)
        job = scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say("Error: " + (e.message ?: e.javaClass.simpleName))
            }
            busy = false
        }
    }

    // ---------- move straight to Spotify ----------

    fun toSpotify(app: Context, vm: MainViewModel) {
        if (!Spotify.signedIn(app)) {
            status = "Connect Spotify first (Library > Spotify)"
            return
        }
        start("Moving to Spotify...") {
            if (optPlaylists) {
                val pls = vm.playlists.toList().filter { it.songs.isNotEmpty() }
                var i = 0
                for (pl in pls) {
                    i++
                    val no = i
                    say("Playlist " + no + "/" + pls.size + ": " + pl.name)
                    val uris = Spotify.matchSongs(app, pl.songs) { n, t ->
                        status = "Playlist " + no + "/" + pls.size + " - songs " + n + "/" + t
                    }
                    if (uris.isEmpty()) {
                        say("  no song found on Spotify, skipped")
                        continue
                    }
                    withContext(Dispatchers.IO) {
                        val id = Spotify.createPlaylist(app, pl.name)
                        Spotify.addTracks(app, id, uris)
                    }
                    say("  done: " + uris.size + " of " + pl.songs.size + " songs")
                }
                if (pls.isEmpty()) say("No playlists to move")
            }
            if (optLiked) {
                val songs = vm.liked.toList()
                if (songs.isEmpty()) {
                    say("No liked songs to move")
                } else {
                    val uris = Spotify.matchSongs(app, songs) { n, t -> status = "Liked songs " + n + "/" + t }
                    if (uris.isNotEmpty()) withContext(Dispatchers.IO) { Spotify.saveToLibrary(app, uris) }
                    say("Liked songs: " + uris.size + " of " + songs.size + " saved on Spotify")
                }
            }
            if (optArtists) {
                val names = vm.likedArtists.toList().map { it.name }
                if (names.isEmpty()) {
                    say("No followed artists to move")
                } else {
                    val jobs = ArrayList<() -> String?>()
                    for (n in names) {
                        val f: () -> String? = { Spotify.findArtist(app, n) }
                        jobs.add(f)
                    }
                    val uris = Spotify.matchMany(jobs) { n, t -> status = "Artists " + n + "/" + t }
                    if (uris.isNotEmpty()) withContext(Dispatchers.IO) { Spotify.saveToLibrary(app, uris) }
                    say("Artists: " + uris.size + " of " + names.size + " followed on Spotify")
                }
            }
            if (optAlbums) {
                val albums = vm.savedAlbums.toList()
                if (albums.isEmpty()) {
                    say("No saved albums to move")
                } else {
                    val jobs = ArrayList<() -> String?>()
                    for (a in albums) {
                        val f: () -> String? = { Spotify.findAlbum(app, a.title, a.artist) }
                        jobs.add(f)
                    }
                    val uris = Spotify.matchMany(jobs) { n, t -> status = "Albums " + n + "/" + t }
                    if (uris.isNotEmpty()) withContext(Dispatchers.IO) { Spotify.saveToLibrary(app, uris) }
                    say("Albums: " + uris.size + " of " + albums.size + " saved on Spotify")
                }
            }
            say("All done. Songs are matched by name, so a few may be missing.")
        }
    }

    // ---------- move straight to YouTube Music ----------

    fun toYouTube(app: Context, vm: MainViewModel) {
        if (!vm.ytSignedIn) {
            status = "Sign in with Google first (Settings > Account)"
            return
        }
        start("Moving to YouTube Music...") {
            if (optPlaylists) {
                vm.ytPushAll()
                say("Playlists are being uploaded to YouTube Music (it can take a minute).")
            }
            if (optLiked) {
                val ids = vm.liked.toList().mapNotNull { Account.videoId(it.url) }.distinct()
                var ok = 0
                for ((i, id) in ids.withIndex()) {
                    try {
                        withContext(Dispatchers.IO) { Account.likeVideo(app, id) }
                        ok++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        say("Stopped after " + ok + " songs: " + (e.message ?: "error"))
                        break
                    }
                    if (i % 5 == 0) status = "Liking songs " + (i + 1) + "/" + ids.size
                    delay(350)
                }
                say("Liked songs: " + ok + " of " + ids.size + " liked on YouTube Music")
            }
            if (optArtists || optAlbums) {
                say("YouTube Music cannot take followed artists or saved albums from here. Use the backup file for those.")
            }
            say("All done.")
        }
    }

    // ---------- backup file (JSON for VAME, CSV for other apps and transfer tools) ----------

    private fun songsJson(list: List<Song>): JSONArray {
        val arr = JSONArray()
        for (s in list) {
            val o = JSONObject()
            o.put("t", s.title)
            o.put("a", s.artist)
            o.put("u", s.url)
            o.put("c", s.cover ?: JSONObject.NULL)
            o.put("d", s.durationSec)
            arr.put(o)
        }
        return arr
    }

    private fun readSongs(a: JSONArray?): List<Song> {
        if (a == null) return emptyList()
        val out = ArrayList<Song>()
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            val u = o.optString("u")
            if (u.isBlank()) continue
            out.add(
                Song(
                    title = o.optString("t"),
                    artist = o.optString("a"),
                    url = u,
                    cover = if (o.isNull("c")) null else o.optString("c"),
                    durationSec = o.optLong("d")
                )
            )
        }
        return out
    }

    fun backupJson(vm: MainViewModel): String {
        val root = JSONObject()
        root.put("app", "VAME")
        root.put("version", 1)
        root.put("exported", System.currentTimeMillis())
        val pls = JSONArray()
        for (p in vm.playlists.toList()) {
            val o = JSONObject()
            o.put("name", p.name)
            o.put("songs", songsJson(p.songs))
            pls.put(o)
        }
        root.put("playlists", pls)
        root.put("liked", songsJson(vm.liked.toList()))
        val ars = JSONArray()
        for (a in vm.likedArtists.toList()) {
            val o = JSONObject()
            o.put("n", a.name)
            o.put("c", a.cover ?: JSONObject.NULL)
            ars.put(o)
        }
        root.put("artists", ars)
        val als = JSONArray()
        for (a in vm.savedAlbums.toList()) {
            val o = JSONObject()
            o.put("t", a.title)
            o.put("a", a.artist)
            o.put("u", a.url)
            o.put("c", a.cover ?: JSONObject.NULL)
            als.put(o)
        }
        root.put("albums", als)
        return root.toString(1)
    }

    private fun q(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""

    fun backupCsv(vm: MainViewModel): String {
        val sb = StringBuilder()
        sb.append("type,playlist,title,artist,album,url\n")
        for (p in vm.playlists.toList()) {
            for (s in p.songs) {
                sb.append("song,").append(q(p.name)).append(",").append(q(s.title)).append(",")
                    .append(q(artistName(s.artist))).append(",,").append(q(s.url)).append("\n")
            }
        }
        for (s in vm.liked.toList()) {
            sb.append("liked,").append(q("Liked songs")).append(",").append(q(s.title)).append(",")
                .append(q(artistName(s.artist))).append(",,").append(q(s.url)).append("\n")
        }
        for (a in vm.likedArtists.toList()) {
            sb.append("artist,,,").append(q(a.name)).append(",,\n")
        }
        for (a in vm.savedAlbums.toList()) {
            sb.append("album,,,").append(q(artistName(a.artist))).append(",").append(q(a.title)).append(",")
                .append(q(a.url)).append("\n")
        }
        return sb.toString()
    }

    fun writeTo(app: Context, uri: Uri, text: String) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    app.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            say(if (ok) "File saved." else "Could not save the file.")
        }
    }

    // reads a VAME backup file and adds what is missing (nothing is removed)
    fun restoreFrom(app: Context, vm: MainViewModel, uri: Uri) {
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    app.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
                } catch (e: Exception) {
                    null
                }
            }
            if (text == null) {
                say("Could not read the file.")
                return@launch
            }
            try {
                say(restore(vm, text))
            } catch (e: Exception) {
                say("Restore failed: " + (e.message ?: "this is not a VAME backup"))
            }
        }
    }

    private fun restore(vm: MainViewModel, text: String): String {
        val root = JSONObject(text)
        if (root.optString("app") != "VAME") throw IllegalStateException("this is not a VAME backup file")
        var pl = 0
        val arr = root.optJSONArray("playlists")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name")
                val songs = readSongs(o.optJSONArray("songs"))
                if (songs.isEmpty()) continue
                if (vm.playlists.any { it.name == name && it.songs.size == songs.size }) continue
                vm.addImportedPlaylist(name, songs, false)
                pl++
            }
        }
        val likedN = vm.addLikedBulk(readSongs(root.optJSONArray("liked")))
        val artists = ArrayList<ArtistItem>()
        val ar = root.optJSONArray("artists")
        if (ar != null) {
            for (i in 0 until ar.length()) {
                val o = ar.optJSONObject(i) ?: continue
                artists.add(ArtistItem(o.optString("n"), if (o.isNull("c")) null else o.optString("c")))
            }
        }
        val artistN = vm.addLikedArtistsBulk(artists)
        val albums = ArrayList<Album>()
        val al = root.optJSONArray("albums")
        if (al != null) {
            for (i in 0 until al.length()) {
                val o = al.optJSONObject(i) ?: continue
                albums.add(
                    Album(o.optString("t"), o.optString("a"), o.optString("u"), if (o.isNull("c")) null else o.optString("c"))
                )
            }
        }
        val albumN = vm.addSavedAlbumsBulk(albums)
        return "Restored: " + pl + " playlists, " + likedN + " liked songs, " + artistN + " artists, " + albumN + " albums."
    }
}

@Composable
fun SwitchAppScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext
    BackHandler { DataMove.open = false }
    val jsonSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) DataMove.writeTo(app, uri, DataMove.backupJson(vm))
    }
    val csvSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) DataMove.writeTo(app, uri, DataMove.backupCsv(vm))
    }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) DataMove.restoreFrom(app, vm, uri)
    }
    val spotifyOn = Spotify.signedIn(app)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Pal.bgTop, Pal.bgMid, Pal.bgBot), endY = 1500f))
            .blockTaps()
    ) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { DataMove.open = false }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Column {
                    Text("Move to another app", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Take your music with you", fontSize = 13.sp, color = Pal.muted)
                }
            }

            KitHeader(Icons.Filled.SwapHoriz, "What to move")
            KitCard {
                KitSwitch("Playlists", vm.playlists.size.toString() + " playlists", DataMove.optPlaylists) { DataMove.optPlaylists = it }
                KitDivider()
                KitSwitch("Liked songs", vm.liked.size.toString() + " songs", DataMove.optLiked) { DataMove.optLiked = it }
                KitDivider()
                KitSwitch("Followed artists", vm.likedArtists.size.toString() + " artists", DataMove.optArtists) { DataMove.optArtists = it }
                KitDivider()
                KitSwitch("Saved albums", vm.savedAlbums.size.toString() + " albums", DataMove.optAlbums) { DataMove.optAlbums = it }
            }

            if (DataMove.busy || DataMove.log.isNotEmpty() || DataMove.status.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                KitCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (DataMove.busy) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Pal.accent, strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            DataMove.status, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = Color.White, modifier = Modifier.weight(1f)
                        )
                    }
                    for (l in DataMove.log.takeLast(12)) {
                        Text(l, fontSize = 12.sp, color = Pal.muted, modifier = Modifier.padding(top = 3.dp))
                    }
                    if (DataMove.busy) {
                        Spacer(Modifier.height(8.dp))
                        KitButton("Stop") { DataMove.cancel() }
                    }
                }
            }

            KitHeader(Icons.Filled.Cloud, "Move straight to", "Needs the account to be connected")
            KitCard {
                Text("Spotify", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                KitNote(
                    if (spotifyOn) "Connected. Playlists are copied as private playlists, the rest is saved / followed."
                    else "Not connected yet. Open Library > Spotify to connect it first."
                )
                KitButton("Move to Spotify", primary = true, enabled = !DataMove.busy && spotifyOn) {
                    DataMove.toSpotify(app, vm)
                }
                KitButton("Open Spotify page") {
                    DataMove.open = false
                    SpotifyHub.open = true
                }
                KitDivider()
                Text("YouTube Music", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                KitNote(
                    if (vm.ytSignedIn) "Signed in. Playlists and liked songs can move. Artists and albums cannot (use the file)."
                    else "Not signed in. Sign in with Google in Settings > Account first."
                )
                KitButton("Move to YouTube Music", primary = true, enabled = !DataMove.busy && vm.ytSignedIn) {
                    DataMove.toYouTube(app, vm)
                }
            }

            KitHeader(Icons.Filled.FileUpload, "Any other app", "Save everything in a file")
            KitCard {
                KitNote(
                    "The CSV list has title, artist and link for every song, so most playlist transfer " +
                        "websites and apps can read it. The VAME backup (.json) is for restoring in VAME."
                )
                KitButton("Save list for other apps (.csv)", enabled = !DataMove.busy) {
                    csvSaver.launch("VAME_music.csv")
                }
                KitButton("Save VAME backup (.json)", enabled = !DataMove.busy) {
                    jsonSaver.launch("VAME_backup.json")
                }
                KitButton("Restore from a VAME backup", enabled = !DataMove.busy) {
                    opener.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }
            KitNote("Moving only copies. Nothing is deleted from VAME or from the other app.")
            Spacer(Modifier.height(48.dp))
        }
    }
}
