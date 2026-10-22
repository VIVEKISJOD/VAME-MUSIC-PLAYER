package com.ameos.music

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// "Arijit Singh - Topic" -> "Arijit Singh"
fun artistName(s: String): String = s.removeSuffix(" - Topic").trim()

// same artist written in different ways gives the same key
fun artistKey(s: String): String = artistName(s).lowercase().filter { it.isLetterOrDigit() }

// ---------------- ARTIST PAGE ----------------

@Composable
fun ArtistScreen(vm: MainViewModel) {
    BackHandler(enabled = vm.artistOpen) { vm.closeArtist() }
    val name = vm.openArtistName
    val songs = vm.artistSongs
    val cover = songs.firstOrNull()?.cover
    val fav = vm.isArtistLiked(name)

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3A1511), Color(0xFF120D0D), Color(0xFF0A0808))
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
                IconButton(onClick = { vm.closeArtist() }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 170.dp)
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Cover(
                            cover,
                            Modifier
                                .size(170.dp)
                                .border(1.dp, Color(0x33FF6B7A), CircleShape),
                            CircleShape
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            name, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (vm.artistLoading) "Loading songs..."
                            else songs.size.toString() + " songs",
                            fontSize = 13.sp, color = Color(0xFF8C7C7E),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (fav) Color(0x88FFB3B9) else Color(0x33FFFFFF))
                                    .clickable { vm.toggleArtistLike(name, cover) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    null, tint = Color.White, modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (fav) "Liked" else "Like artist",
                                    fontSize = 14.sp, color = Color.White
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Row(
                                Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF5A1A1A)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { vm.playArtist(true) }) {
                                    Icon(Icons.Filled.Shuffle, null, tint = Color.White)
                                }
                                Box(
                                    Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD9381E))
                                        .clickable { vm.playArtist(false) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow, null, tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item { SectionLabel("Songs") }
                if (vm.artistLoading && songs.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                Modifier.size(28.dp), color = Color(0xFFFFB3B9), strokeWidth = 3.dp
                            )
                        }
                    }
                }
                items(songs) { s -> SongRow(s, vm, songs) }
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
                    listOf(Color(0xFF3A1511), Color(0xFF120D0D), Color(0xFF0A0808))
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
                        color = Color(0xFF8C7C7E)
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
                    Text(
                        "No liked songs yet. Tap the heart on any song.",
                        fontSize = 15.sp, color = Color(0xFFC9B3B6)
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
