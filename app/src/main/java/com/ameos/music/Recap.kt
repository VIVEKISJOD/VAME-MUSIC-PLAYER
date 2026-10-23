package com.ameos.music

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Recap: a Spotify-style story built from your real listening time.
// Tap the right side = next, left side = back. It moves on by itself.
@Composable
fun RecapScreen(vm: MainViewModel) {
    BackHandler { vm.recapOpen = false }
    val ctx = LocalContext.current
    val data = remember { vm.recap() }
    val empty = data.minutes == 0L && data.plays == 0
    val slides = if (empty) 1 else 6
    var index by remember { mutableIntStateOf(0) }
    val prog = remember { Animatable(0f) }
    LaunchedEffect(index) {
        prog.snapTo(0f)
        if (index < slides - 1) {
            prog.animateTo(1f, tween(6500, easing = LinearEasing))
            index += 1
        } else {
            prog.snapTo(1f)
        }
    }
    val hue = (Pal.hue + index * 52f) % 360f
    val top by animateColorAsState(
        Color.hsv(hue, (0.72f * Pal.sat).coerceIn(0f, 1f), 0.5f), tween(600), label = "recapBg"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, Color(0xFF050505))))
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    if (o.x < size.width * 0.33f) {
                        if (index > 0) index -= 1
                    } else if (index < slides - 1) {
                        index += 1
                    }
                }
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until slides) {
                    val frac = if (i < index) 1f else if (i == index) prog.value else 0f
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x44FFFFFF))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(frac.coerceIn(0f, 1f))
                                .background(Color.White)
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "VAME RECAP", fontSize = 12.sp, color = Color(0xCCFFFFFF), letterSpacing = 3.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.recapOpen = false }) {
                    Icon(Icons.Filled.Close, null, tint = Color.White)
                }
            }
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(380, delayMillis = 80)) +
                        slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { dir * (it / 5) }) togetherWith
                        (fadeOut(tween(160)) + slideOutHorizontally(tween(300)) { -dir * (it / 6) })
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "recapSlides"
            ) { i ->
                if (empty) RecapEmpty(vm)
                else when (i) {
                    0 -> RecapIntro(data)
                    1 -> RecapMinutes(data)
                    2 -> RecapTopArtist(data)
                    3 -> RecapTopSongs(data)
                    4 -> RecapTopArtists(data)
                    else -> RecapSummary(vm, data) {
                        val a = data.topArtists.firstOrNull()?.name ?: "-"
                        val s = data.topSongs.firstOrNull()?.title ?: "-"
                        val text = "My VAME recap: " + data.minutes + " minutes listened, top artist " + a +
                            ", top song " + s
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        ctx.startActivity(Intent.createChooser(send, "Share your recap").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun BigLine(text: String, size: Int, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        text, fontSize = size.sp, fontWeight = FontWeight.Bold, color = Color.White,
        lineHeight = (size + 4).sp, modifier = modifier,
        textAlign = if (center) TextAlign.Center else TextAlign.Start
    )
}

@Composable
private fun SmallLine(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        text, fontSize = 17.sp, color = Color(0xCCFFFFFF), modifier = modifier,
        textAlign = if (center) TextAlign.Center else TextAlign.Start
    )
}

@Composable
private fun RecapEmpty(vm: MainViewModel) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        BigLine("No recap yet", 44)
        SmallLine("Play some songs and come back. VAME counts the minutes you really listen.",
            Modifier.padding(top = 12.dp))
        Text(
            "Back to music", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 28.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x33FFFFFF))
                .clickable { vm.recapOpen = false }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun RecapIntro(d: RecapData) {
    val since = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(d.since))
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        BigLine("Your", 72)
        BigLine("Recap", 72)
        SmallLine("Everything you listened to since " + since + ".", Modifier.padding(top = 16.dp))
        SmallLine(d.plays.toString() + " plays so far", Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RecapMinutes(d: RecapData) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val shown by animateIntAsState(
        if (go) d.minutes.toInt() else 0, tween(1700, easing = FastOutSlowInEasing), label = "minCount"
    )
    val songs = (d.minutes * 60L / 210L).toInt()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        SmallLine("You listened for")
        BigLine(shown.toString(), 104)
        BigLine("minutes", 34)
        val hours = d.minutes / 60
        SmallLine(
            "That is about " + hours + (if (hours == 1L) " hour" else " hours") +
                ", or roughly " + songs + " songs back to back.",
            Modifier.padding(top = 18.dp)
        )
    }
}

@Composable
private fun RecapTopArtist(d: RecapData) {
    val a = d.topArtists.firstOrNull()
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SmallLine("Your top artist", center = true)
        Spacer(Modifier.height(18.dp))
        if (a != null) {
            Cover(a.cover, Modifier.size(230.dp).border(2.dp, Color(0x55FFFFFF), CircleShape), CircleShape, hiRes = true)
            BigLine(a.name, 40, Modifier.padding(top = 22.dp), center = true)
            SmallLine("You spent " + (a.sec / 60) + " minutes with them", Modifier.padding(top = 8.dp), center = true)
        } else {
            BigLine("Not enough yet", 34, center = true)
        }
    }
}

@Composable
private fun RecapTopSongs(d: RecapData) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        BigLine("Your top songs", 36, Modifier.padding(bottom = 14.dp))
        d.topSongs.forEachIndexed { i, s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .appear()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    (i + 1).toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.width(36.dp), textAlign = TextAlign.Center
                )
                Cover(s.cover, Modifier.size(54.dp), RoundedCornerShape(12.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(s.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(StatsStore.cleanArtist(s.artist), fontSize = 13.sp, color = Color(0xCCFFFFFF),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text((s.sec / 60).toString() + " min", fontSize = 13.sp, color = Color(0xCCFFFFFF))
            }
        }
    }
}

@Composable
private fun RecapTopArtists(d: RecapData) {
    val list = d.topArtists.take(5)
    val most = (list.firstOrNull()?.sec ?: 1L).coerceAtLeast(1L)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        BigLine("Your top artists", 36, Modifier.padding(bottom = 14.dp))
        list.forEachIndexed { i, a ->
            Row(
                Modifier.fillMaxWidth().appear().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    (i + 1).toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.width(34.dp)
                )
                Cover(a.cover, Modifier.size(52.dp), CircleShape)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(a.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(
                        Modifier.padding(top = 6.dp).fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF))
                    ) {
                        Box(
                            Modifier.fillMaxHeight()
                                .fillMaxWidth((a.sec.toFloat() / most.toFloat()).coerceIn(0.04f, 1f))
                                .clip(RoundedCornerShape(50)).background(Color.White)
                        )
                    }
                }
                Text((a.sec / 60).toString() + " min", fontSize = 13.sp, color = Color(0xCCFFFFFF),
                    modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@Composable
private fun RecapSummary(vm: MainViewModel, d: RecapData, onShare: () -> Unit) {
    val song = d.topSongs.firstOrNull()
    val artist = d.topArtists.firstOrNull()
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(Color(0x44FFFFFF), Color(0x14FFFFFF))))
                .border(1.dp, GlassEdge, RoundedCornerShape(30.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Cover(song?.cover, Modifier.size(150.dp), RoundedCornerShape(24.dp), hiRes = true)
            BigLine(d.minutes.toString() + " min", 44, Modifier.padding(top = 16.dp), center = true)
            SmallLine("listened \u00B7 " + d.plays + " plays", center = true)
            Spacer(Modifier.height(14.dp))
            SmallLine("Top artist: " + (artist?.name ?: "-"), center = true)
            SmallLine("Top song: " + (song?.title ?: "-"), center = true)
        }
        Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable { onShare() }
                    .padding(horizontal = 24.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Share, null, tint = Color(0xFF111111), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111111))
            }
            Text(
                "Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x33FFFFFF))
                    .clickable { vm.recapOpen = false }
                    .padding(horizontal = 28.dp, vertical = 13.dp)
            )
        }
    }
}
