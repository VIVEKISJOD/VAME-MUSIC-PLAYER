package com.ameos.music

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

// step73: the player in three styles (Material, Apple, Spotify). NowPlayingScreen calls these.

private fun pfmt(ms: Long): String {
    val s = (ms / 1000L).coerceAtLeast(0L)
    return (s / 60L).toString() + ":" + (s % 60L).toString().padStart(2, '0')
}

// ---------------- cover area ----------------
@Composable
internal fun PlayerCoverArt(vm: MainViewModel, song: Song, w: Dp, h: Dp) {
    val style = Pal.style
    val playing = vm.isPlaying
    val base = if (w < h) w else h
    val side = if (style == 1) base * 0.94f else base
    val r = St.playerCoverR
    val rest = if (style == 1) 0.80f else 0.94f
    val sc by animateFloatAsState(
        if (playing) 1f else rest,
        spring(dampingRatio = 0.7f, stiffness = 300f), label = "coverScale"
    )
    val shape = RoundedCornerShape(r)

    // audio pulse (Level 3): runs only while the player page is open
    val pulseOn = CanvasPrefs.pulse && CanvasPrefs.on
    DisposableEffect(pulseOn) {
        if (pulseOn) {
            CanvasPulse.start(vm.player.audioSessionId)
        }
        onDispose { CanvasPulse.stop() }
    }

    Box(
        Modifier
            .size(side)
            .graphicsLayer {
                scaleX = sc
                scaleY = sc
            }
            .then(if (style == 1) Modifier.shadow(28.dp, shape, clip = false) else Modifier)
            .clip(shape)
            .border(1.dp, Color(0x22FFFFFF), shape)
    ) {
        if (CanvasPrefs.on && CanvasPrefs.mode == 1 && style != 1) {
            val tint = rememberCoverTint(song.cover)
            CanvasBackdrop(tint, playing, Modifier.fillMaxSize())
            PlayerCanvas(
                song.cover, playing,
                Modifier.align(Alignment.Center).fillMaxSize(0.78f).clip(RoundedCornerShape(r * 0.7f))
            )
        } else {
            PlayerCanvas(song.cover, playing, Modifier.fillMaxSize())
        }
    }
}

// ---------------- small round icon ----------------
@Composable
private fun PIcon(icon: ImageVector, tint: Color, size: Dp = 48.dp, bg: Color? = null, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (bg != null) Modifier.background(bg) else Modifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

// ---------------- title row ----------------
@Composable
internal fun PlayerTitleRow(vm: MainViewModel, song: Song) {
    val style = Pal.style
    val ctx = LocalContext.current
    val liked = vm.isLiked(song)
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, start = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                song.title, fontSize = if (style == 1) 22.sp else 24.sp, fontWeight = FontWeight.Bold,
                color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                artistName(song.artist), fontSize = 16.sp, color = Color(0xB3FFFFFF),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (style == 1) {
            PIcon(
                if (liked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                if (liked) Pal.accent else Color.White
            ) { vm.toggleLike(song) }
            PIcon(Icons.Filled.MoreHoriz, Color.White) { vm.showMenu = true }
        } else {
            PIcon(Icons.Filled.Sensors, Color(0xCCFFFFFF), 44.dp, if (style == 0) Color(0x22FFFFFF) else null) {
                vm.startRadio()
                Toast.makeText(ctx, "Starting radio", Toast.LENGTH_SHORT).show()
            }
            Spacer(Modifier.width(4.dp))
            PIcon(
                if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                if (liked) Pal.accent else Color.White,
                48.dp,
                if (style == 0) Color(0x22FFFFFF) else null
            ) { vm.toggleLike(song) }
        }
    }
}

// ---------------- progress + times ----------------
@Composable
internal fun PlayerProgress(
    vm: MainViewModel, frac: Float, shownMs: Long, maxMs: Long, quality: String,
    onStart: () -> Unit, onChange: (Float) -> Unit, onEnd: (Float) -> Unit
) {
    val style = Pal.style
    Spacer(Modifier.height(10.dp))
    if (style == 0) {
        WavyScrubber(frac, vm.isPlaying, onStart, onChange, onEnd)
    } else {
        Scrubber(frac, onStart, onChange, onEnd, thin = true, fill = Color.White)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(pfmt(shownMs), fontSize = 14.sp, color = Color(0xB3FFFFFF))
        Spacer(Modifier.weight(1f))
        if (quality.isNotEmpty() && style != 1) {
            Text(quality, fontSize = 12.sp, color = Color(0x99FFFFFF), maxLines = 1)
            Spacer(Modifier.weight(1f))
        }
        Text(
            if (style == 1) "-" + pfmt(maxMs - shownMs) else pfmt(maxMs),
            fontSize = 14.sp, color = Color(0xB3FFFFFF)
        )
    }
}

// wavy progress bar (Material): the played part is a wave that moves while the song plays, flat when paused
@Composable
internal fun WavyScrubber(
    fraction: Float, playing: Boolean,
    onStart: () -> Unit, onChange: (Float) -> Unit, onEnd: (Float) -> Unit
) {
    var last by remember { mutableFloatStateOf(0f) }
    val startS by rememberUpdatedState(onStart)
    val changeS by rememberUpdatedState(onChange)
    val endS by rememberUpdatedState(onEnd)
    val amp by animateFloatAsState(if (playing && !UiOpts.reduceMotion) 1f else 0f, tween(450), label = "waveAmp")
    val inf = rememberInfiniteTransition(label = "wave")
    val shift by inf.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "waveShift"
    )
    val accent = Pal.accent
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        startS()
                        last = (off.x / size.width.toFloat()).coerceIn(0f, 1f)
                        changeS(last)
                    },
                    onDragEnd = { endS(last) },
                    onDragCancel = { endS(last) },
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
            }
    ) {
        val w = size.width
        val cy = size.height / 2f
        val x = w * fraction.coerceIn(0f, 1f)
        val stroke = 5.dp.toPx()
        drawLine(Color(0x33FFFFFF), Offset(x, cy), Offset(w, cy), stroke, StrokeCap.Round)
        val a = 5.dp.toPx() * amp
        val len = 24.dp.toPx()
        val path = Path()
        path.moveTo(0f, cy + a * sin(shift))
        var px = 0f
        while (px <= x) {
            path.lineTo(px, cy + a * sin(px / len * 6.2831855f + shift))
            px += 3f
        }
        drawPath(
            path, accent,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawCircle(Color.White, 7.dp.toPx(), Offset(x, cy + a * sin(x / len * 6.2831855f + shift)))
    }
}

// ---------------- controls ----------------
@Composable
private fun PlayGlyph(vm: MainViewModel, tint: Color, size: Dp) {
    if (vm.loading) {
        CircularProgressIndicator(Modifier.size(size * 0.8f), color = tint, strokeWidth = 3.dp)
    } else {
        Icon(
            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            null, tint = tint, modifier = Modifier.size(size)
        )
    }
}

@Composable
internal fun PlayerControls(vm: MainViewModel) {
    val ctx = LocalContext.current
    val style = Pal.style
    val doShuffle = {
        val ok = vm.shuffleUpNext()
        Toast.makeText(
            ctx, if (ok) "Up next shuffled" else "Not enough songs to shuffle", Toast.LENGTH_SHORT
        ).show()
    }
    val repeatIcon = if (vm.repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat
    val repeatTint = if (vm.repeat != 0) Pal.accent else Color.White
    if (style == 0) {
        // Material: tonal buttons, the play button changes its shape when it plays
        val r by animateDpAsState(if (vm.isPlaying) 26.dp else 44.dp, spring(dampingRatio = 0.6f, stiffness = 300f), label = "playR")
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PIcon(Icons.Filled.Shuffle, Color.White, 44.dp) { doShuffle() }
            Box(
                Modifier
                    .size(64.dp, 56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Pal.accent.copy(alpha = 0.22f))
                    .clickable { vm.prev() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            Box(
                Modifier
                    .size(104.dp, 88.dp)
                    .clip(RoundedCornerShape(r))
                    .background(Pal.accent)
                    .clickable { vm.toggle() },
                contentAlignment = Alignment.Center
            ) { PlayGlyph(vm, Pal.onAccent, 44.dp) }
            Box(
                Modifier
                    .size(64.dp, 56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Pal.accent.copy(alpha = 0.22f))
                    .clickable { vm.next() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            PIcon(repeatIcon, repeatTint, 44.dp) { vm.cycleRepeat() }
        }
    } else if (style == 1) {
        // Apple: plain big glyphs, then the volume bar
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(72.dp).clip(CircleShape).clickable { vm.prev() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
                Box(Modifier.size(88.dp).clip(CircleShape).clickable { vm.toggle() }, contentAlignment = Alignment.Center) {
                    PlayGlyph(vm, Color.White, 68.dp)
                }
                Box(Modifier.size(72.dp).clip(CircleShape).clickable { vm.next() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            VolumeBar()
        }
    } else {
        // Spotify: shuffle, previous, white round play, next, repeat
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PIcon(Icons.Filled.Shuffle, Color.White, 48.dp) { doShuffle() }
            Box(Modifier.size(64.dp).clip(CircleShape).clickable { vm.prev() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { vm.toggle() },
                contentAlignment = Alignment.Center
            ) { PlayGlyph(vm, Color(0xFF101010), 44.dp) }
            Box(Modifier.size(64.dp).clip(CircleShape).clickable { vm.next() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            PIcon(repeatIcon, repeatTint, 48.dp) { vm.cycleRepeat() }
        }
    }
}

// ---------------- bottom row ----------------
private class PItem(val icon: ImageVector, val on: Boolean, val click: () -> Unit)

@Composable
internal fun PlayerBottomRow(vm: MainViewModel, song: Song, page: Int, onQueue: () -> Unit) {
    val ctx = LocalContext.current
    val style = Pal.style
    val outIcon = when (vm.currentOutput?.kind) {
        AudioOutput.BLUETOOTH -> Icons.Filled.Bluetooth
        AudioOutput.WIRED -> Icons.Filled.Headphones
        else -> Icons.Filled.PhoneAndroid
    }
    fun pg(n: Int) {
        PlayerUi.page = if (PlayerUi.page == n) 0 else n
    }
    val lyrics = PItem(Icons.Filled.Subject, page == 1) { pg(1) }
    val effects = PItem(Icons.Filled.Tune, page == 2) { pg(2) }
    val info = PItem(Icons.Filled.Info, page == 3) { pg(3) }
    val output = PItem(outIcon, false) {
        vm.refreshOutputs()
        vm.showOutputDialog = true
    }
    val queue = PItem(Icons.Filled.QueueMusic, false) { onQueue() }
    val share = PItem(Icons.Filled.Share, false) {
        val send = Intent(Intent.ACTION_SEND)
        send.type = "text/plain"
        send.putExtra(Intent.EXTRA_TEXT, song.title + " - " + artistName(song.artist) + "\n" + song.url)
        val chooser = Intent.createChooser(send, null)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(chooser)
    }
    val radio = PItem(Icons.Filled.Sensors, false) {
        vm.startRadio()
        Toast.makeText(ctx, "Starting radio", Toast.LENGTH_SHORT).show()
    }
    val items = when (style) {
        0 -> listOf(lyrics, effects, info, output, queue)
        1 -> listOf(lyrics, output, queue, radio, effects, info)
        else -> listOf(output, share, lyrics, effects, info, queue)
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { it ->
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .then(if (it.on) Modifier.background(Pal.accent.copy(alpha = 0.22f)) else Modifier)
                    .clickable { it.click() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    it.icon, null,
                    tint = if (it.on) Pal.accent else Color(0xDDFFFFFF),
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}
