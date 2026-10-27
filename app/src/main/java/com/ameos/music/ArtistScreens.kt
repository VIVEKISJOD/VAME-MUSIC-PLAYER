package com.ameos.music

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// "Arijit Singh - Topic" -> "Arijit Singh"
fun artistName(s: String): String = s.removeSuffix(" - Topic").trim()

// same artist written in different ways gives the same key
fun artistKey(s: String): String = artistName(s).lowercase().filter { it.isLetterOrDigit() }

// ---------------- ARTIST PAGE ----------------

@Composable
private fun ArtistRoundBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(Color(0x26FFFFFF))
            .border(1.dp, Color(0x40FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

private val ShowAllColor = Color(0xFFE6F27C)

@Composable
private fun ArtistTopRow(s: Song, vm: MainViewModel, list: List<Song>) {
    val playing = vm.current?.url == s.url
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { vm.play(s, list) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(s.cover, Modifier.size(52.dp), RoundedCornerShape(8.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                s.title, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = if (playing) ShowAllColor else Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                s.artist, fontSize = 14.sp, color = Color(0xCCFFFFFF),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable { vm.songMenu = s },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MoreVert, null, tint = Color(0xCCFFFFFF), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ArtistCard(card: YtCard, w: Dp, onClick: () -> Unit) {
    Column(
        Modifier
            .width(w)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Cover(card.cover, Modifier.size(w), RoundedCornerShape(18.dp))
        Text(
            card.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (card.sub.isNotBlank()) {
            Text(
                card.sub, fontSize = 13.sp, color = Color(0xCCFFFFFF),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistScreen(vm: MainViewModel) {
    BackHandler(enabled = vm.artistOpen) { vm.closeArtist() }
    val ctx = LocalContext.current
    val name = vm.openArtistName
    val songs = vm.artistAllSongs
    val page = vm.artistPage
    val cover = songs.firstOrNull()?.cover
    val fav = vm.isArtistLiked(name)
    val album = if (vm.artistAlbumCardOn) vm.artistAlbum else null
    val latest = vm.artistLatestRelease
    // the picture: the artist's own YouTube Music photo, else newest official upload, else a song cover
    val heroPic = page?.pic ?: (if (vm.artistLatestPic) latest?.picture else null) ?: cover
    val shown = when (vm.artistTop) {
        0 -> songs.take(10)
        1 -> songs.take(25)
        else -> songs
    }
    val tint = rememberCoverTint(heroPic)
    val bg = lerp(tint, Color.Black, 0.55f)
    val light = lerp(tint, Color.White, 0.78f)
    val dark = lerp(tint, Color.Black, 0.75f)
    val ls = rememberLazyListState()
    val showTitle = ls.firstVisibleItemIndex >= 1
    val barBg by animateColorAsState(if (showTitle) bg.copy(alpha = 0.94f) else Color.Transparent, label = "artistBar")
    val titleA by animateFloatAsState(if (showTitle) 1f else 0f, label = "artistTitle")
    var openShelves by remember { mutableStateOf(setOf<String>()) }
    var moreAbout by remember { mutableStateOf(false) }
    LaunchedEffect(name) {
        ls.scrollToItem(0)
        openShelves = emptySet()
        moreAbout = false
    }
    val screenW = LocalConfiguration.current.screenWidthDp
    val pageW = (screenW - 48).coerceAtMost(560).dp
    val cardW = if (screenW >= 600) 180.dp else 150.dp
    val dial = Dial("artist", name, name, "Artist", heroPic ?: cover)
    val pinned = vm.isDialed("artist", name)

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .blockTaps()
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = ls,
            contentPadding = PaddingValues(bottom = 170.dp)
        ) {
            // big picture that fades into the page, with the name on top of the fade
            item {
                Box(Modifier.fillMaxWidth().height(430.dp)) {
                    Cover(heroPic, Modifier.fillMaxSize(), RectangleShape, hiRes = true)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.45f to Color.Transparent,
                                    1f to bg
                                )
                            )
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            name, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        val sub = page?.monthly ?: ""
                        if (sub.isNotBlank()) {
                            Text(sub, fontSize = 14.sp, color = Color(0xCCFFFFFF), modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            // shuffle, big play, favourite star
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtistRoundBtn(Icons.Filled.Shuffle) { vm.playArtist(true) }
                    Spacer(Modifier.width(22.dp))
                    Box(
                        Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(light)
                            .clickable { vm.playArtist(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow, null, tint = dark,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                    Spacer(Modifier.width(22.dp))
                    ArtistRoundBtn(if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder) {
                        vm.toggleArtistLike(name, cover)
                    }
                }
            }
            // the artist's top album (glass card)
            if (album != null) {
                item {
                    val cardShape = RoundedCornerShape(28.dp)
                    val saved = vm.isAlbumSaved(album)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 26.dp)
                            .clip(cardShape)
                            .background(Color(0x26FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), cardShape)
                            .clickable { vm.openAlbum(album) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cover(album.cover, Modifier.size(92.dp), RoundedCornerShape(16.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Popular album", fontSize = 13.sp, color = Color(0xCCFFFFFF))
                            Text(
                                album.title, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                artistName(album.artist), fontSize = 14.sp, color = Color(0xCCFFFFFF),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0x66FFFFFF), CircleShape)
                                .clickable { vm.toggleSaveAlbum(album) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (saved) Icons.Filled.Check else Icons.Filled.Add,
                                null, tint = Color.White, modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            // top songs: pages of 4 rows, swipe sideways for the next page
            item {
                Text(
                    "Top songs", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 8.dp)
                )
            }
            if (vm.artistLoading && songs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            Modifier.size(28.dp), color = Color.White, strokeWidth = 3.dp
                        )
                    }
                }
            }
            if (shown.isNotEmpty()) {
                item {
                    val pages = shown.chunked(4)
                    val rowState = rememberLazyListState()
                    LazyRow(
                        state = rowState,
                        flingBehavior = rememberSnapFlingBehavior(rowState),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pages) { pg ->
                            Column(Modifier.width(pageW)) {
                                for (s in pg) ArtistTopRow(s, vm, songs)
                            }
                        }
                    }
                }
            }
            // YouTube Music shelves: Albums, Singles & EPs, Featured on, Playlists, Fans might also like
            if (page != null) {
                for (shelf in page.shelves) {
                    item(key = "shelf_" + shelf.title) {
                        val open = openShelves.contains(shelf.title)
                        Column(Modifier.fillMaxWidth().animateContentSize()) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    shelf.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (shelf.cards.size > 3) {
                                    Text(
                                        if (open) "Show less" else "Show all", fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium, color = ShowAllColor,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                openShelves = if (open) openShelves - shelf.title else openShelves + shelf.title
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            if (open) {
                                Column(
                                    Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    for (row in shelf.cards.chunked(2)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                            for (c in row) {
                                                BoxWithConstraints(Modifier.weight(1f)) {
                                                    ArtistCard(c, maxWidth) { vm.openYtCard(c, shelf.cards) }
                                                }
                                            }
                                            if (row.size < 2) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(shelf.cards) { c ->
                                        ArtistCard(c, cardW) { vm.openYtCard(c, shelf.cards) }
                                    }
                                }
                            }
                        }
                    }
                }
                // about the artist
                if (page.about.isNotBlank() || page.subscribers.isNotBlank() || page.monthly.isNotBlank()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 30.dp)) {
                            Text(
                                "About the artist", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (page.about.isNotBlank()) {
                                Text(
                                    page.about, fontSize = 15.sp, color = Color(0xE6FFFFFF),
                                    maxLines = if (moreAbout) 60 else 3, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 10.dp).animateContentSize()
                                )
                                Text(
                                    if (moreAbout) "Less" else "More", fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold, color = Color.White,
                                    modifier = Modifier
                                        .clickable { moreAbout = !moreAbout }
                                        .padding(top = 6.dp, bottom = 6.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            if (page.subscribers.isNotBlank()) {
                                AboutChip(Icons.Filled.Person, page.subscribers)
                            }
                            if (page.monthly.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                AboutChip(Icons.Filled.GraphicEq, page.monthly)
                            }
                        }
                    }
                }
            }
        }
        // top bar: back, name (appears when you scroll), pin + share
        Row(
            Modifier
                .fillMaxWidth()
                .background(barBg)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { vm.closeArtist() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
            }
            Text(
                name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp).alpha(titleA)
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .clickable { pinToggle(vm, ctx, dial) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PushPin, null, tint = if (pinned) ShowAllColor else Color.White)
                }
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .clickable {
                            try {
                                val send = Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, "Listening to " + name + " on VAME Music")
                                ctx.startActivity(
                                    Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (e: Exception) {
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Share, null, tint = Color.White)
                }
            }
        }
        MiniPlayer(
            vm,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun AboutChip(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x26FFFFFF))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

// ---------------- ALL LIKED SONGS ----------------

@Composable
fun LikedScreen(vm: MainViewModel) {
    BackHandler(enabled = vm.likedOpen) { vm.closeLiked() }
    val songs = vm.liked.toList()
    val likedMins = (songs.sumOf { it.durationSec } / 60).toInt()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Pal.bgTop, Pal.bgMid, Pal.bgBot)
                )
            )
            .blockTaps()
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.closeLiked() }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Liked songs", fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        songs.size.toString() + " songs" + (if (likedMins > 0) "  -  " + likedMins.toString() + " min" else ""), fontSize = 12.sp,
                        color = Pal.muted
                    )
                }
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF5A1A1A)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.playLiked(true) }) {
                        Icon(Icons.Filled.Shuffle, null, tint = Color.White)
                    }
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD9381E))
                            .clickable { vm.playLiked(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow, null, tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
            if (songs.isEmpty()) {
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        Icons.Filled.Favorite, "No liked songs yet",
                        "Tap the heart on any song and it shows up here."
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 170.dp)
                ) {
                    items(songs) { s -> SongRow(s, vm, songs) }
                }
            }
        }
        ScrollTopButton(listState, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 100.dp))
        MiniPlayer(
            vm,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
