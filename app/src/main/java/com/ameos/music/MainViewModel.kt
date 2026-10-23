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

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val player = PlayerHolder.get(app)

    // navigation: 0 Home, 1 Search, 2 Your Type, 3 Library
    var tab by mutableIntStateOf(0)
    var showPlayer by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showLyrics by mutableStateOf(false)
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
        StatsStore.save(getApplication<Application>(), stats)
        PlaybackBridge.onNext = null
        PlaybackBridge.onPrev = null
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
                albumResults = try {
                    MusicRepo.searchAlbums(q).take(10)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
                if (results.isEmpty()) error = "Kuch nahi mila"
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
    }

    fun editPlaylist(id: String, name: String, coverUri: Uri?) {
        val i = playlists.indexOfFirst { it.id == id }
        if (i < 0) return
        val old = playlists[i]
        playlists[i] = old.copy(name = name.trim().ifBlank { old.name })
        savePlaylists()
        if (coverUri != null) setPlaylistCover(id, coverUri)
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
    }

    fun addToPlaylist(id: String, song: Song): Boolean {
        val i = playlists.indexOfFirst { it.id == id }
        if (i < 0) return false
        val pl = playlists[i]
        if (pl.songs.any { it.url == song.url }) return false
        playlists[i] = pl.copy(songs = pl.songs + song)
        savePlaylists()
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

    private fun prefetchNext() {
        if (queue.size < 2) return
        val n = queue[(index + 1) % queue.size]
        if (cache.containsKey(n.url)) return
        viewModelScope.launch {
            try {
                resolveFor(n)
            } catch (e: Exception) {
            }
        }
    }

    private fun loadLyrics(song: Song) {
        lyricsJob?.cancel()
        lyrics = emptyList()
        rawLyrics = emptyList()
        lyricsSynced = false
        lyricsLoading = true
        lyricsJob = viewModelScope.launch {
            try {
                val r = LyricsRepo.fetch(song.title, song.artist, song.durationSec)
                rawLyrics = r.lines
                lyrics = styleLyrics(r.lines)
                lyricsSynced = r.synced
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
            lyricsLoading = false
        }
    }

    private fun start(song: Song) {
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
                val r = if (local != null) Resolved(Uri.fromFile(local).toString(), emptyList())
                else resolveFor(song)
                val meta = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .apply {
                        if (song.cover != null) setArtworkUri(Uri.parse(song.cover))
                    }
                    .build()
                val item = MediaItem.Builder()
                    .setUri(r.audio)
                    .setMediaMetadata(meta)
                    .build()
                player.setMediaItem(item)
                player.prepare()
                player.play()
                played.removeAll { it.url == song.url }
                played.add(0, song)
                while (played.size > 30) played.removeAt(played.size - 1)
                Prefs.saveHistory(getApplication<Application>(), played.toList())
                countPlay(song)

                if (r.related.isNotEmpty()) suggestions = r.related.take(10)
                if (endless && queue.size - index < 8) {
                    val have = queue.map { it.url }.toSet()
                    val more = r.related.filter { it.url !in have }.take(10)
                    if (more.isNotEmpty()) queue = queue + more
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

    fun toggle() {
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
        val list = artistSongs
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

    fun startUpdate(app: android.content.Context, info: Updater.Info) {
        if (updateProgress >= 0) return
        if (Updater.needsInstallPermission(app)) return
        updateError = null
        updateProgress = 0
        viewModelScope.launch {
            try {
                val f = Updater.downloadApk(app, info) { updateProgress = it }
                updateProgress = -1
                showUpdate = false
                Updater.install(app, f)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateProgress = -1
                updateError = "Download failed: " + (e.message ?: e.javaClass.simpleName)
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
}
