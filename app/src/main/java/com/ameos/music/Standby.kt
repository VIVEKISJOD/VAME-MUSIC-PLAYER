package com.ameos.music

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val StbDim = Color(0x99FFFFFF)

private fun stbTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}

private fun Context.stbActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

// Standby = the phone is lying sideways (on a stand or a charger) and plays music.
// While it is open: the screen can stay awake and the status bar can hide (Settings > Standby).
@Composable
private fun StandbyScreenSettings() {
    val view = LocalView.current
    DisposableEffect(UiOpts.standbyAwake) {
        val old = view.keepScreenOn
        view.keepScreenOn = UiOpts.standbyAwake
        onDispose { view.keepScreenOn = old }
    }
    DisposableEffect(UiOpts.standbyHideBars) {
        val win = view.context.stbActivity()?.window
        var ctl: WindowInsetsControllerCompat? = null
        if (win != null && UiOpts.standbyHideBars) {
            try {
                val c = WindowCompat.getInsetsController(win, view)
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                c.hide(WindowInsetsCompat.Type.systemBars())
                ctl = c
            } catch (e: Exception) {
            }
        }
        onDispose {
            try {
                ctl?.show(WindowInsetsCompat.Type.systemBars())
            } catch (e: Exception) {
            }
        }
    }
}

@Composable
fun StandbyPlayer(vm: MainViewModel) {
    val song = vm.current ?: return
    var showQueue by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(0f) }
    var quality by remember { mutableStateOf("") }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var clockFull by remember { mutableStateOf(false) }
    val tint = rememberCoverTint(song.cover)
    // on a tablet this standby is an overlay: closing it keeps the side player as it was
    val closeStb: () -> Unit = { if (vm.tabStandby) vm.tabStandby = false else vm.showPlayer = false }

    BackHandler { closeStb() }
    BackHandler(enabled = showQueue) { showQueue = false }
    StandbyScreenSettings()

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
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val lines = vm.lyrics
    val curLine: String? = if (vm.lyricsSynced && lines.isNotEmpty()) {
        val i = lines.indexOfLast { it.timeMs <= pos }
        if (i >= 0) lines[i].text.ifBlank { "♪" } else null
    } else null

    // the cover gets a little smaller while the song is paused
    val coverScale by animateFloatAsState(
        if (vm.isPlaying) 1f else 0.92f,
        spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "stbCover"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Pal.bgBot,
                        lerp(tint, Color.Black, if (Pal.amoled) 0.9f else 0.55f),
                        lerp(tint, Color.Black, if (Pal.amoled) 1f else 0.8f)
                    )
                )
            )
            .blockTaps()
    ) {
        Cover(
            song.cover,
            Modifier.fillMaxSize().alpha(0.22f).blur(90.dp),
            RoundedCornerShape(0.dp),
            hiRes = true
        )

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 26.dp, vertical = 16.dp)
        ) {
            val side = minOf(maxHeight, maxWidth * 0.44f)
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                // left: the cover
                Box(Modifier.size(side), contentAlignment = Alignment.Center) {
                    Cover(
                        song.cover,
                        Modifier
                            .size(side)
                            .graphicsLayer {
                                scaleX = coverScale
                                scaleY = coverScale
                            }
                            .shadow(24.dp, RoundedCornerShape(24.dp)),
                        RoundedCornerShape(24.dp),
                        hiRes = true
                    )
                }
                Spacer(Modifier.width(30.dp))

                // right: title, bar, buttons, volume
                Box(
                    Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                song.title, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                artistName(song.artist), fontSize = 17.sp, color = StbDim,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        closeStb()
                                        vm.showLyrics = false
                                        vm.openArtist(song.artist)
                                    }
                            )
                        }
                        IconButton(onClick = { vm.toggleLike(song) }) {
                            Icon(
                                if (vm.isLiked(song)) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                null,
                                tint = if (vm.isLiked(song)) Pal.accent else Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = { vm.showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }

                    // the lyric line being sung (tap = full lyrics)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { vm.showLyrics = true },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Crossfade(targetState = curLine, animationSpec = tween(350), label = "stbLine") { t ->
                            if (t != null) {
                                Text(
                                    t, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

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
                        Text(stbTime(shownMs), fontSize = 14.sp, color = StbDim)
                        Spacer(Modifier.weight(1f))
                        if (quality.isNotEmpty()) {
                            Icon(Icons.Filled.Headphones, null, tint = StbDim, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(quality, fontSize = 13.sp, color = StbDim, maxLines = 1)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("-" + stbTime(max - shownMs), fontSize = 14.sp, color = StbDim)
                    }

                    // rewind | play | forward
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { vm.prev() }, modifier = Modifier.size(72.dp).pressScale()) {
                            Icon(Icons.Filled.FastRewind, null, tint = Color.White, modifier = Modifier.size(46.dp))
                        }
                        Box(
                            Modifier.size(76.dp).pressScale().clip(CircleShape).clickable { vm.toggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (vm.loading) {
                                CircularProgressIndicator(
                                    Modifier.size(34.dp), color = Color.White, strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    null, tint = Color.White, modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                        IconButton(onClick = { vm.next() }, modifier = Modifier.size(72.dp).pressScale()) {
                            Icon(Icons.Filled.FastForward, null, tint = Color.White, modifier = Modifier.size(46.dp))
                        }
                    }

                    VolumeBar()

                    val outKind = vm.currentOutput?.kind
                    val outIcon = when (outKind) {
                        AudioOutput.BLUETOOTH -> Icons.Filled.Bluetooth
                        AudioOutput.WIRED -> Icons.Filled.Headphones
                        else -> Icons.Filled.PhoneAndroid
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BarIcon(Icons.Filled.Subject) { vm.showLyrics = true }
                        Row(
                            Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(50))
                                .glassFill(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .width(64.dp)
                                    .fillMaxHeight()
                                    .clickable {
                                        vm.refreshOutputs()
                                        vm.showOutputDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(outIcon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Box(
                                Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(Color(0x44FFFFFF))
                            )
                            Box(
                                Modifier
                                    .width(64.dp)
                                    .fillMaxHeight()
                                    .clickable { vm.cycleRepeat() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (vm.repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                    null,
                                    tint = if (vm.repeat != 0) Pal.accent else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        BarIcon(Icons.Filled.QueueMusic) { showQueue = true }
                    }
                    Text(
                        vm.outputLabel, fontSize = 12.sp, color = StbDim,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    )
                }
                }
            }
        }

        // clock card, top right (the status bar is hidden in standby); tap = full screen clock
        StbClockPill(
            nowMs, tint,
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = 6.dp, end = 20.dp)
        ) { clockFull = true }

        // handle on top: tap or drag down to close
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .width(120.dp)
                .height(26.dp)
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            total += dy
                        },
                        onDragEnd = { if (total > 60f) closeStb() },
                        onDragCancel = { total = 0f }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { closeStb() },
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

        AnimatedVisibility(
            visible = clockFull,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(250))
        ) {
            StbClockFull(nowMs, tint, vm) { clockFull = false }
        }

        AnimatedVisibility(
            visible = showQueue,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = slideInVertically(tween(350)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(200))
        ) {
            Box(
                Modifier.width(470.dp).fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                QueuePanel(vm, 0.94f) { showQueue = false }
            }
        }
    }
}
