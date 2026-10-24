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
import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign

private val Pink: Color get() = Pal.accent
private val SoftWhite = Color(0xFFEDEDED)

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return (s / 60).toString().padStart(2, '0') + ":" + (s % 60).toString().padStart(2, '0')
}

// "..." button. Opens the quick settings sheet (QuickSheet.kt). Used on Lyrics.
@Composable
fun PlayerMenuButton(vm: MainViewModel, size: Dp = 40.dp) {
    if (vm.current == null) return
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .glassFill()
            .clickable { vm.showMenu = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

// Drag down on this area to close the player.
private fun Modifier.swipeDown(onClose: () -> Unit): Modifier = this.pointerInput(Unit) {
    var total = 0f
    detectVerticalDragGestures(
        onDragStart = { total = 0f },
        onVerticalDrag = { change, dy ->
            change.consume()
            total += dy
        },
        onDragEnd = { if (total > 90f) onClose() },
        onDragCancel = { total = 0f }
    )
}

// Thick rounded bar (progress + volume). Gets a little thicker while the finger is on it.
@Composable
internal fun Scrubber(
    fraction: Float,
    onStart: () -> Unit,
    onChange: (Float) -> Unit,
    onEnd: (Float) -> Unit
) {
    var active by remember { mutableStateOf(false) }
    var last by remember { mutableFloatStateOf(0f) }
    val h by animateDpAsState(if (active) 18.dp else 14.dp, spring(), label = "scrubH")
    val startS by rememberUpdatedState(onStart)
    val changeS by rememberUpdatedState(onChange)
    val endS by rememberUpdatedState(onEnd)
    Box(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        active = true
                        startS()
                        last = (off.x / size.width.toFloat()).coerceIn(0f, 1f)
                        changeS(last)
                    },
                    onDragEnd = {
                        active = false
                        endS(last)
                    },
                    onDragCancel = {
                        active = false
                        endS(last)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        last = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        changeS(last)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    val f = (off.x / size.width.toFloat()).coerceIn(0f, 1f)
                    startS()
                    changeS(f)
                    endS(f)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(h)
                .clip(CircleShape)
                .background(Color(0x55FFFFFF))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(SoftWhite)
            )
        }
    }
}

// Real phone volume (music stream), speaker icons on both sides.
@Composable
internal fun VolumeBar() {
    val ctx = LocalContext.current
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxV = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxV) }
    var touching by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            if (!touching) vol = am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxV
            delay(500)
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.VolumeDown, null, tint = Color(0xCCFFFFFF), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            Scrubber(
                fraction = vol,
                onStart = { touching = true },
                onChange = {
                    vol = it
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxV).roundToInt(), 0)
                },
                onEnd = { touching = false }
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Filled.VolumeUp, null, tint = Color(0xCCFFFFFF), modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun BarIcon(icon: ImageVector, tint: Color = Color(0xDDFFFFFF), onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun NowPlayingScreen(vm: MainViewModel) {
    val song = vm.current ?: return
    var showQueue by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(0f) }
    var quality by remember { mutableStateOf("") }
    val tint = rememberCoverTint(song.cover)
    var moving by remember { mutableStateOf(false) } // "Animate cover": slow zoom + drift
    val kb = rememberInfiniteTransition(label = "kb")
    val kbScale by kb.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "kbScale"
    )
    val kbShift by kb.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(21000, easing = LinearEasing), RepeatMode.Reverse),
        label = "kbShift"
    )

    BackHandler { vm.showPlayer = false }
    BackHandler(enabled = showQueue) { showQueue = false }

    LaunchedEffect(song.url) {
        while (true) {
            pos = vm.posMs()
            val d = vm.durMs()
            dur = if (d > 0) d else song.durationSec * 1000
            val f = vm.player.audioFormat
            if (f != null) {
                val parts = ArrayList<String>()
                if (f.bitrate > 0) parts.add((f.bitrate / 1000).toString() + " kbps")
                if (f.sampleRate > 0) {
                    val sr = f.sampleRate
                    parts.add((if (sr % 1000 == 0) (sr / 1000).toString() else "%.1f".format(sr / 1000f)) + " kHz")
                }
                quality = parts.joinToString(" · ")
            }
            delay(200)
        }
    }

    // the lyric line that is being sung right now
    val lines = vm.lyrics
    val curLine: String? = if (vm.lyricsSynced && lines.isNotEmpty()) {
        val i = lines.indexOfLast { it.timeMs <= pos }
        if (i >= 0) lines[i].text.ifBlank { "♪" } else null
    } else null

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Pal.bgBot, lerp(tint, Color.Black, if (Pal.amoled) 0.9f else 0.55f), lerp(tint, Color.Black, if (Pal.amoled) 1f else 0.8f))
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        val colW = minOf(maxWidth, 520.dp)
        val artH = minOf(colW, maxHeight * 0.56f)

        // soft blurred cover behind everything (fills wide screens too)
        Cover(
            song.cover,
            Modifier.fillMaxSize().alpha(0.22f).blur(90.dp),
            RoundedCornerShape(0.dp),
            hiRes = true
        )

        // full-width artwork at the top, fading into the background
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .width(colW)
                .height(artH)
                .clipToBounds()
        ) {
            Cover(
                song.cover,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val sc = if (moving) kbScale else 1f
                        scaleX = sc
                        scaleY = sc
                        translationX = if (moving) kbShift else 0f
                    },
                RoundedCornerShape(0.dp),
                hiRes = true
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x55000000),
                            0.18f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to Pal.bgBot
                        )
                    )
            )
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .width(colW)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // grabber (drag down or tap to close)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .swipeDown { vm.showPlayer = false }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { vm.showPlayer = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(48.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0x99FFFFFF))
                )
            }

            // empty area over the artwork: tap = Animate cover, drag down = close
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .swipeDown { vm.showPlayer = false }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { moving = !moving }
            )

            // title + artist | star, 3 dots
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist, fontSize = 18.sp, color = Color(0x99FFFFFF),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                vm.showPlayer = false
                                vm.showLyrics = false
                                vm.openArtist(song.artist)
                            }
                    )
                }
                IconButton(onClick = { vm.toggleLike(song) }) {
                    Icon(
                        if (vm.isLiked(song)) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        null,
                        tint = if (vm.isLiked(song)) Pink else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { vm.showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // one line of lyrics (tap = full lyrics)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { vm.showLyrics = true },
                contentAlignment = Alignment.CenterStart
            ) {
                Crossfade(targetState = curLine, animationSpec = tween(350), label = "oneLine") { t ->
                    if (t != null) {
                        Text(
                            t, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // progress
            val max = dur.coerceAtLeast(1L)
            val frac = if (dragging) dragFrac else (pos.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            val shownMs = (frac * max.toFloat()).toLong()
            Scrubber(
                fraction = frac,
                onStart = { dragging = true },
                onChange = { dragFrac = it },
                onEnd = { f ->
                    val t = (f * max.toFloat()).toLong()
                    vm.seekTo(t)
                    pos = t
                    dragging = false
                }
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(fmt(shownMs), fontSize = 17.sp, color = Color(0x99FFFFFF))
                Spacer(Modifier.weight(1f))
                if (quality.isNotEmpty()) {
                    Icon(Icons.Filled.Headphones, null, tint = Color(0x99FFFFFF), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(quality, fontSize = 16.sp, color = Color(0x99FFFFFF), maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Text("-" + fmt(max - shownMs), fontSize = 17.sp, color = Color(0x99FFFFFF))
            }

            // controls: rewind | play | forward
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.prev() }, modifier = Modifier.size(80.dp).pressScale()) {
                    Icon(Icons.Filled.FastRewind, null, tint = Color.White, modifier = Modifier.size(56.dp))
                }
                Box(
                    Modifier.size(84.dp).pressScale().clip(CircleShape).clickable { vm.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (vm.loading) {
                        CircularProgressIndicator(
                            Modifier.size(40.dp), color = Color.White, strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color.White, modifier = Modifier.size(76.dp)
                        )
                    }
                }
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(80.dp).pressScale()) {
                    Icon(Icons.Filled.FastForward, null, tint = Color.White, modifier = Modifier.size(56.dp))
                }
            }

            // volume
            VolumeBar()

            // bottom: lyrics | [output | repeat] | queue, device name under the pill
            val outKind = vm.currentOutput?.kind
            val outIcon = when (outKind) {
                AudioOutput.BLUETOOTH -> Icons.Filled.Bluetooth
                AudioOutput.WIRED -> Icons.Filled.Headphones
                else -> Icons.Filled.PhoneAndroid
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarIcon(Icons.Filled.Subject) { vm.showLyrics = true }
                Row(
                    Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(50))
                        .glassFill(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(76.dp)
                            .fillMaxHeight()
                            .clickable {
                                vm.refreshOutputs()
                                vm.showOutputDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(outIcon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0x44FFFFFF))
                    )
                    Box(
                        Modifier
                            .width(76.dp)
                            .fillMaxHeight()
                            .clickable { vm.cycleRepeat() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (vm.repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            null,
                            tint = if (vm.repeat != 0) Pink else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                BarIcon(Icons.Filled.QueueMusic) { showQueue = true }
            }
            Text(
                vm.outputLabel, fontSize = 14.sp, color = Color(0x99FFFFFF),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
            )
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
internal fun QueuePanel(vm: MainViewModel, heightFrac: Float = 0.62f, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val itemPx = with(LocalDensity.current) { 60.dp.toPx() }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val keys = queueKeys(vm.queue)

    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFrac)
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
                    fontSize = 12.sp, color = Pal.textDim
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .glassFill()
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
                            if (dragging) Pal.accent.copy(alpha = 0.33f)
                            else if (cur) Pal.accent.copy(alpha = 0.2f)
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
                            s.artist, fontSize = 12.sp, color = Pal.textDim,
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
                            tint = Pal.textDim, modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
