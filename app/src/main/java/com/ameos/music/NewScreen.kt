package com.ameos.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// A new album / single / playlist on the New page.
data class NewRelease(
    val kind: String,        // "album", "playlist" or "song"
    val id: String,
    val title: String,
    val artist: String,
    val cover: String?,
    val album: Album? = null // set by the search fallback
)

// The New page data. It is loaded once and kept, so switching tabs does not load it again.
object NewFeed {
    var releases by mutableStateOf<List<NewRelease>>(emptyList())
    var songs by mutableStateOf<List<Song>>(emptyList())
    var loading by mutableStateOf(false)
    var loaded by mutableStateOf(false)

    suspend fun load(force: Boolean = false) {
        if (loading || (loaded && !force)) return
        loading = true
        try {
            val rel = ArrayList<NewRelease>()
            val sg = ArrayList<Song>()
            withContext(Dispatchers.IO) {
                for (bid in listOf("FEmusic_new_releases_albums", "FEmusic_new_releases")) {
                    try {
                        val root = Account.browseAnon(bid)
                        // album / single cards (grid or carousel)
                        val two = ArrayList<JSONObject>()
                        Account.collect(root, "musicTwoRowItemRenderer", two)
                        for (item in two) {
                            val r = releaseFrom(item) ?: continue
                            if (rel.none { it.id == r.id }) rel.add(r)
                        }
                        // song rows
                        val rows = ArrayList<JSONObject>()
                        Account.collect(root, "musicResponsiveListItemRenderer", rows)
                        for (row in rows) {
                            val s = Account.toSong(row) ?: continue
                            if (sg.none { it.url == s.url }) sg.add(s)
                        }
                    } catch (e: Exception) {
                    }
                }
                // fallback: the page could not be read, so use search
                if (rel.isEmpty()) {
                    try {
                        for (a in MusicRepo.searchAlbums("new album 2026").take(12)) {
                            rel.add(NewRelease("album", a.url, a.title, a.artist, a.cover, a))
                        }
                    } catch (e: Exception) {
                    }
                }
                if (sg.size < 4) {
                    for (q in listOf("new songs 2026", "new hindi songs 2026")) {
                        try {
                            for (s in MusicRepo.search(q)) if (sg.none { it.url == s.url }) sg.add(s)
                        } catch (e: Exception) {
                        }
                    }
                }
            }
            releases = rel.take(14)
            songs = sg.take(24)
            loaded = releases.isNotEmpty() || songs.isNotEmpty()
        } finally {
            loading = false
        }
    }

    private fun releaseFrom(item: JSONObject): NewRelease? {
        val title = Account.runsText(item.optJSONObject("title")?.optJSONArray("runs")).trim()
        if (title.isBlank()) return null
        val sub = Account.runsText(item.optJSONObject("subtitle")?.optJSONArray("runs")).trim()
        val cover = Account.coverOf(item)
        val nav = item.optJSONObject("navigationEndpoint") ?: return null
        val browse = nav.optJSONObject("browseEndpoint") ?: return null
        val id = browse.optString("browseId")
        if (id.isBlank() || id.startsWith("UC")) return null
        val kind = if (id.startsWith("MPRE")) "album" else "playlist"
        return NewRelease(kind, id, title, YtHome.artistOf(sub), cover)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) { NewFeed.load() }
    val w = LocalConfiguration.current.screenWidthDp
    val pageW = if (w >= 600) 520.dp else (w - 56).dp
    val relState = rememberLazyListState()
    val songState = rememberLazyListState()
    val tick = rememberTick()

    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 210.dp)) {
        item {
            Text(
                "New", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Pal.fg,
                letterSpacing = (-0.8).sp, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 14.dp)
            )
        }
        if (NewFeed.releases.isEmpty() && NewFeed.loading) {
            item {
                val b = shimmerBrush()
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Box(Modifier.width(110.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(b))
                    Box(Modifier.padding(top = 8.dp).width(220.dp).height(20.dp).clip(RoundedCornerShape(6.dp)).background(b))
                    Box(
                        Modifier.padding(top = 12.dp).fillMaxWidth().aspectRatio(1.6f)
                            .clip(RoundedCornerShape(24.dp)).background(b)
                    )
                }
            }
        }
        if (NewFeed.releases.isNotEmpty()) {
            item {
                LazyRow(
                    state = relState,
                    flingBehavior = rememberSnapFlingBehavior(relState),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(NewFeed.releases) { r ->
                        Column(
                            Modifier
                                .width(pageW)
                                .pressScale(0.98f)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    tick()
                                    openRelease(vm, r)
                                }
                        ) {
                            Text(
                                "NEW RELEASE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp, color = Pal.textDim
                            )
                            Text(
                                r.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Pal.fg,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                            Text(
                                r.artist.ifBlank { " " }, fontSize = 17.sp, color = Pal.textDim,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            val shape = RoundedCornerShape(24.dp)
                            Box(
                                Modifier
                                    .padding(top = 10.dp)
                                    .fillMaxWidth()
                                    .aspectRatio(1.6f)
                                    .clip(shape)
                            ) {
                                Cover(r.cover, Modifier.fillMaxWidth().aspectRatio(1.6f), shape, hiRes = true)
                                Box(Modifier.matchParentSizeCompat().border(1.dp, GlassEdge, shape))
                            }
                        }
                    }
                }
            }
        }
        if (NewFeed.songs.isNotEmpty()) {
            item {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "New Songs", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Pal.fg,
                        letterSpacing = (-0.4).sp
                    )
                    Icon(
                        Icons.Filled.ChevronRight, null, tint = Pal.textDim,
                        modifier = Modifier.padding(start = 4.dp).size(26.dp)
                    )
                }
            }
            item {
                val pages = NewFeed.songs.chunked(4)
                LazyRow(
                    state = songState,
                    flingBehavior = rememberSnapFlingBehavior(songState),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(pages) { page ->
                        Column(Modifier.width(if (w >= 600) 440.dp else (w - 48).dp)) {
                            page.forEachIndexed { i, s ->
                                NewSongRow(vm, s, NewFeed.songs)
                                if (i < page.size - 1) {
                                    Box(
                                        Modifier.fillMaxWidth().padding(start = 64.dp)
                                            .height(1.dp).background(Stroke)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (NewFeed.loaded.not() && !NewFeed.loading) {
            item {
                EmptyState(
                    Icons.Filled.CloudOff, "Could not load new music",
                    "Check your internet and try again.", "Try again"
                ) { }
            }
        }
    }
    // the "Try again" button above has no scope of its own, so a tiny effect retries when asked
    LaunchedEffect(NewFeed.loaded, NewFeed.loading) {
        // nothing to do: kept so the screen reloads when the tab is opened again after a failure
        if (!NewFeed.loaded && !NewFeed.loading) NewFeed.load()
    }
}

// Modifier.matchParentSize only exists inside a BoxScope, so this is a plain full size box helper
@Composable
private fun Modifier.matchParentSizeCompat(): Modifier = this.fillMaxWidth().aspectRatio(1.6f)

private fun openRelease(vm: MainViewModel, r: NewRelease) {
    val al = r.album
    if (al != null) vm.openAlbum(al)
    else vm.openYtPlaylist(YtPlaylist(r.id, r.title, r.artist, r.cover))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewSongRow(vm: MainViewModel, s: Song, list: List<Song>) {
    val playing = vm.current?.url == s.url
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { vm.play(s, list) },
                onLongClick = { vm.songMenu = s }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(s.cover, Modifier.size(52.dp), RoundedCornerShape(12.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                s.title, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                color = if (playing) Pal.accent else Pal.fg,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                artistName(s.artist), fontSize = 14.sp, color = Pal.textDim,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable { vm.songMenu = s },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MoreHoriz, null, tint = Pal.textDim)
        }
    }
}
