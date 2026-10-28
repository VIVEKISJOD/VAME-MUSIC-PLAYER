package com.ameos.music

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GenreShelf(val name: String, val songs: List<Song>)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val player = PlayerHolder.get(app)

    // navigation: 0 Home, 1 Search, 2 Your Type, 3 Library
    var tab by mutableIntStateOf(Prefs.startTab(app))
    var showPlayer by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showLyrics by mutableStateOf(false)
    // tablet standby: full screen standby player on top, the side player stays as it is
    var tabStandby by mutableStateOf(false)
    var showSleepDialog by mutableStateOf(false)
    var showMenu by mutableStateOf(false)

    // playlists
    val playlists = mutableStateListOf<Playlist>()
    var playlistOpen by mutableStateOf(false)
    var openPlaylistId by mutableStateOf<String?>(null)
    var showPlaylistDialog by mutableStateOf(false)
    var editPlaylistId by mutableStateOf<String?>(null)
    var pendingSong by mutableStateOf<Song?>(null)
    var addTarget by mutableStateOf<Song?>(null)
    var songMenu by mutableStateOf<Song?>(null)
    var homeFilter by mutableIntStateOf(0)

    var query by mutableStateOf("")
    var results by mutableStateOf<List<Song>>(emptyList())
    var searched by mutableStateOf(false)

    var recommended by mutableStateOf<List<Song>>(emptyList())
    var discover by mutableStateOf<List<Song>>(emptyList())
    var podcasts by mutableStateOf<List<Song>>(emptyList())
    var suggestions by mutableStateOf<List<Song>>(emptyList())
    var played = mutableStateListOf<Song>()
    val liked = mutableStateListOf<Song>()
    val likedArtists = mutableStateListOf<ArtistItem>()

    // artist page + full liked songs page
    var artistOpen by mutableStateOf(false)
    var openArtistName by mutableStateOf("")
    var artistSongs by mutableStateOf<List<Song>>(emptyList())
    var artistLoading by mutableStateOf(false)
    var likedOpen by mutableStateOf(false)

    // albums
    var recommendedAlbums by mutableStateOf<List<Album>>(emptyList())
    var albumResults by mutableStateOf<List<Album>>(emptyList())
    var artistResults by mutableStateOf<List<ArtistHit>>(emptyList())
    var albumOpen by mutableStateOf(false)
    var albumInfo by mutableStateOf<Album?>(null)
    var albumSongs by mutableStateOf<List<Song>>(emptyList())
    var albumLoading by mutableStateOf(false)

    // search page genre tiles
    var tiles by mutableStateOf<List<GenreTile>>(emptyList())

    // offline downloads
    val downloads = mutableStateListOf<Song>()
    val downloading = mutableStateMapOf<String, Float>()
    val downloadingSongs = mutableStateMapOf<String, Song>()
    var downloadsOpen by mutableStateOf(false)
    var downloadBytes by mutableStateOf(0L)
    var downloadQuality by mutableIntStateOf(Prefs.downloadQuality(app))

    // listening stats (Library card + Recap)
    private val stats = StatsStore.load(app)
    var statMinutes by mutableLongStateOf(stats.totalSec / 60)
    var statPlays by mutableIntStateOf(stats.totalPlays)
    val statSince: Long get() = stats.since
    var recapOpen by mutableStateOf(false)
    private var statTick = 0

    // playlist import
    var showImportDialog by mutableStateOf(false)
    var importBusy by mutableStateOf(false)
    var importStatus by mutableStateOf("")
    var importedId by mutableStateOf<String?>(null)

    // keep notification / island alive when paused
    var keepAlive by mutableStateOf(Prefs.keepAlive(app))

    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var current by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)

    var lyrics by mutableStateOf<List<LyricLine>>(emptyList())
    var lyricsSynced by mutableStateOf(false)
    var lyricsLoading by mutableStateOf(false)

    // step44: lyrics provider, translation, text size (declared above init on purpose: loadLyrics runs in init)
    var lyricsSource by mutableStateOf("")
    var lyricsProvider by mutableIntStateOf(Prefs.lyricsProvider(app))
    var lyricsTrans by mutableStateOf<List<String>>(emptyList())
    var lyricsTransOn by mutableStateOf(Prefs.lyricsTransOn(app))
    var lyricsTransHi by mutableIntStateOf(Prefs.lyricsTransHi(app))
    var lyricsTransEn by mutableIntStateOf(Prefs.lyricsTransEn(app))
    var lyricsTransBusy by mutableStateOf(false)
    var lyricsTransError by mutableStateOf("")
    var lyricsPct by mutableIntStateOf(Prefs.lyricsPct(app))
    var showProviderSheet by mutableStateOf(false)
    var lyricsTrying by mutableStateOf(false)
    var lyricsTryFail by mutableStateOf(false)
    // step55: true while the app keeps looking for lyrics in the background (other sources / other search words)
    var lyricsSearching by mutableStateOf(false)
    private var transJob: Job? = null

    // settings
    var title by mutableStateOf(Prefs.title(app))
    var endless by mutableStateOf(Prefs.endless(app))
    var officialOnly by mutableStateOf(Prefs.officialOnly(app))
    var hinglish by mutableStateOf(Prefs.hinglish(app))
    var stopOnSwipe by mutableStateOf(Prefs.stopOnSwipe(app))
    var quality by mutableIntStateOf(Prefs.quality(app))
    var cacheSize by mutableStateOf("")
    var speed by mutableStateOf(Prefs.speed(app))
    var skipSilence by mutableStateOf(Prefs.skipSilence(app))
    var discoverReason by mutableStateOf("")

    // audio output (phone / Bluetooth / headphones)
    var outputs by mutableStateOf<List<OutDevice>>(emptyList())
    var preferredOut by mutableStateOf<Int?>(null)
    var showOutputDialog by mutableStateOf(false)
    private var rawLyrics: List<LyricLine> = emptyList()

    // the device the sound is going to right now
    val currentOutput: OutDevice?
        get() {
            val pid = preferredOut
            if (pid != null) {
                val d = outputs.firstOrNull { it.id == pid }
                if (d != null) return d
            }
            return outputs.firstOrNull { it.kind != AudioOutput.PHONE } ?: outputs.firstOrNull()
        }

    val outputLabel: String
        get() = currentOutput?.name ?: "This phone"
    var genres by mutableStateOf(Prefs.genres(app))
    // Home genre shelves (declared up here because loadHome() runs in the first init block)
    var genreShelves by mutableStateOf<List<GenreShelf>>(emptyList())
    var homeLang by mutableIntStateOf(Prefs.homeLang(app))
    // YouTube Music Home shelves (declared up here because loadHome() runs in the first init block)
    var ytHome by mutableStateOf<List<YtShelf>>(emptyList())
    var ytHomeLoading by mutableStateOf(false)
    var ytHomeError by mutableStateOf<String?>(null)
    private var ytHomeJob: Job? = null
    // tablet (sideways): where the small floating player box sits (side + distance from the bottom)
    var boxRight by mutableStateOf(Prefs.boxRight(app))
    var boxBottomDp by mutableIntStateOf(Prefs.boxBottom(app))
    fun saveBoxPos(right: Boolean, bottomDp: Int) {
        boxRight = right
        boxBottomDp = bottomDp
        Prefs.setBox(getApplication<Application>(), right, bottomDp)
    }
    private var shelfJob: Job? = null
    var showOnboarding by mutableStateOf(!Prefs.onboarded(app))

    // playback modes: repeat 0 off, 1 all, 2 one
    var repeat by mutableIntStateOf(0)
    var sleepLeftSec by mutableIntStateOf(0)
    var sleepAfterSong by mutableStateOf(false)
    var timesLeft by mutableIntStateOf(0)
    var finishSong by mutableStateOf(Prefs.finishSong(app))

    var queue by mutableStateOf<List<Song>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set

    private val cache = HashMap<String, Resolved>()
    // audio links worked out ahead of time (only the last few are kept)
    private val fastCache = LinkedHashMap<String, FastAudio>()
    private var startJob: Job? = null
    private var lyricsJob: Job? = null
    private var sleepJob: Job? = null
    private var artistJob: Job? = null
    private var albumJob: Job? = null

    init {
        MusicRepo.officialOnly = officialOnly
        MusicRepo.quality = quality
        player.setPlaybackParameters(PlaybackParameters(speed))
        player.skipSilenceEnabled = skipSilence
        AudioOutput.watch(app) { refreshOutputs() }
        refreshOutputs()
        liked.addAll(Prefs.likes(app))
        likedArtists.addAll(Prefs.likedArtists(app))
        downloads.addAll(Prefs.downloads(app))
        played.addAll(Prefs.history(app))
        // reopen the app on the last song (paused); tapping play starts it
        val lastSong = played.firstOrNull()
        if (lastSong != null && player.mediaItemCount == 0) {
            current = lastSong
            queue = listOf(lastSong)
            index = 0
            loadLyrics(lastSong)
        }
        playlists.addAll(PlaylistStore.load(app))
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) StatsStore.save(getApplication<Application>(), stats)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onEnded()
            }

            override fun onPlayerError(e: PlaybackException) {
                error = "Play nahi hua: " + (e.message ?: e.errorCodeName)
            }
        })
        isPlaying = player.isPlaying
        // every second of real playing is added to the listening stats
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = current
                if (s != null && isPlaying) addListen(s)
            }
        }
        PlaybackBridge.onNext = { next() }
        PlaybackBridge.onPrev = { prev() }
        PlaybackBridge.onToggle = { toggle() }
        loadHome()
    }

    private fun statFor(s: Song): SongStat {
        val e = stats.songs.getOrPut(s.url) { SongStat(s.url, s.title, s.artist, s.cover, 0L, 0) }
        e.title = s.title
        e.artist = s.artist
        if (s.cover != null) e.cover = s.cover
        return e
    }

    private fun addListen(s: Song) {
        statFor(s).sec += 1
        stats.totalSec += 1
        if (stats.totalSec % 60L == 0L) statMinutes = stats.totalSec / 60
        statTick++
        if (statTick >= 20) {
            statTick = 0
            StatsStore.save(getApplication<Application>(), stats)
        }
    }

    private fun countPlay(s: Song) {
        statFor(s).plays += 1
        stats.totalPlays += 1
        statPlays = stats.totalPlays
        StatsStore.save(getApplication<Application>(), stats)
    }

    fun recap(): RecapData = StatsStore.recap(stats)

    override fun onCleared() {
        Connect.vm = null
        connectJob?.cancel()
        StatsStore.save(getApplication<Application>(), stats)
        PlaybackBridge.onNext = null
        PlaybackBridge.onPrev = null
        PlaybackBridge.onToggle = null
        super.onCleared()
    }

    // ---------- loading lists ----------

    private fun loadAlbums() {
        viewModelScope.launch {
            try {
                val g = genres.toList().shuffled().take(2)
                val qs = if (g.isEmpty()) listOf("new albums 2026") else g.map { it + " albums" }
                val all = ArrayList<Album>()
                for (q in qs) {
                    try {
                        all.addAll(MusicRepo.searchAlbums(q))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                    }
                }
                recommendedAlbums = all.distinctBy { it.url }.shuffled().take(4)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
    }

    fun loadTiles() {
        if (tiles.isNotEmpty()) return
        viewModelScope.launch {
            val names = if (genres.size >= 2) genres.toList().take(4)
            else listOf("Rap", "Alternative/Indie", "Pop", "Hip-Hop")
            val out = ArrayList<GenreTile>()
            for (n in names) {
                try {
                    val songs = MusicRepo.search(n + " music")
                    val covers = songs.mapNotNull { it.cover }.distinct().take(4)
                    val artists = songs.map { artistName(it.artist) }.distinct().take(3)
                    out.add(GenreTile(n, covers, artists.joinToString(", ")))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    out.add(GenreTile(n, emptyList(), ""))
                }
            }
            tiles = out
        }
    }

    fun loadHome() {
        loadAlbums()
        loadGenreShelves()
        loadYtHome()
        viewModelScope.launch {
            try {
                val g = genres.toList().shuffled().take(3)
                var result: List<Song> = emptyList()
                if (g.isNotEmpty()) {
                    val lists = g.map {
                        try {
                            MusicRepo.search(it + " songs 2026")
                        } catch (e: Exception) {
                            emptyList<Song>()
                        }
                    }
                    val out = mutableListOf<Song>()
                    val max = lists.maxOf { it.size }
                    for (i in 0 until max) {
                        for (l in lists) {
                            val x = l.getOrNull(i)
                            if (x != null) out.add(x)
                        }
                    }
                    result = out.distinctBy { it.url }
                }
                if (result.isEmpty()) result = MusicRepo.search("top songs 2026")
                recommended = result
            } catch (e: Exception) {
                error = "Home load fail: " + (e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun loadDiscover() {
        viewModelScope.launch {
            loading = true
            try {
                val qs = ArrayList<String>()
                val names = ArrayList<String>()
                val tops = played.map { it.artist.removeSuffix(" - Topic") }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }
                    .map { it.key }.take(2)
                for (t in tops) {
                    qs.add(t + " songs")
                    names.add(t)
                }
                for (g in genres.toList().shuffled().take(2)) {
                    qs.add(g + " new music this week")
                    names.add(g)
                }
                if (qs.isEmpty()) qs.add("new music this week")
                val all = ArrayList<Song>()
                for (q in qs) {
                    try {
                        all.addAll(MusicRepo.search(q))
                    } catch (e: Exception) {
                    }
                }
                val heard = played.map { it.url }.toSet()
                val seen = HashSet<String>()
                val fresh = all.filter { it.url !in heard && seen.add(it.url) }.shuffled()
                discover = if (fresh.isNotEmpty()) fresh else all.distinctBy { it.url }
                discoverReason = if (names.isEmpty()) "New music this week"
                else "Based on " + names.joinToString(", ")
            } catch (e: Exception) {
                error = "Discover fail: " + (e.message ?: e.javaClass.simpleName)
            }
            loading = false
        }
    }

    fun finishOnboarding(picked: Set<String>) {
        genres = picked
        Prefs.setGenres(getApplication<Application>(), picked)
        Prefs.setOnboarded(getApplication<Application>(), true)
        showOnboarding = false
        discover = emptyList()
        loadHome()
    }

    fun loadPodcasts() {
        viewModelScope.launch {
            try {
                podcasts = MusicRepo.search("podcast episodes")
            } catch (e: Exception) {
                error = "Podcast load fail: " + (e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun pickFilter(i: Int) {
        homeFilter = i
        if (i == 2 && podcasts.isEmpty()) loadPodcasts()
        if (i == 3 && ytSignedIn && ytPlaylists.isEmpty()) refreshYt()
    }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            loading = true
            error = null
            searched = true
            try {
                results = MusicRepo.search(q)
                artistResults = try {
                    MusicRepo.searchArtists(q)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
                albumResults = try {
                    MusicRepo.searchAlbums(q).take(10)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
                if (results.isEmpty() && artistResults.isEmpty() && albumResults.isEmpty()) error = "Kuch nahi mila"
            } catch (e: Exception) {
                error = "Search fail: " + (e.message ?: e.javaClass.simpleName)
            }
            loading = false
        }
    }

    fun searchFor(text: String) {
        query = text
        search()
    }

    // back from search results to the Explore tiles
    fun clearSearch() {
        searched = false
        query = ""
        results = emptyList()
        albumResults = emptyList()
        artistResults = emptyList()
        error = null
    }

    // ---------- settings ----------

    fun updateTitle(t: String) {
        title = t
        Prefs.setTitle(getApplication<Application>(), t)
    }

    fun setEndlessOn(on: Boolean) {
        endless = on
        Prefs.setEndless(getApplication<Application>(), on)
    }

    fun setOfficialOnlyOn(on: Boolean) {
        officialOnly = on
        MusicRepo.officialOnly = on
        Prefs.setOfficialOnly(getApplication<Application>(), on)
        loadHome()
    }

    fun setHinglishOn(on: Boolean) {
        hinglish = on
        Prefs.setHinglish(getApplication<Application>(), on)
        lyrics = styleLyrics(rawLyrics)
    }

    private fun styleLyrics(l: List<LyricLine>): List<LyricLine> =
        if (hinglish) l.map { it.copy(text = Hinglish.convert(it.text)) } else l

    fun updateStopOnSwipe(on: Boolean) {
        stopOnSwipe = on
        Prefs.setStopOnSwipe(getApplication<Application>(), on)
    }

    fun updateQuality(q: Int) {
        quality = q
        MusicRepo.quality = q
        Prefs.setQuality(getApplication<Application>(), q)
        cache.clear()
        prefetchNext()
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                getApplication<Application>().cacheDir.walkTopDown()
                    .filter { it.isFile }.sumOf { it.length() }
            }
            cacheSize = if (bytes >= 1048576L) {
                String.format("%.1f MB", bytes / 1048576.0)
            } else {
                (bytes / 1024).toString() + " KB"
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getApplication<Application>().cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            cache.clear()
            refreshCacheSize()
        }
    }

    fun clearHistory() {
        played.clear()
        Prefs.saveHistory(getApplication<Application>(), emptyList())
        discover = emptyList()
        discoverReason = ""
    }

    // ---------- audio output ----------

    fun refreshOutputs() {
        val old = outputs.map { it.id }.toSet()
        val fresh = AudioOutput.list(getApplication<Application>())
        outputs = fresh
        val pid = preferredOut
        val newExternal = old.isNotEmpty() && fresh.any { it.kind != AudioOutput.PHONE && it.id !in old }
        val gone = pid != null && fresh.none { it.id == pid }
        if (pid != null && (newExternal || gone)) {
            preferredOut = null
            player.setPreferredAudioDevice(null)
        }
    }

    fun chooseOutput(d: OutDevice) {
        preferredOut = d.id
        player.setPreferredAudioDevice(d.info)
    }

    // ---------- queue / playback ----------

    fun play(song: Song, list: List<Song> = listOf(song)) {
        queue = list
        index = list.indexOf(song).coerceAtLeast(0)
        start(song)
    }

    fun playFromQueue(i: Int) {
        if (i !in queue.indices) return
        index = i
        start(queue[i])
    }

    fun next() {
        if (queue.isEmpty()) return
        index = (index + 1) % queue.size
        start(queue[index])
    }

    fun prev() {
        if (queue.isEmpty()) return
        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }
        index = if (index - 1 < 0) queue.size - 1 else index - 1
        start(queue[index])
    }

    fun cycleRepeat() {
        repeat = (repeat + 1) % 3
    }

    fun shuffleQueue() {
        val cur = current ?: return
        if (queue.size < 2) return
        queue = listOf(cur) + queue.filter { it.url != cur.url }.shuffled()
        index = 0
        prefetchNext()
    }

    // shuffles only the songs after the current one (current + already played stay)
    fun shuffleUpNext(): Boolean {
        if (queue.size < index + 3) return false
        val head = queue.subList(0, index + 1).toList()
        val tail = queue.subList(index + 1, queue.size).shuffled()
        queue = head + tail
        prefetchNext()
        return true
    }

    fun playNext(song: Song) {
        if (queue.isEmpty() || current == null) {
            play(song)
            return
        }
        val list = queue.toMutableList()
        var idx = index
        for (i in list.size - 1 downTo 0) {
            if (i != idx && list[i].url == song.url) {
                list.removeAt(i)
                if (i < idx) idx--
            }
        }
        list.add((idx + 1).coerceAtMost(list.size), song)
        queue = list
        index = idx
        prefetchNext()
    }

    fun moveInQueue(from: Int, to: Int) {
        if (from == to || from !in queue.indices || to !in queue.indices) return
        val list = queue.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        queue = list
        index = when {
            from == index -> to
            from < index && to >= index -> index - 1
            from > index && to <= index -> index + 1
            else -> index
        }
    }

    fun reorderDone() {
        prefetchNext()
    }

    fun addToQueue(song: Song) {
        if (queue.isEmpty() || current == null) {
            play(song)
            return
        }
        queue = queue + song
    }

    fun startRadio() {
        val cur = current ?: return
        viewModelScope.launch {
            try {
                val r = resolveFor(cur)
                val more = r.related.filter { it.url != cur.url }.take(25)
                if (more.isNotEmpty()) {
                    queue = listOf(cur) + more
                    index = 0
                    prefetchNext()
                }
            } catch (e: Exception) {
            }
        }
    }

    // ---------- playlists ----------

    private fun savePlaylists() {
        PlaylistStore.save(getApplication<Application>(), playlists.toList())
    }

    fun openPlaylist(id: String) {
        artistOnTop = false
        openPlaylistId = id
        playlistOpen = true
    }

    fun closePlaylist() {
        playlistOpen = false
    }

    fun createPlaylist(name: String, coverUri: Uri?, firstSong: Song?) {
        val id = System.currentTimeMillis().toString()
        val clean = name.trim().ifBlank { "My playlist" }
        playlists.add(Playlist(id, clean, null, if (firstSong != null) listOf(firstSong) else emptyList()))
        savePlaylists()
        if (coverUri != null) setPlaylistCover(id, coverUri)
        ytPush(id)
    }

    fun editPlaylist(id: String, name: String, coverUri: Uri?) {
        val i = playlists.indexOfFirst { it.id == id }
        if (i < 0) return
        val old = playlists[i]
        playlists[i] = old.copy(name = name.trim().ifBlank { old.name })
        savePlaylists()
        if (coverUri != null) setPlaylistCover(id, coverUri)
        ytRename(id)
    }

    private fun setPlaylistCover(id: String, uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                try {
                    val app = getApplication<Application>()
                    val f = File(app.filesDir, "pl_" + id + "_" + System.currentTimeMillis() + ".jpg")
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        f.outputStream().use { out -> input.copyTo(out) }
                    }
                    if (f.length() > 0) Uri.fromFile(f).toString() else null
                } catch (e: Exception) {
                    null
                }
            }
            if (path != null) {
                val i = playlists.indexOfFirst { it.id == id }
                if (i >= 0) {
                    val oldCover = playlists[i].cover
                    playlists[i] = playlists[i].copy(cover = path)
                    savePlaylists()
                    deleteCoverFile(oldCover)
                }
            }
        }
    }

    private fun deleteCoverFile(cover: String?) {
        if (cover == null) return
        try {
            val p = Uri.parse(cover).path
            if (p != null) File(p).delete()
        } catch (e: Exception) {
        }
    }

    fun deletePlaylist(id: String) {
        val pl = playlists.firstOrNull { it.id == id }
        playlists.removeAll { it.id == id }
        savePlaylists()
        deleteCoverFile(pl?.cover)
        ytDelete(id)
    }

    fun addToPlaylist(id: String, song: Song): Boolean {
        val i = playlists.indexOfFirst { it.id == id }
        if (i < 0) return false
        val pl = playlists[i]
        if (pl.songs.any { it.url == song.url }) return false
        playlists[i] = pl.copy(songs = pl.songs + song)
        savePlaylists()
        ytAdd(id, song)
        return true
    }

    fun removeFromPlaylist(id: String, song: Song) {
        val i = playlists.indexOfFirst { it.id == id }
        if (i < 0) return
        val pl = playlists[i]
        playlists[i] = pl.copy(songs = pl.songs.filter { it.url != song.url })
        savePlaylists()
    }

    fun playPlaylist(id: String, shuffle: Boolean) {
        val pl = playlists.firstOrNull { it.id == id } ?: return
        if (pl.songs.isEmpty()) return
        val list = if (shuffle) pl.songs.shuffled() else pl.songs
        play(list.first(), list)
    }

    fun setSleep(minutes: Int, afterSong: Boolean = false) {
        sleepJob?.cancel()
        sleepAfterSong = afterSong
        sleepLeftSec = minutes * 60
        if (minutes <= 0) return
        sleepJob = viewModelScope.launch {
            while (sleepLeftSec > 0) {
                delay(1000)
                sleepLeftSec--
            }
            if (finishSong && player.isPlaying) {
                sleepAfterSong = true
            } else {
                player.pause()
            }
        }
    }

    fun setFinishSongOn(on: Boolean) {
        finishSong = on
        Prefs.setFinishSong(getApplication<Application>(), on)
    }

    // play the current song n times from the start, then stop
    fun playTimes(n: Int) {
        if (current == null) return
        timesLeft = n.coerceAtLeast(1)
        player.seekTo(0)
        player.play()
    }

    private fun onEnded() {
        if (sleepAfterSong) {
            sleepAfterSong = false
            timesLeft = 0
            return
        }
        if (timesLeft > 0) {
            timesLeft--
            if (timesLeft > 0) {
                player.seekTo(0)
                player.play()
            }
            return
        }
        if (repeat == 2) {
            player.seekTo(0)
            player.play()
            return
        }
        val last = index >= queue.size - 1
        if (last && repeat == 0 && !endless) return
        next()
    }

    private suspend fun resolveFor(song: Song): Resolved {
        val cached = cache[song.url]
        if (cached != null) return cached
        val fresh = MusicRepo.resolve(song.url, MusicRepo.quality, true, song.artist)
        cache[song.url] = fresh
        return fresh
    }

    private suspend fun resolveAudioFor(song: Song): FastAudio {
        val hit = fastCache[song.url]
        if (hit != null) return hit
        val fresh = MusicRepo.resolveAudio(song.url, MusicRepo.quality)
        fastCache[song.url] = fresh
        while (fastCache.size > 6) fastCache.remove(fastCache.keys.first())
        return fresh
    }

    private fun prefetchNext() {
        if (queue.size < 2) return
        val n = queue[(index + 1) % queue.size]
        if (fastCache.containsKey(n.url) || cache.containsKey(n.url)) return
        viewModelScope.launch {
            try {
                resolveAudioFor(n)
            } catch (e: Exception) {
            }
        }
    }

    private fun loadLyrics(song: Song) {
        lyricsJob?.cancel()
        transJob?.cancel()
        lyrics = emptyList()
        rawLyrics = emptyList()
        lyricsTrans = emptyList()
        lyricsTransBusy = false
        lyricsTransError = ""
        lyricsSynced = false
        lyricsSource = ""
        lyricsLoading = true
        lyricsTrying = false
        lyricsTryFail = false
        lyricsSearching = false
        lyricsJob = viewModelScope.launch {
            var done = false
            try {
                val r = LyricsRepo.fetch(song.title, song.artist, song.durationSec, lyricsProvider)
                applyLyrics(r)
                done = r.lines.isNotEmpty() && (r.synced || lyricsProvider != 0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
            lyricsLoading = false
            // not found (or only plain text): keep trying in the background while the song plays,
            // each round with other search words and all sources
            var n = 1
            while (!done && n <= 6 && current?.url == song.url) {
                lyricsSearching = true
                delay(if (n == 1) 3000L else 12000L)
                if (current?.url != song.url) break
                try {
                    val r = LyricsRepo.fetch(song.title, song.artist, song.durationSec, lyricsProvider, emptySet(), n)
                    val better = r.lines.isNotEmpty() && (rawLyrics.isEmpty() || (r.synced && !lyricsSynced))
                    if (better) {
                        applyLyrics(r)
                        done = r.synced || lyricsProvider != 0
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                }
                n++
            }
            lyricsSearching = false
        }
    }

    private fun applyLyrics(r: LyricsResult) {
        rawLyrics = r.lines
        lyrics = styleLyrics(r.lines)
        lyricsSynced = r.synced
        lyricsSource = r.source
        lyricsTrans = emptyList()
        lyricsTransError = ""
        if (lyricsTransOn && r.lines.isNotEmpty()) loadTranslation()
    }

    // user picked a provider (0 Auto, 1 LRCLIB, 2 NetEase): save it and search again
    fun chooseLyricsProvider(i: Int) {
        lyricsProvider = i
        Prefs.setLyricsProvider(getApplication<Application>(), i)
        val s = current ?: return
        loadLyrics(s)
    }

    // "Try another source": search again but skip the provider that gave the current lyrics
    fun tryAnotherLyricsSource() {
        val s = current ?: return
        val skip = if (lyricsSource.isBlank()) emptySet() else setOf(lyricsSource)
        lyricsJob?.cancel()
        lyricsTrying = true
        lyricsTryFail = false
        lyricsSearching = false
        lyricsJob = viewModelScope.launch {
            try {
                val r = LyricsRepo.fetch(s.title, s.artist, s.durationSec, 0, skip)
                if (r.lines.isNotEmpty()) applyLyrics(r) else lyricsTryFail = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
            lyricsTrying = false
            lyricsLoading = false
        }
    }

    fun loadTranslation() {
        transJob?.cancel()
        val src = rawLyrics.map { it.text }
        if (src.isEmpty()) return
        lyricsTransBusy = true
        lyricsTransError = ""
        transJob = viewModelScope.launch {
            try {
                // which language is the song in? Devanagari letters = Hindi, else ask Google about a sample
                val joined = src.filter { it.isNotBlank() }.take(8).joinToString("\n")
                val letters = joined.count { it.isLetter() }.coerceAtLeast(1)
                val deva = joined.count { it in '\u0900'..'\u097F' }
                var songLang = if (deva * 100 / letters > 30) "hi" else LyricsTranslate.detect(joined)
                if (songLang.isBlank()) songLang = "en"
                val pick = if (songLang == "hi") lyricsTransHi else lyricsTransEn
                val code = LyricsRepo.langCodes[pick.coerceIn(0, LyricsRepo.langCodes.size - 1)]
                if (code.isBlank()) {
                    lyricsTransError = "Translation is Off for these songs. Change it in Settings > Songs and lyrics."
                } else if (code == songLang) {
                    lyricsTransError = "The lyrics are already in " + LyricsRepo.langNames[pick] + "."
                } else {
                    val out = LyricsTranslate.run(src, code)
                    lyricsTrans = out
                    if (out.none { it.isNotBlank() }) lyricsTransError = "Translation failed. Check the internet."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lyricsTransError = "Translation failed. Check the internet."
            }
            lyricsTransBusy = false
        }
    }

    fun toggleLyricsTrans(on: Boolean) {
        lyricsTransOn = on
        Prefs.setLyricsTransOn(getApplication<Application>(), on)
        if (on && lyricsTrans.isEmpty() && !lyricsTransBusy) loadTranslation()
    }

    // Settings: target language for Hindi songs / for every other song
    fun chooseTransHi(i: Int) {
        lyricsTransHi = i
        Prefs.setLyricsTransHi(getApplication<Application>(), i)
        lyricsTrans = emptyList()
        if (lyricsTransOn) loadTranslation()
    }

    fun chooseTransEn(i: Int) {
        lyricsTransEn = i
        Prefs.setLyricsTransEn(getApplication<Application>(), i)
        lyricsTrans = emptyList()
        if (lyricsTransOn) loadTranslation()
    }

    fun updateLyricsPct(p: Int, save: Boolean) {
        lyricsPct = p.coerceIn(50, 160)
        if (save) Prefs.setLyricsPct(getApplication<Application>(), lyricsPct)
    }

    // plain text of the lyrics for the Share button
    fun lyricsShareText(): String {
        val s = current ?: return ""
        val body = rawLyrics.joinToString("\n") { it.text }.trim()
        return s.title + " - " + artistName(s.artist) + "\n\n" + body
    }

    private fun start(song: Song) {
        val t = Connect.target
        if (t != null) {
            castStart(song, t)
            return
        }
        startLocal(song, 0L)
    }

    private fun applyRelated(song: Song, related: List<Song>) {
        if (related.isNotEmpty()) suggestions = related.take(10)
        if (endless && queue.size - index < 8) {
            val have = queue.map { it.url }.toSet()
            val more = related.filter { it.url !in have }.take(10)
            if (more.isNotEmpty()) queue = queue + more
        }
    }

    private fun startLocal(song: Song, startMs: Long) {
        startJob?.cancel()
        player.pause()
        timesLeft = 0
        current = song
        loading = true
        error = null
        loadLyrics(song)
        startJob = viewModelScope.launch {
            try {
                val local = Downloads.file(getApplication<Application>(), song)
                var fast: FastAudio? = null
                val audioUrl: String = if (local != null) {
                    Uri.fromFile(local).toString()
                } else {
                    val cachedFull = cache[song.url]
                    if (cachedFull != null) {
                        cachedFull.audio
                    } else {
                        val f = resolveAudioFor(song)
                        fast = f
                        f.audio
                    }
                }
                val meta = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .apply {
                        if (song.cover != null) setArtworkUri(Uri.parse(song.cover))
                    }
                    .build()
                val item = MediaItem.Builder()
                    .setUri(audioUrl)
                    .setMediaMetadata(meta)
                    .build()
                player.setMediaItem(item)
                player.prepare()
                if (startMs > 0L) player.seekTo(startMs)
                player.play()
                played.removeAll { it.url == song.url }
                played.add(0, song)
                while (played.size > 30) played.removeAt(played.size - 1)
                Prefs.saveHistory(getApplication<Application>(), played.toList())
                countPlay(song)

                // similar songs are worked out while the song already plays
                val cachedRelated = cache[song.url]?.related
                if (cachedRelated != null) {
                    applyRelated(song, cachedRelated)
                } else if (fast != null) {
                    val info = fast.info
                    viewModelScope.launch {
                        try {
                            val rel = MusicRepo.relatedFor(info, song.url, song.artist)
                            cache[song.url] = Resolved(audioUrl, rel)
                            if (current?.url == song.url) applyRelated(song, rel)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                        }
                    }
                }

                val ctx = getApplication<Application>()
                ctx.startService(Intent(ctx, PlaybackService::class.java))
                prefetchNext()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Play fail: " + e.javaClass.simpleName + " | " + (e.message ?: "-")
            }
            loading = false
        }
    }

    // ---------- home screen widgets (4x1 and 2x2) ----------

    private var widgetJob: kotlinx.coroutines.Job? = null

    fun widgetStart() {
        val app = getApplication<Application>()
        widgetJob?.cancel()
        widgetJob = viewModelScope.launch {
            var lastUrl: String? = null
            androidx.compose.runtime.snapshotFlow { Pair(current, isPlaying) }.collect { st ->
                val c = st.first
                val sp = app.getSharedPreferences("vame_widget", android.content.Context.MODE_PRIVATE)
                if (c == null) {
                    sp.edit().clear().apply()
                    WidgetUpdater.clearArt(app)
                    lastUrl = null
                    WidgetUpdater.refresh(app)
                } else {
                    sp.edit()
                        .putString("t", c.title)
                        .putString("a", artistName(c.artist))
                        .putBoolean("p", st.second)
                        .apply()
                    if (c.url != lastUrl) {
                        lastUrl = c.url
                        WidgetUpdater.clearArt(app)
                        WidgetUpdater.refresh(app)
                        WidgetUpdater.saveArt(app, c.cover)
                    }
                    WidgetUpdater.refresh(app)
                }
            }
        }
    }

    // ---------- Connect: play on another VAME device (same Wi-Fi) ----------

    private var connectJob: kotlinx.coroutines.Job? = null

    fun connectStart() {
        Connect.start(getApplication<Application>(), this)
        connectJob?.cancel()
        connectJob = viewModelScope.launch {
            while (true) {
                Connect.stateJson = connectStateJson()
                delay(700)
            }
        }
    }

    private fun connectStateJson(): String {
        val o = org.json.JSONObject()
        val c = current
        o.put("name", Connect.deviceName)
        if (c != null) {
            o.put("has", true)
            o.put("t", c.title)
            o.put("a", c.artist)
            o.put("u", c.url)
            o.put("d", c.durationSec)
            val cv = c.cover
            if (cv != null) o.put("c", cv)
            o.put("play", player.isPlaying)
            o.put("pos", player.currentPosition)
            val dd = player.duration
            o.put("dur", if (dd > 0L) dd else c.durationSec * 1000L)
        } else {
            o.put("has", false)
        }
        val am = getApplication<Application>().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        val mx = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        o.put("vol", am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) * 100 / mx)
        return o.toString()
    }

    // position / length / seek: from the other device while casting, else the local player
    fun posMs(): Long {
        val r = Connect.remote
        if (Connect.target != null && r != null) {
            val add = if (r.playing) android.os.SystemClock.elapsedRealtime() - r.at else 0L
            return r.posMs + add
        }
        return player.currentPosition
    }

    fun durMs(): Long {
        val r = Connect.remote
        if (Connect.target != null && r != null && r.durMs > 0L) return r.durMs
        return player.duration
    }

    fun seekTo(ms: Long) {
        val t = Connect.target
        if (t != null) {
            Connect.cmd(t, "seek", ms)
            val r = Connect.remote
            if (r != null) {
                Connect.remote = Remote(r.title, r.artist, r.cover, r.url, r.playing, ms, r.durMs, r.vol)
            }
        } else {
            player.seekTo(ms)
        }
    }

    // this device was told to play (another VAME sent a queue)
    fun connectPlayHere(list: List<Song>, i: Int, posMs: Long) {
        if (list.isEmpty() || Connect.target != null) return
        val k = i.coerceIn(0, list.size - 1)
        queue = list
        index = k
        startLocal(list[k], posMs)
    }

    // buttons pressed on another device
    fun connectCmd(cmd: String, v: Long) {
        if (Connect.target != null) return
        when (cmd) {
            "toggle" -> if (player.isPlaying) player.pause() else toggle()
            "play" -> if (!player.isPlaying) toggle()
            "pause" -> player.pause()
            "next" -> next()
            "prev" -> prev()
            "seek" -> player.seekTo(v)
            "vol" -> {
                val am = getApplication<Application>().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                val mx = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (v.coerceIn(0L, 100L) * mx / 100L).toInt(), 0)
            }
        }
    }

    private fun castStart(song: Song, t: Peer) {
        startJob?.cancel()
        player.pause()
        timesLeft = 0
        current = song
        error = null
        loadLyrics(song)
        Connect.sendPlay(t, queue, index, 0L)
    }

    // choose where the music plays: a Peer = that device, null = this device
    fun connectTo(p: Peer?) {
        val old = Connect.target
        if (p == null) {
            if (old == null) return
            val pos = posMs()
            Connect.cmd(old, "pause")
            Connect.target = null
            Connect.remote = null
            val c = current
            if (c != null) startLocal(c, pos)
            return
        }
        if (old != null && old.id == p.id) return
        val pos = if (old == null) player.currentPosition else posMs()
        if (old != null) Connect.cmd(old, "pause")
        player.pause()
        Connect.target = p
        Connect.remote = null
        if (current != null) Connect.sendPlay(p, queue, index, pos)
        Connect.startPolling()
    }

    // follow what the other device really plays (it moves on to the next song by itself)
    fun applyRemote(r: Remote) {
        isPlaying = r.playing
        val c = current
        if (c == null || c.url != r.url) {
            val k = queue.indexOfFirst { it.url == r.url }
            val s = if (k >= 0) queue[k] else Song(r.title, r.artist, r.url, r.cover, r.durMs / 1000L)
            if (k >= 0) index = k
            current = s
            loadLyrics(s)
        }
    }

    fun connectLost(p: Peer) {
        if (Connect.target?.id != p.id) return
        Connect.target = null
        Connect.remote = null
        android.widget.Toast.makeText(getApplication<Application>(), "Lost connection to " + p.name, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun toggle() {
        val tg = Connect.target
        if (tg != null) {
            Connect.cmd(tg, "toggle")
            return
        }
        val cur = current
        if (cur != null && player.mediaItemCount == 0 && !loading) {
            start(cur)
            return
        }
        if (player.isPlaying) player.pause() else player.play()
    }

    fun cycleSpeed() {
        val steps = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
        val i = steps.indexOf(speed)
        speed = steps[(i + 1) % steps.size]
        player.setPlaybackParameters(PlaybackParameters(speed))
        Prefs.setSpeed(getApplication<Application>(), speed)
    }

    fun setSkipSilenceOn(on: Boolean) {
        skipSilence = on
        player.skipSilenceEnabled = on
        Prefs.setSkipSilence(getApplication<Application>(), on)
    }

    fun isLiked(song: Song): Boolean = liked.any { it.url == song.url }

    fun toggleLike(song: Song) {
        if (isLiked(song)) liked.removeAll { it.url == song.url } else liked.add(0, song)
        Prefs.saveLikes(getApplication<Application>(), liked.toList())
    }

    // ---------- artist page ----------

    fun isArtistLiked(name: String): Boolean {
        val k = artistKey(name)
        return likedArtists.any { artistKey(it.name) == k }
    }

    fun toggleArtistLike(name: String, cover: String?) {
        val k = artistKey(name)
        if (isArtistLiked(name)) {
            likedArtists.removeAll { artistKey(it.name) == k }
        } else {
            likedArtists.add(0, ArtistItem(artistName(name), cover))
        }
        Prefs.saveLikedArtists(getApplication<Application>(), likedArtists.toList())
    }

    fun openArtist(name: String) {
        val clean = artistName(name)
        if (clean.isBlank()) return
        val key = artistKey(clean)
        openArtistName = clean
        artistOpen = true
        artistAlbum = null
        artistLatestRelease = null
        artistPage = null
        artistOnTop = true
        artistJob?.cancel()
        // songs we already know by this artist show first
        val known = ArrayList<Song>()
        val seen0 = HashSet<String>()
        for (s in liked + played) {
            if (artistKey(s.artist) == key && seen0.add(s.url)) known.add(s)
        }
        artistSongs = known
        artistLoading = true
        artistJob = viewModelScope.launch {
            // the newest official upload: its picture is the artist page "canvas"
            launch {
                try {
                    artistLatestRelease = MusicRepo.latestRelease(clean)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                }
            }
            // the artist's top album for the card on the artist page
            launch {
                try {
                    val al = MusicRepo.searchAlbums(clean)
                    artistAlbum = al.firstOrNull {
                        val k = artistKey(it.artist)
                        k.isNotEmpty() && (k.contains(key) || key.contains(k))
                    } ?: al.firstOrNull()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                }
            }
            // the full YouTube Music artist page (albums, singles, about...)
            launch {
                try {
                    val id = YtArtist.find(clean)
                    if (id != null) artistPage = YtArtist.load(id, clean)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                }
            }
            try {
                val all = ArrayList<Song>()
                for (q in listOf(clean + " songs", clean + " best songs", clean + " hits")) {
                    try {
                        all.addAll(MusicRepo.search(q))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                    }
                }
                val seen = HashSet<String>()
                val uniq = all.filter { seen.add(it.url) }
                val mine = uniq.filter {
                    val k = artistKey(it.artist)
                    k.isNotEmpty() && (k.contains(key) || key.contains(k))
                }
                val pick = if (mine.size >= 3) mine else uniq
                val have = known.map { it.url }.toSet()
                artistSongs = known + pick.filter { it.url !in have }
                if (artistSongs.isEmpty()) error = "Is artist ke songs nahi mile"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Artist load fail: " + (e.message ?: e.javaClass.simpleName)
            }
            artistLoading = false
        }
    }

    fun closeArtist() {
        artistOpen = false
    }

    fun playArtist(shuffle: Boolean) {
        val list = artistAllSongs
        if (list.isEmpty()) return
        val q = if (shuffle) list.shuffled() else list
        play(q.first(), q)
    }

    // ---------- liked songs page ----------

    fun closeLiked() {
        likedOpen = false
    }

    fun playLiked(shuffle: Boolean) {
        if (liked.isEmpty()) return
        val q = if (shuffle) liked.toList().shuffled() else liked.toList()
        play(q.first(), q)
    }

    // ---------- albums ----------

    fun openAlbum(a: Album) {
        artistOnTop = false
        albumInfo = a
        albumOpen = true
        albumSongs = emptyList()
        albumLoading = true
        albumJob?.cancel()
        albumJob = viewModelScope.launch {
            try {
                val d = MusicRepo.playlistSongs(a.url, 120)
                albumSongs = d.songs
                if (d.songs.isEmpty()) error = "Is album mein songs nahi mile"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Album load fail: " + (e.message ?: e.javaClass.simpleName)
            }
            albumLoading = false
        }
    }

    fun closeAlbum() {
        albumOpen = false
    }

    fun playAlbum(shuffle: Boolean) {
        val list = albumSongs
        if (list.isEmpty()) return
        val q = if (shuffle) list.shuffled() else list
        play(q.first(), q)
    }

    // closes every page and goes to Home
    fun goHome() {
        artistOpen = false
        albumOpen = false
        likedOpen = false
        downloadsOpen = false
        playlistOpen = false
        recapOpen = false
        tab = 0
    }

    // ---------- downloads ----------

    fun isDownloaded(song: Song): Boolean = downloads.any { it.url == song.url }

    fun downloadSong(song: Song) {
        if (isDownloaded(song) || downloading.containsKey(song.url)) return
        val app = getApplication<Application>()
        downloading[song.url] = 0f
        downloadingSongs[song.url] = song
        viewModelScope.launch {
            try {
                val r = MusicRepo.resolve(song.url, Prefs.downloadQuality(app), false)
                val ok = Downloads.fetch(app, song, r.audio) { p -> downloading[song.url] = p }
                if (ok) {
                    downloads.removeAll { it.url == song.url }
                    downloads.add(0, song)
                    Prefs.saveDownloads(app, downloads.toList())
                    refreshDownloadBytes()
                } else {
                    error = "Download fail hua: " + song.title
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Download fail: " + (e.message ?: e.javaClass.simpleName)
            }
            downloading.remove(song.url)
            downloadingSongs.remove(song.url)
        }
    }

    fun downloadAll(list: List<Song>) {
        for (s in list) downloadSong(s)
    }

    fun deleteDownload(song: Song) {
        val app = getApplication<Application>()
        Downloads.delete(app, song)
        downloads.removeAll { it.url == song.url }
        Prefs.saveDownloads(app, downloads.toList())
        refreshDownloadBytes()
    }

    fun refreshDownloadBytes() {
        viewModelScope.launch {
            downloadBytes = withContext(Dispatchers.IO) {
                Downloads.totalBytes(getApplication<Application>())
            }
        }
    }

    fun updateDownloadQuality(q: Int) {
        downloadQuality = q
        Prefs.setDownloadQuality(getApplication<Application>(), q)
    }

    fun playDownloads(shuffle: Boolean) {
        if (downloads.isEmpty()) return
        val q = if (shuffle) downloads.toList().shuffled() else downloads.toList()
        play(q.first(), q)
    }

    fun setKeepAliveOn(on: Boolean) {
        keepAlive = on
        Prefs.setKeepAlive(getApplication<Application>(), on)
    }

    // ---------- import playlist (YouTube / YouTube Music / Spotify link) ----------

    // looks for many songs at once (8 at the same time) and shows x/y progress
    private suspend fun findSongs(queries: List<String>): List<Song> {
        val total = queries.size
        val found = arrayOfNulls<Song>(total)
        var done = 0
        for (chunk in queries.withIndex().chunked(8)) {
            importStatus = "Songs dhoondh raha hoon: " + done + "/" + total
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

    fun importPlaylist(link: String) {
        val raw = link.trim()
        if (raw.isEmpty() || importBusy) return
        importBusy = true
        importedId = null
        importStatus = "Link check kar raha hoon..."
        viewModelScope.launch {
            try {
                var name: String
                var songs: List<Song>
                if (!raw.contains("http", ignoreCase = true)) {
                    // a typed / pasted list: one song per line ("Title - Artist")
                    val lines = Importer.textLines(raw)
                    if (lines.size < 2) {
                        throw IllegalStateException("Link do, ya har line mein ek song likho (kam se kam 2)")
                    }
                    name = "My imported list"
                    songs = findSongs(lines)
                } else {
                    val l = Importer.extractLink(raw)
                    if (Importer.isSpotify(l)) {
                        val sp = Importer.spotify(l)
                        name = sp.name
                        songs = findSongs(sp.queries)
                    } else {
                        val u = Importer.youtubeUrl(l)
                            ?: throw IllegalStateException("Playlist link sahi nahi hai (list= wala link do)")
                        importStatus = "Playlist padh raha hoon..."
                        val d = MusicRepo.playlistSongs(u, 500)
                        name = d.name
                        songs = d.songs.distinctBy { it.url }
                    }
                }
                if (songs.isEmpty()) throw IllegalStateException("Koi song nahi mila")
                val id = System.currentTimeMillis().toString()
                playlists.add(Playlist(id, name.take(40).ifBlank { "Imported playlist" }, null, songs))
                savePlaylists()
                importedId = id
                ytPush(id)
                importStatus = "Ho gaya! " + songs.size + " songs ki playlist \"" + name + "\" ban gayi."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                importStatus = "Import fail: " + (e.message ?: e.javaClass.simpleName)
            }
            importBusy = false
        }
    }

    // ---------- app updates (new GitHub Release) ----------
    var updateInfo by mutableStateOf<Updater.Info?>(null)
    var showUpdate by mutableStateOf(false)
    var updateStatus by mutableStateOf("")
    var updateProgress by mutableIntStateOf(-1) // -1 = not downloading, 0..100 = downloading
    var updateError by mutableStateOf<String?>(null)

    var updateDetail by mutableStateOf("")
    private var updateJob: Job? = null

    fun cancelUpdate() {
        updateJob?.cancel()
        updateJob = null
        updateProgress = -1
        updateDetail = ""
        updateError = "Download cancelled. Tap Try again to continue."
    }

    fun startUpdate(app: android.content.Context, info: Updater.Info) {
        if (updateProgress >= 0) return
        if (Updater.needsInstallPermission(app)) return
        updateError = null
        updateDetail = ""
        updateProgress = 0
        updateJob = viewModelScope.launch {
            try {
                val f = Updater.downloadApk(app, info, { updateProgress = it }, { updateDetail = it })
                updateProgress = -1
                updateDetail = ""
                showUpdate = false
                Updater.install(app, f)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateProgress = -1
                updateDetail = ""
                updateError = "Download failed: " + (e.message ?: e.javaClass.simpleName) +
                    ". Tap Try again (it continues where it stopped) or use the browser."
            }
        }
    }

    init {
        checkUpdate(false)
    }

    fun checkUpdate(manual: Boolean) {
        viewModelScope.launch {
            if (manual) updateStatus = "Checking..."
            val app = getApplication<Application>()
            try {
                val info = Updater.latest()
                val cur = Updater.currentVersion(app)
                if (info != null && Updater.isNewer(info.version, cur)) {
                    updateInfo = info
                    showUpdate = true
                    updateStatus = "Update " + info.version + " is available"
                } else if (manual) {
                    updateStatus = "You are on the latest version (" + cur + ")"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (manual) updateStatus = "Could not check: " + (e.message ?: e.javaClass.simpleName)
            }
        }
    }

    // ---------- optional Google / YouTube Music account ----------
    var showLogin by mutableStateOf(false)
    var ytSignedIn by mutableStateOf(Account.signedIn(app))
    var ytName by mutableStateOf(Account.name(app))
    var ytPhoto by mutableStateOf<String?>(Account.photo(app))
    val ytPlaylists = mutableStateListOf<YtPlaylist>()
    var ytBusy by mutableStateOf(false)
    var ytStatus by mutableStateOf("")

    // your name on the Library stats cards + albums saved to Library
    var userName by mutableStateOf(Prefs.userName(app))
    val savedAlbums = mutableStateListOf<Album>()

    // lyrics text size + keep screen on in lyrics
    var lyricsSize by mutableIntStateOf(Prefs.lyricsSize(app))
    var keepScreenLyrics by mutableStateOf(Prefs.keepScreenLyrics(app))
    val ytArtists = mutableStateListOf<ArtistItem>()

    fun updateLyricsSize(i: Int) {
        lyricsSize = i
        Prefs.setLyricsSize(getApplication<Application>(), i)
        updateLyricsPct(when (i) { 0 -> 80; 2 -> 120; else -> 100 }, true)
    }

    fun setKeepScreenLyricsOn(on: Boolean) {
        keepScreenLyrics = on
        Prefs.setKeepScreenLyrics(getApplication<Application>(), on)
    }

    // ---- save VAME playlists to the person's YouTube Music account (write access) ----
    var ytSync by mutableStateOf(Account.syncOn(app))
    private val ytPushing = HashSet<String>()

    fun toggleYtSync() {
        ytSync = !ytSync
        Account.saveSyncOn(getApplication<Application>(), ytSync)
        toast(if (ytSync) "New playlists are saved to YouTube Music too" else "Playlists stay only in VAME")
    }

    // YouTube playlists that VAME created itself are already shown as VAME playlists, so hide the copy
    val ytPlaylistsShown: List<YtPlaylist>
        get() {
            val mapped = Account.mappedIds(getApplication<Application>())
            return ytPlaylists.filter { it.id.removePrefix("VL") !in mapped }
        }

    fun ytPush(id: String) {
        if (!ytSignedIn || !ytSync) return
        if (Account.ytIdOf(getApplication<Application>(), id) != null) return
        if (!ytPushing.add(id)) return
        val pl = playlists.firstOrNull { it.id == id }
        if (pl == null) {
            ytPushing.remove(id)
            return
        }
        viewModelScope.launch {
            try {
                val ids = pl.songs.mapNotNull { Account.videoId(it.url) }.distinct().take(300)
                val yid = withContext(Dispatchers.IO) {
                    Account.createPlaylist(getApplication<Application>(), pl.name, ids)
                }
                if (yid.isNotBlank()) Account.mapId(getApplication<Application>(), id, yid)
            } catch (e: Exception) {
                toast("YouTube Music: " + (e.message ?: "playlist not saved"))
            } finally {
                ytPushing.remove(id)
            }
        }
    }

    private fun ytAdd(id: String, song: Song) {
        if (!ytSignedIn || !ytSync) return
        val yid = Account.ytIdOf(getApplication<Application>(), id)
        if (yid == null) {
            ytPush(id)
            return
        }
        val vid = Account.videoId(song.url) ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { Account.addVideo(getApplication<Application>(), yid, vid) }
            } catch (e: Exception) {
                toast("YouTube Music: " + (e.message ?: "song not added"))
            }
        }
    }

    private fun ytRename(id: String) {
        if (!ytSignedIn) return
        val yid = Account.ytIdOf(getApplication<Application>(), id) ?: return
        val name = playlists.firstOrNull { it.id == id }?.name ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { Account.renamePlaylist(getApplication<Application>(), yid, name) }
            } catch (e: Exception) {
            }
        }
    }

    private fun ytDelete(id: String) {
        val yid = Account.ytIdOf(getApplication<Application>(), id) ?: return
        Account.unmapId(getApplication<Application>(), id)
        if (!ytSignedIn) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { Account.deletePlaylist(getApplication<Application>(), yid) }
            } catch (e: Exception) {
            }
        }
    }

    // uploads every VAME playlist that is not on YouTube Music yet
    fun ytPushAll() {
        if (!ytSignedIn) return
        var n = 0
        for (p in playlists.toList()) {
            if (Account.ytIdOf(getApplication<Application>(), p.id) == null && p.songs.isNotEmpty()) {
                val was = ytSync
                ytSync = true
                ytPush(p.id)
                ytSync = was
                n++
            }
        }
        toast(if (n == 0) "All playlists are already on YouTube Music" else "Uploading " + n + " playlists to YouTube Music...")
    }

    // start tab, lyrics alignment, tablet player width, artist page album card
    var startTab by mutableIntStateOf(Prefs.startTab(app))
    var lyricsLeft by mutableStateOf(Prefs.lyricsLeft(app))
    var panelWide by mutableStateOf(Prefs.panelWide(app))
    var artistAlbum by mutableStateOf<Album?>(null)
    var artistPage by mutableStateOf<ArtistPage?>(null)
    // true = the artist page is the top page; false = an album / playlist opened from it is on top
    var artistOnTop by mutableStateOf(true)
    val artistAllSongs: List<Song>
        get() = ((artistPage?.songs ?: emptyList<Song>()) + artistSongs).distinctBy { it.url }

    // speed dial (pinned playlists, albums, artists, songs)
    val dials = mutableStateListOf<Dial>().apply { addAll(SpeedDial.load(app)) }

    fun isDialed(kind: String, id: String): Boolean = dials.any { it.kind == kind && it.id == id }

    // true = pinned now, false = unpinned now
    fun toggleDial(d: Dial): Boolean {
        val i = dials.indexOfFirst { it.kind == d.kind && it.id == d.id }
        if (i >= 0) dials.removeAt(i) else dials.add(0, d)
        SpeedDial.save(getApplication<Application>(), dials.toList())
        return i < 0
    }

    fun openDial(d: Dial) {
        when (d.kind) {
            "playlist" -> openPlaylist(d.id)
            "album" -> {
                if (d.id.startsWith("yt:")) openYtPlaylist(YtPlaylist(d.id.removePrefix("yt:"), d.title, d.sub, d.cover))
                else openAlbum(Album(d.title, d.sub, d.id, d.cover))
            }
            "artist" -> openArtist(d.id)
            "song" -> play(Song(d.title, d.sub, d.id, d.cover, 0L))
        }
    }

    // ---------- queue / playlist menu helpers ----------

    // removes the whole queue and stops the music (like "Dismiss queue" in YouTube Music)
    fun dismissQueue() {
        if (Connect.target != null) connectTo(null)
        startJob?.cancel()
        player.stop()
        player.clearMediaItems()
        queue = emptyList()
        index = 0
        current = null
        isPlaying = false
        showPlayer = false
        showLyrics = false
        tabStandby = false
    }

    fun playAllNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        if (queue.isEmpty() || current == null) {
            play(songs.first(), songs)
            return
        }
        val urls = songs.map { it.url }.toSet()
        val list = queue.toMutableList()
        var idx = index
        for (i in list.size - 1 downTo 0) {
            if (i != idx && list[i].url in urls) {
                list.removeAt(i)
                if (i < idx) idx--
            }
        }
        val curUrl = list.getOrNull(idx)?.url
        list.addAll((idx + 1).coerceAtMost(list.size), songs.filter { it.url != curUrl })
        queue = list
        index = idx
        prefetchNext()
    }

    fun addAllToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        if (queue.isEmpty() || current == null) {
            play(songs.first(), songs)
            return
        }
        val have = queue.map { it.url }.toSet()
        queue = queue + songs.filter { it.url !in have }
    }

    fun startMix(songs: List<Song>) {
        val seed = songs.randomOrNull() ?: return
        play(seed, listOf(seed))
        startRadio()
    }

    // adds every song to one of your playlists; returns how many were new
    fun addAllToPlaylist(id: String, songs: List<Song>): Int {
        var n = 0
        for (s in songs) if (addToPlaylist(id, s)) n++
        return n
    }

    // new playlist made from these songs
    fun newPlaylistFrom(name: String, songs: List<Song>) {
        val id = System.currentTimeMillis().toString()
        playlists.add(Playlist(id, name.trim().ifBlank { "My playlist" }.take(40), null, songs.distinctBy { it.url }))
        savePlaylists()
        ytPush(id)
    }
    var artistLatestRelease by mutableStateOf<LatestRelease?>(null)
    var artistLatestPic by mutableStateOf(Prefs.artistLatest(app))
    var artistAlbumCardOn by mutableStateOf(Prefs.artistAlbumCard(app))
    var artistTop by mutableIntStateOf(Prefs.artistTop(app))

    // Home shelves built from the genres picked at sign up (English and Hindi mix)

    fun chooseHomeLang(i: Int) {
        homeLang = i
        Prefs.setHomeLang(getApplication<Application>(), i)
        loadGenreShelves()
    }

    private fun shelfQueries(g: String, lang: Int): List<String> = when (g) {
        "Bollywood" -> listOf("Bollywood hindi songs 2026", "Bollywood romantic songs")
        "Punjabi" -> listOf("Punjabi songs 2026", "Punjabi hits")
        "K-pop" -> listOf("K-pop songs 2026", "K-pop hits")
        "Devotional" -> listOf("hindi devotional songs", "bhajan")
        else -> when (lang) {
            1 -> listOf(g + " english songs 2026")
            2 -> listOf(g + " hindi songs 2026")
            else -> listOf(g + " english songs 2026", g + " hindi songs 2026")
        }
    }

    // YouTube Music Home shelves: first page + up to two more, shown as each page arrives
    fun loadYtHome() {
        ytHomeJob?.cancel()
        ytHomeJob = viewModelScope.launch {
            ytHomeLoading = true
            ytHomeError = null
            val all = ArrayList<YtShelf>()
            try {
                var token: String? = null
                var n = 0
                while (n < 3) {
                    val pg = YtHome.page(getApplication<Application>(), token)
                    for (sh in pg.shelves) {
                        if (all.none { it.title == sh.title }) all.add(sh)
                    }
                    ytHome = all.toList()
                    token = pg.next ?: break
                    n++
                }
                if (all.isEmpty()) ytHomeError = "No shelves came back from YouTube Music."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (all.isEmpty()) ytHomeError = e.message ?: e.javaClass.simpleName
            }
            ytHomeLoading = false
        }
    }

    // tap on a YouTube Music Home card: play the song, open the album / playlist / artist page
    fun openYtCard(card: YtCard, shelf: List<YtCard>) {
        when (card.kind) {
            "song" -> {
                val songs = shelf.filter { it.kind == "song" }
                    .map { Song(it.title, it.artist, it.id, it.cover, 0L) }
                val me = songs.firstOrNull { it.url == card.id } ?: return
                play(me, songs)
            }
            "artist" -> openArtist(card.title)
            else -> openYtPlaylist(YtPlaylist(card.id, card.title, card.sub, card.cover))
        }
    }

    fun loadGenreShelves() {
        shelfJob?.cancel()
        shelfJob = viewModelScope.launch {
            val names = if (genres.isEmpty()) listOf("Pop", "Hip-Hop", "Bollywood") else genres.toList().take(6)
            val lang = homeLang
            val out = ArrayList<GenreShelf>()
            genreShelves = emptyList()
            for (n in names) {
                val qs = shelfQueries(n, lang)
                val lists: List<List<Song>> = try {
                    coroutineScope {
                        qs.map { q ->
                            async {
                                try {
                                    MusicRepo.search(q)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    emptyList<Song>()
                                }
                            }
                        }.awaitAll()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList<List<Song>>()
                }
                val mixed = ArrayList<Song>()
                val max = lists.maxOfOrNull { it.size } ?: 0
                for (i in 0 until max) {
                    for (l in lists) {
                        val x = l.getOrNull(i)
                        if (x != null) mixed.add(x)
                    }
                }
                val songs = mixed.distinctBy { it.url }.take(20)
                if (songs.isNotEmpty()) {
                    out.add(GenreShelf(n, songs))
                    genreShelves = out.toList()
                }
            }
        }
    }

    fun chooseArtistLatestPic(on: Boolean) {
        artistLatestPic = on
        Prefs.setArtistLatest(getApplication<Application>(), on)
    }

    fun chooseArtistAlbumCard(on: Boolean) {
        artistAlbumCardOn = on
        Prefs.setArtistAlbumCard(getApplication<Application>(), on)
    }

    fun updateArtistTop(i: Int) {
        artistTop = i
        Prefs.setArtistTop(getApplication<Application>(), i)
    }

    fun updateStartTab(i: Int) {
        startTab = i
        Prefs.setStartTab(getApplication<Application>(), i)
    }

    fun setLyricsLeftOn(on: Boolean) {
        lyricsLeft = on
        Prefs.setLyricsLeft(getApplication<Application>(), on)
    }

    fun setPanelWideOn(on: Boolean) {
        panelWide = on
        Prefs.setPanelWide(getApplication<Application>(), on)
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(getApplication<Application>(), msg, android.widget.Toast.LENGTH_LONG).show()
    }

    init {
        savedAlbums.addAll(Prefs.savedAlbums(app))
        if (ytSignedIn) {
            ytPlaylists.addAll(Account.library(app))
            refreshYt()
        }
    }

    fun updateUserName(t: String) {
        userName = t
        Prefs.setUserName(getApplication<Application>(), t)
    }

    // called by the login screen when the Google cookie is ready
    fun onLoginDone(cookie: String) {
        Account.saveCookie(getApplication<Application>(), cookie)
        ytSignedIn = true
        showLogin = false
        refreshYt()
        loadYtHome()
    }

    fun refreshYt() {
        if (!ytSignedIn || ytBusy) return
        ytBusy = true
        ytStatus = "YouTube library la raha hoon..."
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                val acc = Account.fetchAccount(app)
                if (acc != null) {
                    ytName = acc.name
                    ytPhoto = acc.photo
                    Account.saveAccount(app, acc)
                }
                val list = Account.libraryPlaylists(app)
                ytPlaylists.clear()
                ytPlaylists.addAll(list)
                Account.saveLibrary(app, list)
                try {
                    val ar = Account.libraryArtists(app)
                    ytArtists.clear()
                    ytArtists.addAll(ar)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                }
                ytStatus = list.size.toString() + " playlists mili"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ytStatus = "Library load fail: " + (e.message ?: e.javaClass.simpleName)
            }
            ytBusy = false
        }
    }

    fun signOutYt() {
        Account.signOut(getApplication<Application>())
        ytSignedIn = false
        ytName = ""
        ytPhoto = null
        ytStatus = ""
        ytPlaylists.clear()
        ytArtists.clear()
        loadYtHome()
    }

    // opens a YouTube library playlist on the album page (songs stay on YouTube, nothing is copied)
    fun openYtPlaylist(p: YtPlaylist) {
        artistOnTop = false
        albumInfo = Album(p.title, "YouTube Music", "yt:" + p.id, p.cover)
        albumOpen = true
        albumSongs = emptyList()
        albumLoading = true
        albumJob?.cancel()
        albumJob = viewModelScope.launch {
            try {
                val raw = Account.playlistSongs(getApplication<Application>(), p.id)
                // album tracks come without a picture / artist: take them from the album card
                val songs = if (p.id.startsWith("MPRE")) {
                    raw.map { it.copy(cover = it.cover ?: p.cover, artist = it.artist.ifBlank { YtHome.artistOf(p.subtitle) }) }
                } else raw
                albumSongs = songs
                val cur = albumInfo
                if (cur != null && cur.cover == null && songs.isNotEmpty()) {
                    albumInfo = cur.copy(cover = songs.first().cover)
                }
                if (songs.isEmpty()) error = "Is playlist mein songs nahi mile"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "YouTube playlist load fail: " + (e.message ?: e.javaClass.simpleName)
            }
            albumLoading = false
        }
    }

    fun openYtLiked() {
        openYtPlaylist(YtPlaylist("VLLM", "YouTube liked songs", "", null))
    }

    // the open YouTube playlist -> a normal VAME playlist
    fun saveYtAsPlaylist() {
        val a = albumInfo ?: return
        val songs = albumSongs.distinctBy { it.url }
        if (songs.isEmpty()) return
        val id = System.currentTimeMillis().toString()
        playlists.add(Playlist(id, a.title.take(40).ifBlank { "YouTube playlist" }, null, songs))
        savePlaylists()
        toast("\"" + a.title + "\" VAME playlist ban gayi (" + songs.size + " songs)")
    }

    // all your YouTube liked songs -> VAME Liked songs (only adds, never removes)
    fun importYtLikes() {
        if (!ytSignedIn || ytBusy) return
        ytBusy = true
        ytStatus = "Liked songs la raha hoon..."
        viewModelScope.launch {
            try {
                val songs = Account.playlistSongs(getApplication<Application>(), "VLLM", 500)
                var added = 0
                for (s in songs.reversed()) {
                    if (!isLiked(s)) {
                        liked.add(0, s)
                        added++
                    }
                }
                Prefs.saveLikes(getApplication<Application>(), liked.toList())
                ytStatus = added.toString() + " nayi liked songs aayi"
                toast(added.toString() + " songs Liked songs mein add hue")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ytStatus = "Likes load fail: " + (e.message ?: e.javaClass.simpleName)
            }
            ytBusy = false
        }
    }

    fun isAlbumSaved(a: Album): Boolean = savedAlbums.any { it.url == a.url }

    fun toggleSaveAlbum(a: Album) {
        if (isAlbumSaved(a)) savedAlbums.removeAll { it.url == a.url } else savedAlbums.add(0, a)
        Prefs.saveSavedAlbums(getApplication<Application>(), savedAlbums.toList())
    }

    // ---------- bulk helpers (Spotify copy, backup restore) ----------

    // a new VAME playlist from a list of songs, returns its id
    fun addImportedPlaylist(name: String, songs: List<Song>, syncYt: Boolean = true): String {
        var id = System.currentTimeMillis().toString() + playlists.size.toString()
        while (playlists.any { it.id == id }) id += "x"
        playlists.add(Playlist(id, name.take(40).ifBlank { "Imported playlist" }, null, songs.distinctBy { it.url }))
        savePlaylists()
        if (syncYt) ytPush(id)
        return id
    }

    // adds songs to Liked songs (only the ones that are not there yet), returns how many were new
    fun addLikedBulk(songs: List<Song>): Int {
        var n = 0
        for (s in songs.reversed()) {
            if (!isLiked(s)) {
                liked.add(0, s)
                n++
            }
        }
        if (n > 0) Prefs.saveLikes(getApplication<Application>(), liked.toList())
        return n
    }

    fun addLikedArtistsBulk(list: List<ArtistItem>): Int {
        var n = 0
        for (a in list) {
            if (a.name.isNotBlank() && !isArtistLiked(a.name)) {
                likedArtists.add(0, ArtistItem(artistName(a.name), a.cover))
                n++
            }
        }
        if (n > 0) Prefs.saveLikedArtists(getApplication<Application>(), likedArtists.toList())
        return n
    }

    fun addSavedAlbumsBulk(list: List<Album>): Int {
        var n = 0
        for (a in list) {
            if (!isAlbumSaved(a)) {
                savedAlbums.add(0, a)
                n++
            }
        }
        if (n > 0) Prefs.saveSavedAlbums(getApplication<Application>(), savedAlbums.toList())
        return n
    }
}
