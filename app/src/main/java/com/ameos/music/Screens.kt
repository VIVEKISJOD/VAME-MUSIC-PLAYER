package com.ameos.music

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenPad = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 200.dp)

@Composable
fun ScreenTitle(text: String, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            modifier = Modifier.weight(1f))
        trailing()
    }
}

// ---------------- HOME ----------------

@Composable
fun HomeScreen(vm: MainViewModel) {
    val hero = vm.current ?: vm.recommended.firstOrNull()
    val recs = vm.recommended.drop(1).take(4)
    val lastAlbums = if (vm.played.size > 1) vm.played.toList() else vm.recommended.drop(5)

    LazyColumn(contentPadding = ScreenPad) {
        item {
            ScreenTitle(vm.title.ifBlank { "Music" }) {
                Icon(
                    Icons.Filled.AccountCircle, null, tint = Color.White,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { vm.showSettings = true }
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Music", "Podcasts", "Playlists").forEachIndexed { i, t ->
                    val chipBg by animateColorAsState(
                        if (i == vm.homeFilter) Color(0xFF7A1A24) else Color(0x22FFFFFF),
                        label = "chip"
                    )
                    Text(
                        t, fontSize = 14.sp, color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(chipBg)
                            .clickable { vm.pickFilter(i) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
        if (vm.homeFilter == 0) {
        item { SectionLabel("Last Played") }
        item {
            if (hero != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .pressScale(0.98f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0x33FF6B7A), RoundedCornerShape(24.dp))
                        .clickable { vm.play(hero, vm.recommended) }
                ) {
                    Cover(hero.cover, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC200A0E)))
                        )
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(hero.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp))
                        Text(hero.artist, fontSize = 14.sp, color = Color(0xFFE6BDC0), maxLines = 1)
                    }
                    Row(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x66FFFFFF))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Replay", color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF2A1215)))
            }
        }
        item { SectionLabel("Recommended Albums", "NEW") }
        item {
            if (vm.recommendedAlbums.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    vm.recommendedAlbums.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { a ->
                                Row(
                                    Modifier.weight(1f).pressScale().clickable { vm.openAlbum(a) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Cover(a.cover, Modifier.size(48.dp), RoundedCornerShape(8.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(a.title, fontSize = 15.sp, color = Color.White,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(a.artist, fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                val rows = recs.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { s ->
                                Row(
                                    Modifier.weight(1f).pressScale().clickable { vm.play(s, vm.recommended) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Cover(s.cover, Modifier.size(48.dp), RoundedCornerShape(8.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(s.title, fontSize = 15.sp, color = Color.White,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(s.artist, fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item { SectionLabel("Last played albums") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(lastAlbums) { s ->
                    Column(Modifier.width(150.dp).pressScale().clickable { vm.play(s, lastAlbums) }) {
                        Cover(s.cover, Modifier.size(150.dp), RoundedCornerShape(16.dp))
                        Text(s.title, fontSize = 14.sp, color = Color.White, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        Text(s.artist, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (vm.suggestions.isNotEmpty()) {
            item { SectionLabel("Suggested for you") }
            items(vm.suggestions.take(6)) { s -> SongRow(s, vm, vm.suggestions) }
        }
        }
        if (vm.homeFilter == 1) {
            item { SectionLabel("Songs for you") }
            items(vm.recommended) { s -> SongRow(s, vm, vm.recommended) }
        }
        if (vm.homeFilter == 2) {
            item { SectionLabel("Podcasts") }
            if (vm.podcasts.isEmpty()) {
                item { Text("Loading...", color = Color(0xFFE6BDC0)) }
            }
            items(vm.podcasts) { s -> SongRow(s, vm, vm.podcasts) }
        }
        if (vm.homeFilter == 3) {
            item { SectionLabel("Playlists") }
            item {
                TwoColumn(listOf("Daily", "Bangers", "Evening")) { n, m ->
                    LibItem(m, n, null, null) {
                        vm.searchFor("$n songs")
                        vm.tab = 1
                    }
                }
            }
        }
    }
}

// ---------------- SEARCH ----------------

private val TileColors = listOf(
    Color(0xFF3B3B3B), Color(0xFF2E5A6B), Color(0xFF8A2B6B), Color(0xFF5A3A12)
)

@Composable
private fun GenreTileView(tile: GenreTile, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(150.dp)
            .pressScale()
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .clickable { onClick() }
    ) {
        if (tile.covers.size >= 4) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    Cover(tile.covers[0], Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(0.dp))
                    Cover(tile.covers[1], Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(0.dp))
                }
                Row(Modifier.weight(1f)) {
                    Cover(tile.covers[2], Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(0.dp))
                    Cover(tile.covers[3], Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(0.dp))
                }
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x22000000), Color(0xCC000000)))
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(tile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (tile.artists.isNotEmpty()) {
                Text(tile.artists, fontSize = 11.sp, color = Color(0xFFD9C0C2), maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun AlbumCard(a: Album, vm: MainViewModel) {
    Column(Modifier.width(140.dp).pressScale().clickable { vm.openAlbum(a) }) {
        Cover(a.cover, Modifier.size(140.dp), RoundedCornerShape(16.dp))
        Text(a.title, fontSize = 14.sp, color = Color.White, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(a.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SearchScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) { vm.loadTiles() }
    LazyColumn(contentPadding = ScreenPad) {
        item { ScreenTitle("Search") }
        item {
            TextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                placeholder = { Text("Search for Music...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x33FFFFFF),
                    unfocusedContainerColor = Color(0x33FFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (vm.loading) {
            item {
                Text("Loading...", color = Color(0xFFE6BDC0), modifier = Modifier.padding(top = 12.dp))
            }
        }
        if (!vm.searched) {
            item { SectionLabel("Recommended") }
            item {
                val list = if (vm.tiles.isNotEmpty()) vm.tiles
                else listOf("Rap", "Alternative/Indie", "Pop", "Hip-Hop").map { GenreTile(it, emptyList(), "") }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    list.chunked(2).forEachIndexed { r, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEachIndexed { c, t ->
                                GenreTileView(
                                    t, TileColors[(r * 2 + c) % TileColors.size], Modifier.weight(1f)
                                ) { vm.searchFor(t.name + " music") }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            if (vm.albumResults.isNotEmpty()) {
                item { SectionLabel("Albums") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(vm.albumResults) { a -> AlbumCard(a, vm) }
                    }
                }
                item { SectionLabel("Songs") }
            }
            items(vm.results) { s -> SongRow(s, vm, vm.results) }
        }
    }
}

// ---------------- YOUR TYPE ----------------

@Composable
fun YourTypeScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) {
        if (vm.discover.isEmpty()) vm.loadDiscover()
    }
    val collage = (vm.discover + vm.played + vm.liked + vm.recommended)
        .mapNotNull { it.cover }.distinct().take(8)
    LazyColumn(contentPadding = ScreenPad) {
        item {
            ScreenTitle("Your Type") {
                RoundIcon(Icons.Filled.Refresh) { vm.loadDiscover() }
            }
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                if (collage.size >= 8) {
                    Column(Modifier.fillMaxSize().blur(18.dp).alpha(0.55f)) {
                        Row(Modifier.weight(1f)) {
                            for (i in 0 until 4) {
                                Cover(collage[i], Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(0.dp))
                            }
                        }
                        Row(Modifier.weight(1f)) {
                            for (i in 4 until 8) {
                                Cover(collage[i], Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(0.dp))
                            }
                        }
                    }
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0x33000000), Color(0xAA0E0B0B)))
                    )
                )
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Discover new Music", fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White, textAlign = TextAlign.Center)
                    Text("Picked for you", fontSize = 13.sp, color = Color(0xFFD9C0C2),
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (vm.discoverReason.isNotEmpty() && vm.discover.isNotEmpty()) {
            item {
                Text(
                    vm.discoverReason, fontSize = 13.sp, color = Color(0xFF8C7C7E),
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                )
            }
        }
        if (vm.discover.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("We're ready, user.", fontSize = 16.sp, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.size(48.dp).pressScale(0.9f).clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .clickable { vm.loadDiscover() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White,
                            modifier = Modifier.size(30.dp))
                    }
                }
            }
        } else {
            items(vm.discover) { s -> SongRow(s, vm, vm.discover) }
        }
    }
}

// ---------------- LIBRARY ----------------

@Composable
private fun <T> TwoColumn(list: List<T>, cell: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        list.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { c -> cell(c, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LibItem(
    modifier: Modifier, title: String, sub: String?, cover: String?,
    circle: Boolean = false, onClick: () -> Unit
) {
    Row(modifier.clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        Cover(cover, Modifier.size(46.dp), if (circle) CircleShape else RoundedCornerShape(10.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 15.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) {
                Text(sub, fontSize = 11.sp, color = Color(0xFF8C7C7E), maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun LibraryScreen(vm: MainViewModel) {
    val artists = vm.likedArtists.take(6)
    val songs = vm.liked.take(3)
    val plItems: List<Playlist?> = vm.playlists.map { it }.plus(listOf<Playlist?>(null))
    LazyColumn(contentPadding = ScreenPad) {
        item { ScreenTitle("Library") }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressScale()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x22FFFFFF))
                    .clickable {
                        vm.refreshDownloadBytes()
                        vm.downloadsOpen = true
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Download, null, tint = Color(0xFFFFB3B9))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Downloads", fontSize = 16.sp, color = Color.White)
                    Text(
                        vm.downloads.size.toString() + " songs for offline",
                        fontSize = 12.sp, color = Color(0xFF8C7C7E)
                    )
                }
            }
        }
        item { SectionLabel("Playlists") }
        item {
            TwoColumn(plItems) { p, m ->
                if (p == null) {
                    Column(m) {
                        Row(
                            Modifier.clickable {
                                vm.editPlaylistId = null
                                vm.pendingSong = null
                                vm.showPlaylistDialog = true
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x22FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Add, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("New playlist", fontSize = 15.sp, color = Color.White)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.clickable {
                                vm.importStatus = ""
                                vm.importedId = null
                                vm.showImportDialog = true
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x22FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.FileDownload, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("Import playlist", fontSize = 15.sp, color = Color.White)
                        }
                    }
                } else {
                    LibItem(m.pressScale(), p.name, p.songs.size.toString() + " songs", p.shownCover()) {
                        vm.openPlaylist(p.id)
                    }
                }
            }
        }
        item { SectionLabel("Liked Artists") }
        item {
            if (artists.isEmpty()) {
                Text(
                    "Open an artist page (... menu > Go to artist) and tap Like artist",
                    fontSize = 13.sp, color = Color(0xFF8C7C7E)
                )
            } else {
                TwoColumn(artists) { a, m ->
                    LibItem(m.pressScale(), a.name, null, a.cover, true) {
                        vm.openArtist(a.name)
                    }
                }
            }
        }
        item { SectionLabel("Liked Songs") }
        item {
            if (songs.isEmpty()) {
                Text("No liked songs yet", fontSize = 13.sp, color = Color(0xFF8C7C7E))
            } else {
                val cells: List<Song?> = songs.map { it }.plus(listOf<Song?>(null))
                TwoColumn(cells) { s, m ->
                    if (s == null) {
                        Box(
                            m
                                .pressScale()
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, Color(0x88FFFFFF), RoundedCornerShape(50))
                                .clickable { vm.likedOpen = true }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("View More", fontSize = 15.sp, color = Color.White)
                        }
                    } else {
                        LibItem(m.pressScale(), s.title, s.artist, s.cover) {
                            vm.play(s, vm.liked.toList())
                        }
                    }
                }
            }
        }
    }
}
