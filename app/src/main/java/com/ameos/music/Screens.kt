package com.ameos.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.runtime.setValue
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

// Top right button: your Google photo when signed in, otherwise the plain account icon
@Composable
fun ProfileButton(vm: MainViewModel, size: androidx.compose.ui.unit.Dp, tappable: Boolean) {
    val photo = vm.ytPhoto
    val base = Modifier.size(size).clip(CircleShape)
    val mod = if (tappable) base.clickable { vm.showSettings = true } else base
    if (vm.ytSignedIn && photo != null) {
        Cover(photo, mod, CircleShape)
    } else {
        Icon(Icons.Filled.AccountCircle, null, tint = Color.White, modifier = mod)
    }
}

// A row of big song cards (used for the genre shelves on Home)
@Composable
private fun SongShelf(vm: MainViewModel, songs: List<Song>) {
    val w = if (LocalConfiguration.current.screenWidthDp >= 600) 200.dp else 160.dp
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(songs) { s ->
            Column(Modifier.width(w).pressScale().clickable { vm.play(s, songs) }) {
                Cover(s.cover, Modifier.size(w), RoundedCornerShape(18.dp), hiRes = true)
                Text(s.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                Text(s.artist, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------- HOME ----------------

@Composable
fun HomeScreen(vm: MainViewModel) {
    val recs = vm.recommended.take(6)
    val lastAlbums = if (vm.played.size > 1) vm.played.toList() else vm.recommended.drop(5)

    LazyColumn(contentPadding = ScreenPad) {
        item {
            ScreenTitle(vm.title.ifBlank { "Music" }) {
                ProfileButton(vm, 34.dp, true)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Music", "Podcasts", "Playlists").forEachIndexed { i, t ->
                    val chipBg by animateColorAsState(
                        if (i == vm.homeFilter) Pal.accentDeep else Color(0x22FFFFFF),
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
        // Liked songs shelf (replaces the old Last Played banner)
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(if (vm.liked.isNotEmpty()) "Liked songs" else "Songs for you")
                if (vm.liked.isNotEmpty()) {
                    Text(
                        "See all", fontSize = 13.sp, color = Pal.textDim,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { vm.likedOpen = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        item {
            val shelf = if (vm.liked.isNotEmpty()) vm.liked.toList() else vm.recommended.take(12)
            val big = LocalConfiguration.current.screenWidthDp >= 600
            val sz = if (big) 230.dp else 184.dp
            if (shelf.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().height(sz).clip(RoundedCornerShape(24.dp))
                        .glassFill().border(1.dp, Stroke, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tap the heart on a song and it shows up here", fontSize = 14.sp, color = Pal.textDim)
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(shelf) { s ->
                        Column(
                            Modifier.width(sz).appear().pressScale(0.97f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { vm.play(s, shelf) }
                        ) {
                            Box(Modifier.size(sz)) {
                                Cover(s.cover, Modifier.fillMaxSize(), RoundedCornerShape(20.dp), hiRes = true)
                                Box(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, GlassEdge, RoundedCornerShape(20.dp))
                                )
                                Box(
                                    Modifier.align(Alignment.BottomEnd).padding(8.dp).size(36.dp)
                                        .clip(CircleShape).glass(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Text(s.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                            Text(s.artist, fontSize = 12.sp, color = Pal.textDim, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item { SectionLabel("Recommended Albums", "NEW") }
        item {
            if (vm.recommendedAlbums.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    vm.recommendedAlbums.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            pair.forEach { a ->
                                Row(
                                    Modifier.weight(1f).pressScale().clickable { vm.openAlbum(a) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Cover(a.cover, Modifier.size(66.dp), RoundedCornerShape(12.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(a.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(a.artist, fontSize = 14.sp,
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
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    rows.forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            pair.forEach { s ->
                                Row(
                                    Modifier.weight(1f).pressScale().clickable { vm.play(s, vm.recommended) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Cover(s.cover, Modifier.size(66.dp), RoundedCornerShape(12.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(s.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(s.artist, fontSize = 14.sp,
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
        if (UiOpts.homeLastAlbums) item { SectionLabel("Last played albums") }
        if (UiOpts.homeLastAlbums) item {
            val lw = if (LocalConfiguration.current.screenWidthDp >= 600) 220.dp else 176.dp
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(lastAlbums) { s ->
                    Column(Modifier.width(lw).pressScale().clickable { vm.play(s, lastAlbums) }) {
                        Cover(s.cover, Modifier.size(lw), RoundedCornerShape(20.dp), hiRes = true)
                        Text(s.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        Text(s.artist, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        val picks = vm.recommended.drop(4).take(12)
        if (UiOpts.homeQuick && picks.isNotEmpty()) {
            item { SectionLabel("Quick picks") }
            item {
                val qw = if (LocalConfiguration.current.screenWidthDp >= 600) 220.dp else 176.dp
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(picks) { s ->
                        Column(Modifier.width(qw).pressScale().clickable { vm.play(s, picks) }) {
                            Cover(s.cover, Modifier.size(qw), RoundedCornerShape(20.dp), hiRes = true)
                            Text(s.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                            Text(s.artist, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        for (sh in (if (UiOpts.homeShelves) vm.genreShelves else emptyList())) {
            item { SectionLabel(sh.name) }
            item { SongShelf(vm, sh.songs) }
        }
        if (UiOpts.homeArtists && vm.likedArtists.isNotEmpty()) {
            item { SectionLabel("Your artists") }
            item {
                val aw = if (LocalConfiguration.current.screenWidthDp >= 600) 130.dp else 104.dp
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(vm.likedArtists.toList()) { a ->
                        Column(
                            Modifier.width(aw).pressScale().clickable { vm.openArtist(a.name) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Cover(a.cover, Modifier.size(aw), CircleShape, hiRes = true)
                            Text(a.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        }
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
                item { Text("Loading...", color = Pal.textDim) }
            }
            items(vm.podcasts) { s -> SongRow(s, vm, vm.podcasts) }
        }
        if (vm.homeFilter == 3) {
            item { SectionLabel("Playlists (" + (vm.playlists.size + vm.ytPlaylistsShown.size) + ")") }
            item {
                val hw = LocalConfiguration.current.screenWidthDp
                val cols = if (hw >= 600) 4 else 2
                val hl = ArrayList<LibTile>()
                hl.add(LibTile("New playlist", "Save your own mix", null, Icons.Filled.Add, null) {
                    vm.editPlaylistId = null
                    vm.pendingSong = null
                    vm.showPlaylistDialog = true
                })
                hl.add(LibTile("Import playlist", "From a playlist link", null, Icons.Filled.FileDownload, null) {
                    vm.importStatus = ""
                    vm.importedId = null
                    vm.showImportDialog = true
                })
                hl.add(LibTile(
                    "Liked Songs", vm.liked.size.toString() + " songs", null,
                    Icons.Filled.Favorite, listOf(Color(0xFF8B6CF0), Color(0xFFEF5DA8))
                ) { vm.likedOpen = true })
                for (p in vm.playlists) {
                    hl.add(LibTile(p.name, p.songs.size.toString() + " songs", p.shownCover(), Icons.Filled.LibraryMusic, null) {
                        vm.openPlaylist(p.id)
                    })
                }
                for (yp in vm.ytPlaylistsShown) {
                    hl.add(LibTile(yp.title, yp.subtitle.ifBlank { "Playlist" }, yp.cover, Icons.Filled.LibraryMusic, null) {
                        vm.openYtPlaylist(yp)
                    })
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (rowTiles in hl.chunked(cols)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            for (t in rowTiles) PlTile(t, Modifier.weight(1f))
                            for (k in rowTiles.size until cols) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (vm.playlists.isEmpty() && vm.ytPlaylistsShown.isEmpty()) {
                item {
                    Text(
                        "No playlists yet. Make one in Library, or sign in there to see your YouTube Music playlists.",
                        fontSize = 13.sp, color = Pal.textDim, modifier = Modifier.padding(top = 14.dp)
                    )
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
                Text(tile.artists, fontSize = 11.sp, color = Pal.textDim, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun AlbumCard(a: Album, vm: MainViewModel) {
    Column(Modifier.width(140.dp).pressScale().clickable { vm.openAlbum(a) }) {
        Cover(a.cover, Modifier.size(140.dp), RoundedCornerShape(16.dp), hiRes = true)
        Text(a.title, fontSize = 14.sp, color = Color.White, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(a.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private data class Mood(val name: String, val sub: String, val query: String, val color: Color)

private val Moods = listOf(
    Mood("Chill", "Coffee shop blend", "chill coffee shop songs", Color(0xFF8FA8E0)),
    Mood("Commute", "Feel-good hip hop and R&B", "feel good hip hop r&b songs", Color(0xFFD9A93A)),
    Mood("Energize", "Pump-up pop", "pump up pop songs", Color(0xFFD6C36A)),
    Mood("Feel good", "Sunshine indie", "sunshine indie songs", Color(0xFF8FDB93)),
    Mood("Focus", "Lofi loft", "lofi focus beats", Color(0xFF9E9E9E)),
    Mood("Gaming", "Gaming hits", "gaming music mix", Color(0xFF707070)),
    Mood("Party", "Bollywood dance", "bollywood dance songs", Color(0xFF8E63D6)),
    Mood("Romance", "Bollywood romantic moments", "bollywood romantic songs", Color(0xFFB01E14)),
    Mood("Sad", "Bollywood melancholy", "bollywood sad songs", Color(0xFF7C7C7C)),
    Mood("Sleep", "Classical for sleeping", "calm piano sleep music", Color(0xFF6A3FC4))
)

// colourful mood tile: solid colour strip on the left, tinted cover on the right
@Composable
private fun MoodTile(m: Mood, cover: String?, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .height(150.dp)
            .appear()
            .pressScale(0.97f)
            .clip(shape)
            .clickable { onClick() }
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.weight(0.28f).fillMaxHeight().background(
                    Brush.verticalGradient(
                        listOf(m.color, androidx.compose.ui.graphics.lerp(m.color, Color.White, 0.32f))
                    )
                )
            )
            Box(Modifier.weight(0.72f).fillMaxHeight()) {
                Cover(cover, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
                Box(Modifier.fillMaxSize().background(m.color.copy(alpha = 0.58f)))
                Text(
                    m.sub, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.92f), textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 8.dp, end = 8.dp, bottom = 14.dp)
                )
            }
        }
        Text(
            m.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 14.dp)
        )
        Box(Modifier.fillMaxSize().border(1.dp, GlassEdge, shape))
    }
}

// Explore (was Search): big title, search box, colourful mood tiles
@Composable
fun ExploreScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) { vm.loadTiles() }
    // Back from search results returns to the mood tiles first
    BackHandler(enabled = vm.searched) { vm.clearSearch() }
    val cols = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2
    val pool = (vm.recommended + vm.discover + vm.played.toList() + vm.liked.toList())
        .mapNotNull { it.cover }.distinct()
    LazyColumn(contentPadding = ScreenPad) {
        item {
            ScreenTitle("Explore") {
                ProfileButton(vm, 34.dp, true)
            }
        }
        item {
            TextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                placeholder = { Text("Search for Music...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (vm.searched) {
                        IconButton(onClick = { vm.clearSearch() }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                },
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
                Text("Loading...", color = Pal.textDim, modifier = Modifier.padding(top = 12.dp))
            }
        }
        if (!vm.searched) {
            item { Spacer(Modifier.height(10.dp)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Moods.chunked(cols).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEachIndexed { c, m ->
                                val idx = r * cols + c
                                val cover = if (pool.isEmpty()) null else pool[(idx * 3) % pool.size]
                                MoodTile(m, cover, Modifier.weight(1f)) { vm.searchFor(m.query) }
                            }
                            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            if (vm.tiles.isNotEmpty()) {
                item { SectionLabel("Your genres") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        vm.tiles.chunked(2).forEachIndexed { r, pair ->
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
                    Text("Picked for you", fontSize = 13.sp, color = Pal.textDim,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (vm.discoverReason.isNotEmpty() && vm.discover.isNotEmpty()) {
            item {
                Text(
                    vm.discoverReason, fontSize = 13.sp, color = Pal.muted,
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

// grid with big tiles: 2 columns on a phone, 4 on a tablet
@Composable
private fun <T> BigGrid(list: List<T>, cell: @Composable (T, Modifier) -> Unit) {
    val cols = if (LocalConfiguration.current.screenWidthDp >= 600) 4 else 2
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        list.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { c -> cell(c, Modifier.weight(1f)) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BigTile(
    modifier: Modifier, title: String, sub: String, cover: String?, onClick: () -> Unit
) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).clickable { onClick() }) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Cover(cover, Modifier.fillMaxSize(), RoundedCornerShape(18.dp), hiRes = true)
            if (cover == null) {
                Icon(
                    Icons.Filled.LibraryMusic, null, tint = Color(0x55FFFFFF),
                    modifier = Modifier.align(Alignment.Center).size(44.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(sub, fontSize = 12.sp, color = Pal.muted, maxLines = 1)
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
                Text(sub, fontSize = 11.sp, color = Pal.muted, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---- Library (stats cards, quick rows, big tiles) ----

private data class LibTile(
    val title: String,
    val sub: String,
    val cover: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val grad: List<Color>?,
    val onClick: () -> Unit
)

@Composable
private fun PlTile(t: LibTile, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(modifier.appear().pressScale(0.97f).clip(RoundedCornerShape(24.dp)).clickable { t.onClick() }) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(
                    if (t.grad != null) Brush.linearGradient(t.grad) else Brush.verticalGradient(listOf(Pal.surface2, Pal.surface))
                )
                .border(1.dp, GlassEdge, shape),
            contentAlignment = Alignment.Center
        ) {
            if (t.cover != null) {
                Cover(t.cover, Modifier.fillMaxSize(), shape, hiRes = true)
            } else if (t.icon != null) {
                Icon(t.icon, null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
        }
        Text(
            t.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp)
        )
        Text(t.sub, fontSize = 14.sp, color = Pal.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LibRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onClick() }
                .padding(vertical = 14.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 21.sp, color = Color.White)
                if (sub != null) Text(sub, fontSize = 13.sp, color = Pal.muted)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Pal.muted)
        }
        Box(Modifier.fillMaxWidth().padding(start = 52.dp).height(1.dp).background(Color(0x22FFFFFF)))
    }
}

@Composable
private fun StatCard(width: androidx.compose.ui.unit.Dp, label: String, bigText: String, bigSize: Int,
                     caption: String, bottomLeft: String, bottomLeftSub: String, bottomRight: String?,
                     bottomRightSub: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .width(width)
            .height(210.dp)
            .pressScale(0.98f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Pal.accentDeep.copy(alpha = 0.55f), Pal.surface)))
            .glassFill()
            .border(1.dp, GlassEdge, shape)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Text(
            label, fontSize = 12.sp, color = Pal.textDim, letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Column(Modifier.align(Alignment.CenterStart).padding(top = 10.dp)) {
            Text(
                bigText, fontSize = bigSize.sp, fontWeight = FontWeight.Bold, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = (bigSize + 2).sp
            )
            Text(
                caption, fontSize = 13.sp, color = Pal.textDim, letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    bottomLeft, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, letterSpacing = 2.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(bottomLeftSub, fontSize = 13.sp, color = Pal.textDim, maxLines = 1)
            }
            if (bottomRight != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(bottomRightSub ?: "", fontSize = 10.sp, color = Pal.textDim, letterSpacing = 1.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text(bottomRight, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        letterSpacing = 2.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(vm: MainViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // signed in but the list is empty: load it when the Library opens
    LaunchedEffect(vm.ytSignedIn) {
        if (vm.ytSignedIn && vm.ytPlaylists.isEmpty()) vm.refreshYt()
    }
    var showAll by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val w = LocalConfiguration.current.screenWidthDp
    val big = w >= 600
    val tileSize = if (big) 190.dp else 160.dp
    val cardW = minOf((w * 0.82f).dp, 420.dp)
    val rec = androidx.compose.runtime.remember(vm.statMinutes, vm.statPlays) { vm.recap() }
    val name = vm.userName.ifBlank { vm.ytName }.ifBlank { vm.title }.ifBlank { "VAME" }.uppercase()
    val year = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    val since = java.text.SimpleDateFormat("MM / yy", java.util.Locale.getDefault()).format(java.util.Date(vm.statSince))
    val topArtist = rec.topArtists.firstOrNull()
    val topSong = rec.topSongs.firstOrNull()
    val openRecap = { vm.recapOpen = true }

    // artists: the ones you liked first, then the ones you listen to most
    val artistTiles = ArrayList<ArtistItem>()
    artistTiles.addAll(vm.likedArtists)
    for (a in rec.topArtists) {
        if (artistTiles.none { it.name.equals(a.name, ignoreCase = true) }) artistTiles.add(ArtistItem(a.name, a.cover))
    }

    val tiles = ArrayList<LibTile>()
    tiles.add(LibTile("New playlist", "Save your own mix", null, Icons.Filled.Add, null) {
        vm.editPlaylistId = null
        vm.pendingSong = null
        vm.showPlaylistDialog = true
    })
    tiles.add(LibTile("Import playlist", "From a playlist link", null, Icons.Filled.FileDownload, null) {
        vm.importStatus = ""
        vm.importedId = null
        vm.showImportDialog = true
    })
    tiles.add(LibTile("Spotify", "Connect, copy, merge", null, Icons.Filled.Cloud, listOf(Color(0xFF1F9D55), Color(0xFF0B4D2A))) {
        SpotifyHub.open = true
    })
    tiles.add(LibTile("Move to another app", "Take your music with you", null, Icons.Filled.SwapHoriz, listOf(Color(0xFF3A5BD9), Color(0xFF7B3FD0))) {
        DataMove.open = true
    })
    tiles.add(LibTile(
        "Liked Songs", vm.liked.size.toString() + " songs", null,
        Icons.Filled.Favorite, listOf(Color(0xFF8B6CF0), Color(0xFFEF5DA8))
    ) { vm.likedOpen = true })
    for (p in vm.playlists) {
        tiles.add(LibTile(p.name, p.songs.size.toString() + " songs", p.shownCover(), Icons.Filled.LibraryMusic, null) {
            vm.openPlaylist(p.id)
        })
    }

    LazyColumn(contentPadding = ScreenPad) {
        item {
            ScreenTitle("Library") {
                Row(
                    Modifier.glass(RoundedCornerShape(50)).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.recapOpen = true }) {
                        Icon(Icons.Filled.History, null, tint = Color.White)
                    }
                    IconButton(onClick = { vm.showSettings = true }) {
                        ProfileButton(vm, 30.dp, false)
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 6.dp)) {
                item {
                    StatCard(
                        cardW, "YOUR LISTENING EXPERIENCE", vm.statMinutes.toString(), 52, "MINUTES LISTENED",
                        name, vm.statPlays.toString() + " Plays \u00B7 " + year, since, "MEMBER SINCE", openRecap
                    )
                }
                item {
                    StatCard(
                        cardW, "YOUR TOP ARTIST", topArtist?.name ?: "Not yet", 34, "TOP ARTIST",
                        name, if (topArtist != null) (topArtist.sec / 60).toString() + " min listened" else "Play some songs",
                        null, null, openRecap
                    )
                }
                item {
                    StatCard(
                        cardW, "YOUR TOP SONG", topSong?.title ?: "Not yet", 30, "TOP SONG",
                        name, topSong?.artist?.let { StatsStore.cleanArtist(it) } ?: "Play some songs",
                        null, null, openRecap
                    )
                }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            LibRow(Icons.Filled.Download, "Downloads", vm.downloads.size.toString() + " songs for offline") {
                vm.refreshDownloadBytes()
                vm.downloadsOpen = true
            }
        }
        item { LibRow(Icons.Filled.Favorite, "Liked songs", vm.liked.size.toString() + " songs") { vm.likedOpen = true } }
        item { LibRow(Icons.Filled.AutoAwesome, "Your Recap", "Top songs and artists") { vm.recapOpen = true } }

        if (vm.ytSignedIn) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("YouTube Music", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(
                        if (vm.ytBusy) "Loading..." else "Refresh", fontSize = 16.sp, color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { vm.refreshYt() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            item {
                val ytTiles = ArrayList<LibTile>()
                ytTiles.add(LibTile(
                    "Liked songs", "On YouTube Music", null,
                    Icons.Filled.ThumbUp, listOf(Color(0xFFE53935), Color(0xFF8E1B1B))
                ) { vm.openYtLiked() })
                for (yp in vm.ytPlaylistsShown) {
                    ytTiles.add(LibTile(yp.title, yp.subtitle.ifBlank { "Playlist" }, yp.cover, Icons.Filled.LibraryMusic, null) {
                        vm.openYtPlaylist(yp)
                    })
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(ytTiles) { t -> PlTile(t, Modifier.width(tileSize)) }
                }
            }
            if (vm.ytArtists.isNotEmpty()) {
                item {
                    Text(
                        "Your YouTube artists", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                        modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)
                    )
                }
                item {
                    val yar = vm.ytArtists.toList()
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(yar) { a ->
                            PlTile(LibTile(a.name, "Artist", a.cover, Icons.Filled.Person, null) {
                                vm.openArtist(a.name)
                            }, Modifier.width(tileSize))
                        }
                    }
                }
            }
            if (vm.ytStatus.startsWith("Library load fail")) {
                item {
                    Text(
                        vm.ytStatus, fontSize = 12.sp, color = Color(0xFFFFB4AB),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            item {
                LibRow(Icons.Filled.AccountCircle, "Sign in with Google", "Optional: see your YouTube Music library") {
                    vm.showLogin = true
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playlists", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(
                    if (showAll) "Show less" else "Show all", fontSize = 16.sp, color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showAll = !showAll }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        item {
            androidx.compose.animation.AnimatedContent(
                targetState = showAll,
                transitionSpec = { fadeIn(androidx.compose.animation.core.tween(240)) togetherWith fadeOut(androidx.compose.animation.core.tween(160)) },
                label = "plShow"
            ) { all ->
                if (all) {
                    val cols = if (big) 4 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        tiles.chunked(cols).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                row.forEach { t -> PlTile(t, Modifier.weight(1f)) }
                                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(tiles) { t -> PlTile(t, Modifier.width(tileSize)) }
                    }
                }
            }
        }
        if (vm.played.isNotEmpty()) {
            item {
                Text(
                    "Recently played", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
                )
            }
            item {
                val list = vm.played.toList().take(12)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(list) { s ->
                        PlTile(LibTile(s.title, s.artist, s.cover, Icons.Filled.MusicNote, null) {
                            vm.play(s, list)
                        }, Modifier.width(tileSize))
                    }
                }
            }
        }
        if (vm.savedAlbums.isNotEmpty()) {
            item {
                Text(
                    "Saved albums", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
                )
            }
            item {
                val albums = vm.savedAlbums.toList()
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(albums) { al ->
                        PlTile(LibTile(al.title, al.artist.ifBlank { "Album" }, al.cover, Icons.Filled.Album, null) {
                            vm.openAlbum(al)
                        }, Modifier.width(tileSize))
                    }
                }
            }
        }
        item {
            Text(
                "Artists", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
            )
        }
        item {
            if (artistTiles.isEmpty()) {
                Text(
                    "Artists you like or listen to a lot show up here",
                    fontSize = 13.sp, color = Pal.muted
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(artistTiles.take(14)) { a ->
                        PlTile(LibTile(a.name, "Artist", a.cover, Icons.Filled.Person, null) {
                            vm.openArtist(a.name)
                        }, Modifier.width(tileSize))
                    }
                }
            }
        }
    }
}
