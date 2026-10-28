package com.ameos.music

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class Song(
    val title: String,
    val artist: String,
    val url: String,
    val cover: String?,
    val durationSec: Long
)

data class Resolved(val audio: String, val related: List<Song>)

// step59: the audio link comes first (fast), the "similar songs" are worked out afterwards from the same info
class FastAudio(val audio: String, val info: StreamInfo)

data class Album(val title: String, val artist: String, val url: String, val cover: String?)

// an artist found by search (tap opens the artist page)
data class ArtistHit(val name: String, val cover: String?)

// the newest official upload of an artist: the song, its big picture, and a short name
data class LatestRelease(val song: Song, val picture: String?, val label: String)

data class PlaylistData(val name: String, val artist: String, val cover: String?, val songs: List<Song>)

object MusicRepo {

    // "Official songs only": hides remixes, slowed/reverb, lofi, covers, karaoke, mashups
    // and fan/lyrics channels. Set from Settings (MainViewModel keeps it in sync).
    @Volatile
    var officialOnly: Boolean = true

    // audio quality: 0 high, 1 normal, 2 data saver
    @Volatile
    var quality: Int = 0

    // words that are junk anywhere in the title
    private val strongJunk = Regex(
        "\\b(remix|remixed|slowed|reverb|sped up|speed up|speedup|nightcore|mashup|karaoke|" +
            "lofi|lo-fi|8d|bass boosted|bass boost|bootleg|unofficial|tiktok|ringtone|" +
            "whatsapp status|jhankar|tapori)\\b",
        RegexOption.IGNORE_CASE
    )

    // words that are junk only in the (brackets) / - dash / | pipe part of the title
    private val weakJunk = Regex(
        "\\b(cover|dj|mix|instrumental|flip|edit by|version by)\\b",
        RegexOption.IGNORE_CASE
    )

    // channels that are not the artist or the label
    private val junkChannel = Regex(
        "\\b(lyrics|lyrical|lofi|lo-fi|remix|remixes|slowed|reverb|nightcore|8d|karaoke|" +
            "mashup|cover|covers|fan made|unofficial|status|ringtones)\\b",
        RegexOption.IGNORE_CASE
    )

    // "Top 100 hits", "best of", jukebox, non-stop mixes, full albums, various artists...
    private val compilation = Regex(
        "(\\btop\\s*\\d+|\\btop hits\\b|\\b\\d+\\s*(hits|songs|tracks)\\b|\\bbest of\\b|" +
            "\\bgreatest hits\\b|\\bjukebox\\b|\\bnon[- ]?stop\\b|\\bmegamix\\b|\\bplaylist\\b|" +
            "\\bcompilation\\b|\\bvarious artists\\b|\\bfull album\\b|\\ball time hits\\b|\\bhits of\\b)",
        RegexOption.IGNORE_CASE
    )

    private fun isCompilation(title: String, artist: String): Boolean {
        if (compilation.containsMatchIn(title)) return true
        if (compilation.containsMatchIn(artist)) return true
        return false
    }

    // YouTube Music "Topic" channels and VEVO are the real artist / label uploads
    private fun looksLikeMusicChannel(artist: String): Boolean {
        val a = artist.lowercase()
        return a.endsWith("- topic") || a.contains("vevo")
    }

    private fun isOfficial(s: Song): Boolean {
        val t = s.title
        if (strongJunk.containsMatchIn(t)) return false
        val cut = t.indexOfFirst { it == '(' || it == '[' || it == '-' || it == '|' }
        if (cut >= 0 && weakJunk.containsMatchIn(t.substring(cut))) return false
        if (junkChannel.containsMatchIn(s.artist)) return false
        return true
    }

    // if the person typed remix/slowed/lofi themselves, they want those, so do not filter
    private fun applyFilter(list: List<Song>, query: String?): List<Song> {
        // always: no "Top 100 hits" style compilations and no hour-long uploads,
        // unless the person typed that themselves
        val wantsCompilation = query != null && compilation.containsMatchIn(query)
        val music = if (wantsCompilation) list else list.filter {
            !isCompilation(it.title, it.artist) &&
                (it.durationSec <= 0L || it.durationSec in 45L..720L)
        }
        val base = if (music.isEmpty()) list else music
        if (!officialOnly) return base
        if (query != null && strongJunk.containsMatchIn(query)) return base
        val clean = base.filter { isOfficial(it) }
        return if (clean.isEmpty()) base else clean
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val extractor = ServiceList.YouTube.getSearchExtractor(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS),
            ""
        )
        extractor.fetchPage()
        val all = extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { toSong(it) }
        applyFilter(all, query)
    }

    // "3 weeks ago" -> seconds (smaller = newer); null when it cannot be read
    private fun ageSeconds(text: String?): Long? {
        if (text == null) return null
        val m = Regex("(\\d+)\\s+(second|minute|hour|day|week|month|year)s?\\s+ago", RegexOption.IGNORE_CASE)
            .find(text) ?: return null
        val n = m.groupValues[1].toLongOrNull() ?: return null
        val unit = when (m.groupValues[2].lowercase()) {
            "second" -> 1L
            "minute" -> 60L
            "hour" -> 3600L
            "day" -> 86400L
            "week" -> 604800L
            "month" -> 2592000L
            else -> 31536000L
        }
        return n * unit
    }

    // newest official video / song upload of the artist (used for the artist page picture)
    suspend fun latestRelease(artist: String): LatestRelease? = withContext(Dispatchers.IO) {
        val key = artist.lowercase().filter { it.isLetterOrDigit() }
        if (key.isEmpty()) return@withContext null
        val extractor = ServiceList.YouTube.getSearchExtractor(
            artist + " official video",
            listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
            ""
        )
        extractor.fetchPage()
        var best: StreamInfoItem? = null
        var bestAge = Long.MAX_VALUE
        for (item in extractor.initialPage.items) {
            if (item !is StreamInfoItem) continue
            val title = item.name ?: continue
            val channel = (item.uploaderName ?: "").lowercase().filter { it.isLetterOrDigit() }
            val titleKey = title.lowercase().filter { it.isLetterOrDigit() }
            val byArtist = (channel.isNotEmpty() && (channel.contains(key) || key.contains(channel))) ||
                titleKey.contains(key)
            if (!byArtist) continue
            if (junkChannel.containsMatchIn(item.uploaderName ?: "")) continue
            if (strongJunk.containsMatchIn(title) || isCompilation(title, item.uploaderName ?: "")) continue
            if (item.duration in 1L..44L || item.duration > 720L) continue
            val age = ageSeconds(item.textualUploadDate) ?: continue
            if (age < bestAge) {
                bestAge = age
                best = item
            }
        }
        val b = best ?: return@withContext null
        val pic = b.thumbnails.maxByOrNull { it.height }?.url
        val label = title0(b.name ?: "")
        LatestRelease(toSong(b), pic, label)
    }

    // "Softly (Official Video)" -> "Softly"
    private fun title0(t: String): String {
        val cleaned = t.replace(Regex("\\s*[\\(\\[][^\\)\\]]*[\\)\\]]"), "").trim()
        return if (cleaned.isBlank()) t else cleaned
    }

    // artists (YouTube Music "Artists" search)
    suspend fun searchArtists(query: String): List<ArtistHit> = withContext(Dispatchers.IO) {
        val extractor = ServiceList.YouTube.getSearchExtractor(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS),
            ""
        )
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<ChannelInfoItem>()
            .map {
                ArtistHit(
                    name = (it.name ?: "").replace(" - Topic", "").trim(),
                    cover = bigCover(it.thumbnails.maxByOrNull { t -> t.height }?.url)
                )
            }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.lowercase() }
            .take(10)
    }

    // albums (YouTube Music "Albums" search)
    suspend fun searchAlbums(query: String): List<Album> = withContext(Dispatchers.IO) {
        val extractor = ServiceList.YouTube.getSearchExtractor(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS),
            ""
        )
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<PlaylistInfoItem>()
            .filter {
                val title = it.name ?: ""
                val artist = (it.uploaderName ?: "").trim()
                artist.isNotEmpty() &&
                    !isCompilation(title, artist) &&
                    !Regex("\\bhits\\b", RegexOption.IGNORE_CASE).containsMatchIn(title) &&
                    !junkChannel.containsMatchIn(artist) &&
                    (it.streamCount <= 0L || it.streamCount <= 45L)
            }
            .map {
                Album(
                    title = it.name ?: "Album",
                    artist = it.uploaderName ?: "",
                    url = it.url,
                    cover = bigCover(it.thumbnails.maxByOrNull { t -> t.height }?.url)
                )
            }
    }

    // all songs of an album / playlist link (used by Album page and Import playlist)
    suspend fun playlistSongs(url: String, maxItems: Int = 200): PlaylistData =
        withContext(Dispatchers.IO) {
            val info = PlaylistInfo.getInfo(url)
            val out = ArrayList<Song>()
            for (item in info.relatedItems) {
                if (item is StreamInfoItem) out.add(toSong(item))
            }
            var next = info.nextPage
            var guard = 0
            while (next != null && out.size < maxItems && guard < 12) {
                val page = PlaylistInfo.getMoreItems(ServiceList.YouTube, url, next)
                for (item in page.items) {
                    if (item is StreamInfoItem) out.add(toSong(item))
                }
                next = page.nextPage
                guard++
            }
            PlaylistData(
                name = info.name ?: "Playlist",
                artist = (info.uploaderName ?: "").ifBlank { out.firstOrNull()?.artist ?: "" },
                cover = out.firstOrNull()?.cover,
                songs = out
            )
        }

    // music-only "similar songs": YouTube Music catalog songs by the same artist
    private suspend fun similarMusic(artist: String): List<Song> {
        val a = artist.removeSuffix(" - Topic").trim()
        if (a.isBlank()) return emptyList()
        return try {
            search(a + " songs")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun pickAudio(info: StreamInfo, q: Int): String {
        val streams = info.audioStreams
        val progressive = streams.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val pool = if (progressive.isNotEmpty()) progressive else streams
        val known = pool.filter { it.averageBitrate > 0 }
        val sorted = (if (known.isNotEmpty()) known else pool).sortedBy { it.averageBitrate }
        val best = when (q) {
            2 -> sorted.firstOrNull()
            1 -> sorted.getOrNull(sorted.size / 2)
            else -> sorted.lastOrNull()
        } ?: throw IllegalStateException("Is song ki audio nahi mili")
        return best.content
    }

    // only what is needed to start playing; no waiting for the similar-songs search
    suspend fun resolveAudio(videoUrl: String, q: Int = quality): FastAudio = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(videoUrl)
        FastAudio(pickAudio(info, q), info)
    }

    // similar songs for the song that is already playing (runs in the background)
    suspend fun relatedFor(info: StreamInfo, videoUrl: String, artistHint: String): List<Song> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val who = artistHint.ifBlank { info.uploaderName ?: "" }
                val similarJob = async { similarMusic(who) }
                try {
                    val fromPage = info.relatedItems
                        .filterIsInstance<StreamInfoItem>()
                        .filter { it.duration in 60L..600L }
                        .map { toSong(it) }
                        .filter { looksLikeMusicChannel(it.artist) }
                        .filter { !isCompilation(it.title, it.artist) }
                        .filter { !officialOnly || isOfficial(it) }
                    val similar: List<Song> = similarJob.await()
                    val mixed = ArrayList<Song>()
                    val n = maxOf(similar.size, fromPage.size)
                    for (i in 0 until n) {
                        similar.getOrNull(i)?.let { mixed.add(it) }
                        fromPage.getOrNull(i)?.let { mixed.add(it) }
                    }
                    mixed.filter { it.url != videoUrl }.distinctBy { it.url }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList<Song>()
                }
            }
        }

    suspend fun resolve(
        videoUrl: String,
        q: Int = quality,
        withRelated: Boolean = true,
        artistHint: String = ""
    ): Resolved = withContext(Dispatchers.IO) {
        coroutineScope {
            // start the "similar songs" search at the same time as the stream lookup
            var similarJob = if (withRelated && artistHint.isNotBlank()) {
                async { similarMusic(artistHint) }
            } else null

            val info = StreamInfo.getInfo(videoUrl)
            val streams = info.audioStreams
            val progressive = streams.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            val pool = if (progressive.isNotEmpty()) progressive else streams
            val known = pool.filter { it.averageBitrate > 0 }
            val sorted = (if (known.isNotEmpty()) known else pool).sortedBy { it.averageBitrate }
            val best = when (q) {
                2 -> sorted.firstOrNull()
                1 -> sorted.getOrNull(sorted.size / 2)
                else -> sorted.lastOrNull()
            } ?: throw IllegalStateException("Is song ki audio nahi mili")

            if (withRelated && similarJob == null) {
                val name = info.uploaderName ?: ""
                similarJob = async { similarMusic(name) }
            }

            val related: List<Song> = if (!withRelated) {
                emptyList<Song>()
            } else try {
                // from the YouTube page: keep only real music uploads (Topic / VEVO)
                val fromPage = info.relatedItems
                    .filterIsInstance<StreamInfoItem>()
                    .filter { it.duration in 60L..600L }
                    .map { toSong(it) }
                    .filter { looksLikeMusicChannel(it.artist) }
                    .filter { !isCompilation(it.title, it.artist) }
                    .filter { !officialOnly || isOfficial(it) }
                val similar: List<Song> = similarJob?.await() ?: emptyList<Song>()
                // mix: one from the artist, one from similar music
                val mixed = ArrayList<Song>()
                val n = maxOf(similar.size, fromPage.size)
                for (i in 0 until n) {
                    similar.getOrNull(i)?.let { mixed.add(it) }
                    fromPage.getOrNull(i)?.let { mixed.add(it) }
                }
                mixed.filter { it.url != videoUrl }.distinctBy { it.url }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList<Song>()
            }
            Resolved(best.content, related)
        }
    }

    private fun toSong(item: StreamInfoItem): Song = Song(
        title = item.name ?: "Unknown",
        artist = item.uploaderName ?: "",
        url = item.url,
        cover = bigCover(item.thumbnails.maxByOrNull { it.height }?.url),
        durationSec = item.duration
    )

    private fun bigCover(url: String?): String? {
        if (url == null) return null
        if (!(url.contains("googleusercontent.com") || url.contains("ggpht.com"))) return url
        return url.replace(Regex("=(?:w\\d+-h\\d+|s\\d+)[^/]*$"), "=w600-h600-l90-rj")
    }
}
