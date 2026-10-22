package com.ameos.music

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SsBg = Color(0xFF2A1F21)
private val SsRow = Color(0xFF382C2F)
private val SsSub = Color(0xFFC9B3B6)

@Composable
private fun SongSheetRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SsRow)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SsSub, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(sub, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SsSub)
        }
    }
}

// Bottom sheet that opens when you long-press any song: Play next, Add to playlist, Add to queue.
@Composable
fun SongSheet(vm: MainViewModel) {
    val ctx = LocalContext.current
    val holder = remember { arrayOfNulls<Song>(1) }
    val open = vm.songMenu
    if (open != null) holder[0] = open
    val show = open != null
    val close = { vm.songMenu = null }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { close() }
            )
        }
        AnimatedVisibility(
            visible = show,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200))
        ) {
            val song = holder[0]
            if (song != null) {
                // registered only while the sheet is visible, so Back closes it first
                BackHandler { close() }
                val scope = rememberCoroutineScope()
                val dragY = remember { Animatable(0f) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, dragY.value.roundToInt()) }
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(SsBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (dragY.value > 160f) close()
                                        else scope.launch { dragY.animateTo(0f) }
                                    },
                                    onDragCancel = { scope.launch { dragY.animateTo(0f) } },
                                    onVerticalDrag = { change, amount ->
                                        change.consume()
                                        scope.launch {
                                            dragY.snapTo((dragY.value + amount).coerceAtLeast(0f))
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(44.dp, 5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x66FFFFFF))
                        )
                    }
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Cover(song.cover, Modifier.size(60.dp), RoundedCornerShape(16.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    song.title, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song.artist, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                                    color = SsSub, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SongSheetRow(Icons.Filled.QueuePlayNext, "Play next", "Plays right after the current song") {
                            val idle = vm.current == null
                            close()
                            vm.playNext(song)
                            if (!idle) Toast.makeText(ctx, "Will play next", Toast.LENGTH_SHORT).show()
                        }
                        SongSheetRow(Icons.Filled.PlaylistAdd, "Add to playlist", "Pick or create a playlist") {
                            close()
                            vm.addTarget = song
                        }
                        SongSheetRow(Icons.Filled.QueueMusic, "Add to queue", "Adds to the end of the queue") {
                            val idle = vm.current == null
                            close()
                            vm.addToQueue(song)
                            if (!idle) Toast.makeText(ctx, "Added to queue", Toast.LENGTH_SHORT).show()
                        }
                        SongSheetRow(
                            Icons.Filled.Download,
                            if (vm.isDownloaded(song)) "Downloaded" else "Download",
                            "Save for offline listening"
                        ) {
                            close()
                            if (vm.isDownloaded(song)) {
                                Toast.makeText(ctx, "Already downloaded", Toast.LENGTH_SHORT).show()
                            } else {
                                vm.downloadSong(song)
                                Toast.makeText(ctx, "Download started", Toast.LENGTH_SHORT).show()
                            }
                        }
                        SongSheetRow(Icons.Filled.Person, "Go to artist", artistName(song.artist)) {
                            close()
                            vm.showPlayer = false
                            vm.showLyrics = false
                            vm.openArtist(song.artist)
                        }
                        Spacer(Modifier.navigationBarsPadding().height(24.dp))
                    }
                }
            }
        }
    }
}
