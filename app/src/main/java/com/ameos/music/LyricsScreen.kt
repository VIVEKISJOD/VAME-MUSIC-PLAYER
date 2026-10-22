package com.ameos.music

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun LyricsScreen(vm: MainViewModel) {
    val song = vm.current ?: return
    BackHandler { vm.showLyrics = false }
    val tint = rememberCoverTint(song.cover)

    var pos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(song.url) {
        while (true) {
            pos = vm.player.currentPosition
            delay(100)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(lerp(tint, Color.Black, 0.5f))
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
            // top pill: cover, title, artist
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x44FFFFFF))
                    .clickable { vm.showLyrics = false }
                    .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cover(song.cover, Modifier.size(46.dp), CircleShape)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.widthIn(max = 200.dp)) {
                    Text(
                        song.title, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD0D0D0), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Filled.Equalizer, null, tint = Color(0xFFD0D0D0), modifier = Modifier.size(22.dp))
            }

            // lyrics
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LyricsList(vm, pos)
            }

            // bottom controls
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x44FFFFFF))
                        .clickable { vm.showLyrics = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFFC9C9C9))
                        .clickable { vm.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (vm.loading) {
                        CircularProgressIndicator(
                            Modifier.size(30.dp), color = Color(0xFF1B1315), strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color(0xFF1B1315), modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                PlayerMenuButton(vm, 56.dp)
            }
        }
    }
}

@Composable
private fun LyricsList(vm: MainViewModel, pos: Long) {
    val lines = vm.lyrics
    if (vm.lyricsLoading || lines.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (vm.lyricsLoading) "Loading lyrics..." else "No lyrics found for this song",
                color = Color(0xFFBBBBBB), fontSize = 18.sp, textAlign = TextAlign.Center
            )
        }
        return
    }
    val synced = vm.lyricsSynced
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
            val blurTarget = if (!synced || active < 0) 0.dp else when (dist) {
                0 -> 0.dp
                1 -> 4.dp
                2 -> 8.dp
                else -> 12.dp
            }
            val alphaTarget = if (!synced) 0.85f else when {
                active < 0 -> 0.6f
                dist == 0 -> 1f
                dist == 1 -> 0.6f
                else -> 0.4f
            }
            val blurR by animateDpAsState(blurTarget, tween(450), label = "blur")
            val a by animateFloatAsState(alphaTarget, tween(450), label = "alpha")
            val text = if (line.text.isBlank()) "..." else line.text
            val base = Modifier
                .fillMaxWidth()
                .clickable(enabled = synced) { vm.player.seekTo(line.timeMs) }
                .padding(horizontal = 28.dp, vertical = 14.dp)
            val mod = if (blurR > 0.dp) base.blur(blurR) else base

            if (synced && i == active) {
                val nextT = lines.getOrNull(i + 1)?.timeMs ?: (line.timeMs + 4000)
                val span = (nextT - line.timeMs).coerceAtLeast(1L)
                val frac = ((pos - line.timeMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
                val words = text.split(" ")
                val p = frac * 1.15f * words.size
                val styled = buildAnnotatedString {
                    words.forEachIndexed { wi, w ->
                        val t = (p - wi).coerceIn(0f, 1f)
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.40f + 0.60f * t))) {
                            append(w)
                        }
                        if (wi < words.size - 1) append(" ")
                    }
                }
                Text(
                    styled, fontSize = 34.sp, lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = mod
                )
            } else {
                Text(
                    text, fontSize = 34.sp, lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = a),
                    modifier = mod
                )
            }
        }
    }
}
