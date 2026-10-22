package com.ameos.music

import android.content.Intent
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.filled.DragHandle
import kotlin.math.roundToInt
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Pink = Color(0xFFFFB3B9)

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}

// "..." button. Opens the quick settings sheet (QuickSheet.kt). Used on Now Playing and Lyrics.
@Composable
fun PlayerMenuButton(vm: MainViewModel, size: Dp = 40.dp) {
    if (vm.current == null) return
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable { vm.showMenu = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun NowPlayingScreen(vm: MainViewModel) {
    val song = vm.current ?: return
    var showQueue by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val tint = rememberCoverTint(song.cover)
    var big by remember { mutableStateOf(false) }
    val bgAlpha by animateFloatAsState(if (big) 0.95f else 0.30f, tween(450), label = "bgAlpha")
    val bgBlur by animateDpAsState(if (big) 0.dp else 50.dp, tween(450), label = "bgBlur")
    val coverAlpha by animateFloatAsState(if (big) 0f else 1f, tween(350), label = "coverAlpha")
    val shade by animateFloatAsState(if (big) 1f else 0f, tween(450), label = "shade")

    BackHandler { vm.showPlayer = false }
    BackHandler(enabled = showQueue) { showQueue = false }

    LaunchedEffect(song.url) {
        while (true) {
            pos = vm.player.currentPosition
            val d = vm.player.duration
            dur = if (d > 0) d else song.durationSec * 1000
            delay(300)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(tint, lerp(tint, Color.Black, 0.35f), lerp(tint, Color.Black, 0.65f))
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        Cover(
            song.cover,
            Modifier.fillMaxSize().alpha(bgAlpha).blur(bgBlur),
            RoundedCornerShape(0.dp)
        )
        Box(
            Modifier
                .fillMaxSize()
                .alpha(shade)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x66000000), Color.Transparent, Color(0xDD000000))
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // header
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.showPlayer = false }) {
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White)
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        song.artist, fontSize = 12.sp, color = Color(0xFFE6BDC0),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                vm.showPlayer = false
                                vm.showLyrics = false
                                vm.openArtist(song.artist)
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Text(
                        song.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(48.dp))
            }

            // big cover (tap = lyrics)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Cover(
                    song.cover,
                    Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 20.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .alpha(coverAlpha)
                        .clickable { vm.showLyrics = true },
                    RoundedCornerShape(24.dp)
                )
            }

            // heart | lyrics, repeat, queue
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIcon(
                    if (vm.isLiked(song)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                ) { vm.toggleLike(song) }
                Spacer(Modifier.weight(1f))
                Text(
                    "Animate cover", fontSize = 12.sp, color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (big) Color(0x88FFB3B9) else Color(0x33FFFFFF))
                        .clickable { big = !big }
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                )
                Spacer(Modifier.weight(1f))
                RoundIcon(Icons.Filled.Subject) { vm.showLyrics = true }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (vm.repeat == 0) Color(0x33FFFFFF) else Color(0x88FFB3B9))
                        .clickable { vm.cycleRepeat() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (vm.repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        null, tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                RoundIcon(Icons.Filled.QueueMusic) { showQueue = true }
            }

            // progress
            val max = dur.coerceAtLeast(1L).toFloat()
            val shown = if (dragging) dragValue else pos.toFloat()
            Slider(
                value = shown.coerceIn(0f, max),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    vm.player.seekTo(dragValue.toLong())
                    pos = dragValue.toLong()
                    dragging = false
                },
                valueRange = 0f..max,
                colors = SliderDefaults.colors(
                    thumbColor = Pink,
                    activeTrackColor = Pink,
                    inactiveTrackColor = Color(0x55FFFFFF)
                )
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(fmt(shown.toLong()), fontSize = 12.sp, color = Color(0xFFE6BDC0))
                Spacer(Modifier.weight(1f))
                Text(fmt(dur), fontSize = 12.sp, color = Color(0xFFE6BDC0))
            }

            // controls
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.prev() }, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Pink, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.width(16.dp))
                Box(
                    Modifier.size(72.dp).clip(CircleShape).clickable { vm.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (vm.loading) {
                        CircularProgressIndicator(
                            Modifier.size(34.dp), color = Pink, strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Pink, modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.SkipNext, null, tint = Pink, modifier = Modifier.size(36.dp))
                }
            }

            // bottom row
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x33FFFFFF))
                        .clickable {
                            vm.refreshOutputs()
                            vm.showOutputDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val outIcon = when (vm.currentOutput?.kind) {
                        AudioOutput.BLUETOOTH -> Icons.Filled.Bluetooth
                        AudioOutput.WIRED -> Icons.Filled.Headphones
                        else -> Icons.Filled.PhoneAndroid
                    }
                    Icon(outIcon, null, tint = Pink, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        vm.outputLabel, fontSize = 12.sp, color = Pink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 170.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                PlayerMenuButton(vm)
            }
        }

        AnimatedVisibility(
            visible = showQueue,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(350)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(200))
        ) {
            QueuePanel(vm) { showQueue = false }
        }
    }
}

private fun queueKeys(q: List<Song>): List<String> {
    val seen = HashMap<String, Int>()
    return q.map { song ->
        val n = seen[song.url] ?: 0
        seen[song.url] = n + 1
        song.url + "#" + n
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueuePanel(vm: MainViewModel, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val itemPx = with(LocalDensity.current) { 60.dp.toPx() }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val keys = queueKeys(vm.queue)

    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.62f)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xF2200B0F))
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Up next", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    "Drag the handle to reorder. Long-press a song for more.",
                    fontSize = 12.sp, color = Color(0xFFE6BDC0)
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable {
                        val ok = vm.shuffleUpNext()
                        Toast.makeText(
                            ctx,
                            if (ok) "Up next shuffled" else "Not enough songs to shuffle",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shuffle, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
            itemsIndexed(vm.queue, key = { i, _ -> keys[i] }) { i, s ->
                val k = keys[i]
                val dragging = dragKey == k
                val cur = i == vm.index
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .then(if (dragging) Modifier.zIndex(1f) else Modifier.animateItem())
                        .graphicsLayer { translationY = if (dragging) dragY else 0f }
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (dragging) Color(0x55FFB3B9)
                            else if (cur) Color(0x33FFB3B9)
                            else Color.Transparent
                        )
                        .combinedClickable(
                            onClick = { vm.playFromQueue(i) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.songMenu = s
                            }
                        )
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cover(s.cover, Modifier.size(44.dp), RoundedCornerShape(8.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.title, fontSize = 15.sp, color = Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            s.artist, fontSize = 12.sp, color = Color(0xFFE6BDC0),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        Modifier
                            .size(52.dp)
                            .pointerInput(k) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragKey = k
                                        dragY = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragY += amount.y
                                        val now = queueKeys(vm.queue)
                                        val from = now.indexOf(dragKey)
                                        if (from >= 0) {
                                            val shift = (dragY / itemPx).roundToInt()
                                            val to = (from + shift).coerceIn(0, now.size - 1)
                                            if (to != from) {
                                                vm.moveInQueue(from, to)
                                                dragY -= (to - from) * itemPx
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        dragKey = null
                                        dragY = 0f
                                        vm.reorderDone()
                                    },
                                    onDragCancel = {
                                        dragKey = null
                                        dragY = 0f
                                        vm.reorderDone()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.DragHandle, null,
                            tint = Color(0xFFE6BDC0), modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
