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
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0x22FFFFFF))
            .border(1.dp, Color(0x55FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun ArtistScreen(vm: MainViewModel) {
    BackHandler(enabled = vm.artistOpen) { vm.closeArtist() }
    val ctx = LocalContext.current
    val name = vm.openArtistName
    val songs = vm.artistSongs
    val cover = songs.firstOrNull()?.cover
    val fav = vm.isArtistLiked(name)
    val album = if (vm.artistAlbumCardOn) vm.artistAlbum else null
    val latest = vm.artistLatestRelease
    // the picture: newest official upload (or the song cover when it is not found / switched off)
    val heroPic = (if (vm.artistLatestPic) latest?.picture else null) ?: cover
    val shown = when (vm.artistTop) {
        0 -> songs.take(10)
        1 -> songs.take(25)
        else -> songs
    }
    val tint = rememberCoverTint(heroPic)
    val bg = lerp(tint, Color.Black, 0.62f)
    val light = lerp(tint, Color.White, 0.78f)
    val dark = lerp(tint, Color.Black, 0.75f)

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 170.dp)
        ) {
            // big picture that fades into the page, with the name on top of the fade
            item {
                Box(Modifier.fillMaxWidth().height(480.dp)) {
                    Cover(heroPic, Modifier.fillMaxSize(), RectangleShape, hiRes = true)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.5f to Color.Transparent,
                                    1f to bg
                                )
                            )
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            name, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        if (latest != null) {
                            Row(
                                Modifier
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x55000000))
                                    .clickable { vm.play(latest.song, listOf(latest.song)) }
                                    .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow, null, tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Latest: " + latest.label, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, color = Color.White,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 240.dp)
                                )
                            }
                        }
                    }
                }
            }
            // shuffle, big play, favourite star
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtistRoundBtn(Icons.Filled.Shuffle) { vm.playArtist(true) }
                    Spacer(Modifier.width(26.dp))
                    Box(
                        Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(light)
                            .clickable { vm.playArtist(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow, null, tint = dark,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.width(26.dp))
                    ArtistRoundBtn(if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder) {
                        vm.toggleArtistLike(name, cover)
                    }
                }
            }
            // the artist's top album
            if (album != null) {
                item {
                    val cardShape = RoundedCornerShape(32.dp)
                    val saved = vm.isAlbumSaved(album)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 28.dp)
                            .clip(cardShape)
                            .background(Color(0x26FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), cardShape)
                            .clickable { vm.openAlbum(album) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cover(album.cover, Modifier.size(104.dp), RoundedCornerShape(18.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Popular album", fontSize = 14.sp, color = Color(0xCCFFFFFF))
                            Text(
                                album.title, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                artistName(album.artist), fontSize = 15.sp, color = Color(0xCCFFFFFF),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(46.dp)
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
            item {
                Text(
                    "Top songs", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 30.dp, bottom = 10.dp)
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
            items(shown) { s ->
                Box(Modifier.padding(horizontal = 16.dp)) { SongRow(s, vm, songs) }
            }
        }
        // back button and share, floating over the picture
        Row(
            Modifier
                .fillMaxWidth()
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
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable {
                        try {
                            val send = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(
                                    Intent.EXTRA_TEXT,
                                    if (latest != null) "Listening to " + name + " - " + latest.label + " on VAME Music"
                                    else "Listening to " + name + " on VAME Music"
                                )
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
        MiniPlayer(
            vm,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

// ---------------- ALL LIKED SONGS ----------------

@Composable
fun LikedScreen(vm: MainViewModel) {
    BackHandler(enabled = vm.likedOpen) { vm.closeLiked() }
    val songs = vm.liked.toList()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Pal.bgTop, Pal.bgMid, Pal.bgBot)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
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
                        songs.size.toString() + " songs", fontSize = 12.sp,
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 170.dp)
                ) {
                    items(songs) { s -> SongRow(s, vm, songs) }
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
