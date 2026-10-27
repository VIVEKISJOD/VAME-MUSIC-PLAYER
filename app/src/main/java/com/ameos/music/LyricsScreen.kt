package com.ameos.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

// keeps the screen awake while lyrics are open (Settings switch)
@Composable
private fun KeepScreenOn(vm: MainViewModel) {
    val view = LocalView.current
    DisposableEffect(vm.keepScreenLyrics) {
        val old = view.keepScreenOn
        view.keepScreenOn = vm.keepScreenLyrics
        onDispose { view.keepScreenOn = old }
    }
}

// Tablet lyrics mode: the whole screen. Cover (canvas) + song name on the left,
// lyrics on the right, and only a back button at the bottom right.
@Composable
fun TabletLyricsScreen(vm: MainViewModel) {
    val song = vm.current ?: return
    BackHandler { vm.showLyrics = false }
    KeepScreenOn(vm)
    val tint = rememberCoverTint(song.cover)
    var pos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(song.url) {
        while (true) {
            pos = vm.posMs()
            delay(100)
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(lerp(tint, Color.Black, if (Pal.amoled) 0.92f else 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // left: cover + name. On a short screen (phone sideways, Standby) everything is smaller.
            BoxWithConstraints(Modifier.weight(0.42f).fillMaxHeight()) {
                val small = maxHeight < 520.dp
                val side = minOf(maxWidth - 50.dp, 440.dp, maxHeight - (if (small) 100.dp else 150.dp))
                    .coerceAtLeast(110.dp)
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(start = if (small) 26.dp else 44.dp, end = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Cover(
                        song.cover,
                        Modifier.size(side),
                        RoundedCornerShape(if (small) 20.dp else 28.dp),
                        hiRes = true
                    )
                    Spacer(Modifier.height(if (small) 12.dp else 22.dp))
                    Text(
                        song.title, fontSize = if (small) 20.sp else 30.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, maxLines = if (small) 1 else 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        artistName(song.artist), fontSize = if (small) 15.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD0D0D0), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            BoxWithConstraints(Modifier.weight(0.58f).fillMaxHeight()) {
                val tools = maxHeight >= 480.dp
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        LyricsList(vm, pos)
                    }
                    if (tools) {
                        LyricsTools(vm, false, { }, Modifier.padding(end = 90.dp), showFull = false)
                    }
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 28.dp, bottom = 24.dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(Color(0x55FFFFFF))
                .clickable { vm.showLyrics = false },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
        }
    }
}

@Composable
fun LyricsScreen(vm: MainViewModel) {
    val song = vm.current ?: return
    BackHandler { vm.showLyrics = false }
    var full by remember { mutableStateOf(false) }
    BackHandler(enabled = full) { full = false }
    KeepScreenOn(vm)
    val tint = rememberCoverTint(song.cover)

    var pos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(song.url) {
        while (true) {
            pos = vm.posMs()
            delay(100)
        }
    }
    val label = when {
        vm.lyricsLoading -> "SEARCHING LYRICS..."
        vm.lyrics.isEmpty() -> "NO LYRICS FOUND"
        else -> (if (vm.lyricsSynced) "SYNCED" else "PLAIN TEXT") +
            (if (vm.lyricsSource.isNotBlank()) " \u2022 " + vm.lyricsSource else "")
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(lerp(tint, Color.Black, if (Pal.amoled) 0.92f else 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            AnimatedVisibility(visible = !full) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LyricsCircle(46.dp, { vm.showLyrics = false }) {
                            Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Column(
                            Modifier.weight(1f).padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("LYRICS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                song.title + " \u2022 " + artistName(song.artist), fontSize = 13.sp,
                                color = Color(0xFFD0D0D0), maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        LyricsCircle(46.dp, { vm.showSleepDialog = true }, vm.sleepLeftSec > 0 || vm.sleepAfterSong) {
                            Icon(Icons.Filled.Timer, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        PlayerMenuButton(vm, 46.dp)
                    }
                    Text(
                        label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD0D0D0),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.showProviderSheet = true }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                LyricsList(vm, pos)
            }

            AnimatedVisibility(visible = !full) {
                Column(Modifier.fillMaxWidth()) {
                    LyricsTools(vm, full, { full = !full })
                    WaveScrubber(vm, Modifier.padding(horizontal = 22.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LyricsCircle(58.dp, { vm.prev() }) {
                            Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        LyricsCircle(78.dp, { vm.toggle() }) {
                            if (vm.loading) {
                                CircularProgressIndicator(Modifier.size(30.dp), color = Color.White, strokeWidth = 3.dp)
                            } else {
                                Icon(
                                    if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    null, tint = Color.White, modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        LyricsCircle(58.dp, { vm.next() }) {
                            Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
        if (full) {
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)) {
                LyricsCircle(46.dp, { full = false }) {
                    Icon(Icons.Filled.FullscreenExit, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun LyricsList(vm: MainViewModel, pos: Long) {
    val lines = vm.lyrics
    if (vm.lyricsLoading || lines.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (vm.lyricsLoading) "Loading lyrics..." else "No lyrics found for this song",
                color = Color(0xFFBBBBBB), fontSize = 18.sp, textAlign = TextAlign.Center
            )
            if (!vm.lyricsLoading) {
                Text(
                    "Try another source", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x33FFFFFF))
                        .clickable { vm.showProviderSheet = true }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
        return
    }
    val synced = vm.lyricsSynced
    val fs = (34 * vm.lyricsPct.coerceIn(50, 160) / 100).coerceAtLeast(14)
    val trSize = (fs * 55 / 100).coerceAtLeast(12)
    val trOn = vm.lyricsTransOn && vm.lyricsTrans.size == lines.size
    val al = if (vm.lyricsLeft) TextAlign.Start else TextAlign.Center
    val active = if (synced) lines.indexOfLast { it.timeMs <= pos } else -1
    val state = rememberLazyListState()
    LaunchedEffect(active) {
        if (active >= 0) state.animateScrollToItem(active, -280)
    }
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 120.dp, bottom = 300.dp)
    ) {
        itemsIndexed(lines) { i, line ->
            val dist = if (active < 0) 0 else abs(i - active)
            val blurK = when (UiOpts.lyricsBlur) {
                1 -> 0.4f
                2 -> 0f
                else -> 1f
            }
            val blurBase = when (dist) {
                0 -> 0f
                1 -> 4f
                2 -> 8f
                else -> 12f
            }
            val blurTarget = if (!synced || active < 0) 0.dp else (blurBase * blurK).dp
            val alphaTarget = if (!synced) 0.85f else when {
                active < 0 -> 0.6f
                dist == 0 -> 1f
                dist == 1 -> 0.6f
                else -> 0.4f
            }
            val blurR by animateDpAsState(blurTarget, tween(450), label = "blur")
            val a by animateFloatAsState(alphaTarget, tween(450), label = "alpha")
            val text = if (line.text.isBlank()) "..." else line.text
            val tr = if (trOn) vm.lyricsTrans[i] else ""
            val base = Modifier
                .fillMaxWidth()
                .clickable(enabled = synced) { vm.seekTo(line.timeMs) }
                .padding(horizontal = 28.dp, vertical = 14.dp)
            val mod = if (blurR > 0.dp) base.blur(blurR) else base

            Column(mod) {
                if (synced && i == active) {
                    val nextT = lines.getOrNull(i + 1)?.timeMs ?: (line.timeMs + 4000)
                    val span = (nextT - line.timeMs).coerceAtLeast(1L)
                    // words are spread over the time the line is really sung (not the gap after it)
                    val sung = minOf(span, text.length * 110L + 800L).coerceAtLeast(1L)
                    val frac = ((pos - line.timeMs).toFloat() / sung.toFloat()).coerceIn(0f, 1f)
                    val words = text.split(" ")
                    val total = words.fold(0) { acc, w -> acc + w.length + 1 }.coerceAtLeast(1)
                    val p = frac * 1.08f * total
                    var startC = 0
                    val styled = buildAnnotatedString {
                        words.forEachIndexed { wi, w ->
                            val t = ((p - startC) / (w.length + 1).toFloat()).coerceIn(0f, 1f)
                            startC += w.length + 1
                            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.40f + 0.60f * t))) {
                                append(w)
                            }
                            if (wi < words.size - 1) append(" ")
                        }
                    }
                    Text(
                        styled, fontSize = fs.sp, lineHeight = (fs + 8).sp,
                        fontWeight = FontWeight.Bold, textAlign = al,
                        style = TextStyle(
                            shadow = Shadow(Color.White.copy(alpha = 0.55f), Offset.Zero, 24f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text, fontSize = fs.sp, lineHeight = (fs + 8).sp,
                        fontWeight = FontWeight.Bold, textAlign = al,
                        color = Color.White.copy(alpha = a),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (tr.isNotBlank()) {
                    Text(
                        tr, fontSize = trSize.sp, lineHeight = (trSize + 6).sp,
                        fontWeight = FontWeight.Medium, textAlign = al,
                        color = Color.White.copy(alpha = a * 0.7f),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
