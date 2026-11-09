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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset

private val Pink: Color get() = Pal.accent
private val SoftWhite = Color(0xFFEDEDED)

private fun fmtShort(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}

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
    onEnd: (Float) -> Unit,
    thin: Boolean = false
) {
    var active by remember { mutableStateOf(false) }
    var last by remember { mutableFloatStateOf(0f) }
    val h by animateDpAsState(
        if (thin) (if (active) 10.dp else 6.dp) else (if (active) 18.dp else 14.dp), spring(), label = "scrubH"
    )
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

// step64: which page of the player is open (0 cover, 1 lyrics, 2 effects, 3 info)
object PlayerUi {
    var page by mutableIntStateOf(0)
}

// round glass button used all over the player
@Composable
private fun GlassCircle(size: Dp, onClick: () -> Unit, lit: Boolean = false, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(size)
            .pressScale(0.92f)
            .glassBar(CircleShape, if (lit) Pal.accent else null)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun SectionCaps(text: String) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
        color = Color(0x99FFFFFF), modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

// slider with a white pill as the thumb (Effects page)
@Composable
private fun PillSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onDone: () -> Unit
) {
    var w by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val thumbPx = with(density) { 46.dp.toPx() }
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val frac = ((value - range.start) / span).coerceIn(0f, 1f)
    val changeS by rememberUpdatedState(onChange)
    val doneS by rememberUpdatedState(onDone)
    fun fromX(x: Float): Float {
        val usable = (w - thumbPx).coerceAtLeast(1f)
        val f = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
        return range.start + f * span
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .onSizeChanged { w = it.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off -> changeS(fromX(off.x)) },
                    onDragEnd = { doneS() },
                    onDragCancel = { doneS() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        changeS(fromX(change.position.x))
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    changeS(fromX(off.x))
                    doneS()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color(0x40FFFFFF))
        )
        val fillW = with(density) { ((w - thumbPx) * frac + thumbPx / 2f).toDp() }
        Box(
            Modifier
                .width(fillW)
                .height(8.dp)
                .clip(CircleShape)
                .background(Color(0x99FFFFFF))
        )
        Box(
            Modifier
                .offset { IntOffset(((w - thumbPx) * frac).roundToInt(), 0) }
                .size(46.dp, 30.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun EffectRow(label: String, valueText: String, onReset: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
            color = Color(0x99FFFFFF), modifier = Modifier.weight(1f)
        )
        Text(valueText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(
            "Reset", fontSize = 14.sp, color = Color(0xCCFFFFFF),
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onReset() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TextChip(text: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .then(if (on) Modifier.glassActiveTab(shape) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text, fontSize = 16.sp,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            color = if (on) Color.White else Color(0xB3FFFFFF)
        )
    }
}

private fun near(a: Float, b: Float, eps: Float): Boolean = Math.abs(a - b) <= eps

@Composable
private fun EffectsPage(vm: MainViewModel) {
    val speed = vm.speed
    val semis = SoundFx.pitchSemis
    val rev = SoundFx.reverb
    val sel = when {
        near(speed, 1f, 0.02f) && near(semis, 0f, 0.15f) && rev == 0 -> 0
        near(speed, 0.85f, 0.02f) && near(semis, -2.8f, 0.15f) && rev == 60 -> 1
        near(speed, 1.25f, 0.02f) && near(semis, 3.9f, 0.15f) && rev == 0 -> 2
        else -> 3
    }
    var sleepSel by remember { mutableIntStateOf(0) }
    val sleepMin = listOf(0, 5, 15, 30, 45)
    val running = vm.sleepLeftSec > 0 || vm.sleepAfterSong
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            "PRESETS", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
            color = Color(0x99FFFFFF), modifier = Modifier.padding(top = 6.dp)
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextChip("Original", sel == 0) { SoundFx.setEffects(vm, 1f, 0f, 0); SoundFx.save() }
            TextChip("Slowed + Reverb", sel == 1) { SoundFx.setEffects(vm, 0.85f, -2.8f, 60); SoundFx.save() }
            TextChip("Sped Up", sel == 2) { SoundFx.setEffects(vm, 1.25f, 3.9f, 0); SoundFx.save() }
            TextChip("Custom", sel == 3) { }
        }
        EffectRow("SPEED", "%.2f".format(speed) + "×") { SoundFx.setEffects(vm, 1f, semis, rev); SoundFx.save() }
        PillSlider(speed, 0.5f..2f, { SoundFx.setEffects(vm, Math.round(it * 100f) / 100f, semis, rev) }) { SoundFx.save() }
        EffectRow("PITCH", (if (semis >= 0f) "+" else "") + "%.1f".format(semis) + " st") {
            SoundFx.setEffects(vm, speed, 0f, rev); SoundFx.save()
        }
        PillSlider(semis, -12f..12f, { SoundFx.setEffects(vm, speed, Math.round(it * 10f) / 10f, rev) }) { SoundFx.save() }
        EffectRow("REVERB", rev.toString() + "%") { SoundFx.setEffects(vm, speed, semis, 0); SoundFx.save() }
        PillSlider(rev.toFloat(), 0f..100f, { SoundFx.setEffects(vm, speed, semis, it.roundToInt()) }) { SoundFx.save() }

        Text(
            "VOLUME BOOST", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
            color = Color(0x99FFFFFF), modifier = Modifier.padding(top = 18.dp)
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SoundFx.boostNames.forEachIndexed { i, n ->
                TextChip(n, SoundFx.boost == i) { SoundFx.pickBoost(i) }
            }
        }

        Text(
            "Sleep timer" + (if (vm.sleepLeftSec > 0) "  •  stops in " + (vm.sleepLeftSec / 60) + ":" +
                (vm.sleepLeftSec % 60).toString().padStart(2, '0') else if (vm.sleepAfterSong) "  •  after this song" else ""),
            fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
            color = Color(0x99FFFFFF), modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sleepMin.forEachIndexed { i, m ->
                val on = if (!running) i == 0 else i == sleepSel
                TextChip(if (m == 0) "Off" else m.toString() + " min", on) {
                    sleepSel = i
                    vm.setSleep(m)
                }
            }
        }
        Text(
            "VOLUME", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
            color = Color(0x99FFFFFF), modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
        )
        VolumeBar()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color(0x99FFFFFF))
        Text(value, fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun InfoPage(vm: MainViewModel, song: Song, quality: String) {
    val len = if (song.durationSec > 0) fmt(song.durationSec * 1000) else "-"
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        InfoRow("TITLE", song.title)
        InfoRow("ARTIST", artistName(song.artist))
        InfoRow("LENGTH", len)
        InfoRow("AUDIO QUALITY", if (quality.isEmpty()) "-" else quality)
        InfoRow("SOURCE", "YouTube Music")
        InfoRow("OUTPUT", vm.outputLabel)
        val shape = RoundedCornerShape(50)
        Box(
            Modifier
                .padding(top = 14.dp)
                .pressScale(0.96f)
                .glassBar(shape)
                .clickable {
                    vm.showPlayer = false
                    vm.showLyrics = false
                    vm.openArtist(song.artist)
                }
                .padding(horizontal = 22.dp, vertical = 13.dp)
        ) {
            Text("Open artist", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LyricsPage(vm: MainViewModel, pos: Long) {
    var panel by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LyricsList(vm, pos, 16.dp, 140.dp)
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCircle(46.dp, { vm.toggleLyricsTrans(!vm.lyricsTransOn) }, vm.lyricsTransOn) {
                Icon(Icons.Filled.Translate, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            GlassCircle(46.dp, { panel = !panel }, panel) {
                Icon(Icons.Filled.TextFields, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            GlassCircle(46.dp, { vm.showProviderSheet = true }) {
                Icon(Icons.Filled.LibraryMusic, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        AnimatedVisibility(
            visible = panel,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 2 }
        ) {
            LyricsStylePanel(vm, Modifier.padding(bottom = 4.dp))
        }
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
    val ctx = LocalContext.current
    val kb = rememberInfiniteTransition(label = "kb")
    val kbScale by kb.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "kbScale"
    )
    val page = PlayerUi.page
    DisposableEffect(Unit) {
        onDispose { PlayerUi.page = 0 }
    }

    BackHandler { vm.showPlayer = false }
    BackHandler(enabled = PlayerUi.page != 0) { PlayerUi.page = 0 }
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

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(tint, Color(0xFF8E8780), 0.45f),
                        lerp(tint, Color(0xFF5E5852), 0.5f),
                        lerp(tint, Color(0xFF3A3632), 0.6f)
                    )
                )
            )
            .blockTaps()
    ) {
        val colW = minOf(maxWidth, 560.dp)

        // soft blurred cover behind everything (fills wide screens too)
        Cover(
            song.cover,
            Modifier.fillMaxSize().alpha(0.30f).blur(80.dp),
            RoundedCornerShape(0.dp),
            hiRes = true
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .width(colW)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // top: close | page title | menu | output
            val outKind = vm.currentOutput?.kind
            val outIcon = when (outKind) {
                AudioOutput.BLUETOOTH -> Icons.Filled.Bluetooth
                AudioOutput.WIRED -> Icons.Filled.Headphones
                else -> Icons.Filled.PhoneAndroid
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp).swipeDown { vm.showPlayer = false },
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCircle(48.dp, { vm.showPlayer = false }) {
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(
                    when (page) {
                        1 -> "Lyrics"
                        2 -> "Effects"
                        3 -> "Info"
                        else -> artistName(song.artist)
                    },
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = page == 0) {
                            vm.showPlayer = false
                            vm.showLyrics = false
                            vm.openArtist(song.artist)
                        }
                )
                GlassCircle(48.dp, { vm.showMenu = true }) {
                    Icon(Icons.Filled.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(8.dp))
                GlassCircle(48.dp, {
                    vm.refreshOutputs()
                    vm.showOutputDialog = true
                }) {
                    Icon(outIcon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // the four pages (swipe left / right, or use the pill at the bottom)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onHorizontalDrag = { _, dx -> total += dx },
                            onDragEnd = {
                                if (total < -90f) PlayerUi.page = minOf(3, PlayerUi.page + 1)
                                else if (total > 90f) PlayerUi.page = maxOf(0, PlayerUi.page - 1)
                            }
                        )
                    }
            ) {
                Crossfade(targetState = page, animationSpec = tween(240), label = "playerPage") { pg ->
                    when (pg) {
                        1 -> LyricsPage(vm, pos)
                        2 -> EffectsPage(vm)
                        3 -> InfoPage(vm, song, quality)
                        else -> BoxWithConstraints(
                            Modifier
                                .fillMaxSize()
                                .swipeDown { vm.showPlayer = false }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { moving = !moving },
                            contentAlignment = Alignment.Center
                        ) {
                            val side = minOf(maxWidth, maxHeight)
                            val sc by animateFloatAsState(
                                if (vm.isPlaying) 1f else 0.92f,
                                spring(dampingRatio = 0.7f, stiffness = 300f), label = "coverScale"
                            )
                            Box(
                                Modifier
                                    .size(side)
                                    .graphicsLayer {
                                        scaleX = sc
                                        scaleY = sc
                                    }
                                    .clip(RoundedCornerShape(26.dp))
                            ) {
                                Cover(
                                    song.cover,
                                    Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val k = if (moving) kbScale else 1f
                                            scaleX = k
                                            scaleY = k
                                        },
                                    RoundedCornerShape(0.dp),
                                    hiRes = true
                                )
                            }
                        }
                    }
                }
            }

            // title + artist pill with radio | queue | like
            val pillShape = RoundedCornerShape(36.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .glassBar(pillShape)
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        artistName(song.artist), fontSize = 16.sp, color = Color(0xB3FFFFFF),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                GlassCircle(46.dp, {
                    vm.startRadio()
                    Toast.makeText(ctx, "Starting radio", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Filled.Sensors, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(6.dp))
                GlassCircle(46.dp, { showQueue = true }) {
                    Icon(Icons.Filled.QueueMusic, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(6.dp))
                GlassCircle(46.dp, { vm.toggleLike(song) }) {
                    Icon(
                        if (vm.isLiked(song)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        null,
                        tint = if (vm.isLiked(song)) Pink else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // progress (thin) + times
            val max = dur.coerceAtLeast(1L)
            val frac = if (dragging) dragFrac else (pos.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            val shownMs = (frac * max.toFloat()).toLong()
            Spacer(Modifier.height(10.dp))
            Scrubber(
                fraction = frac,
                onStart = { dragging = true },
                onChange = { dragFrac = it },
                onEnd = { f ->
                    val t = (f * max.toFloat()).toLong()
                    vm.seekTo(t)
                    pos = t
                    dragging = false
                },
                thin = true
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(fmtShort(shownMs), fontSize = 15.sp, color = Color(0xB3FFFFFF))
                Spacer(Modifier.weight(1f))
                if (quality.isNotEmpty()) {
                    Text(quality, fontSize = 13.sp, color = Color(0x99FFFFFF), maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Text(fmtShort(max), fontSize = 15.sp, color = Color(0xB3FFFFFF))
            }

            // controls: shuffle | previous | play | next | repeat
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val ok = vm.shuffleUpNext()
                    Toast.makeText(
                        ctx,
                        if (ok) "Up next shuffled" else "Not enough songs to shuffle",
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(Icons.Filled.Shuffle, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                GlassCircle(68.dp, { vm.prev() }) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                GlassCircle(84.dp, { vm.toggle() }) {
                    if (vm.loading) {
                        CircularProgressIndicator(Modifier.size(34.dp), color = Color.White, strokeWidth = 3.dp)
                    } else {
                        Icon(
                            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color.White, modifier = Modifier.size(46.dp)
                        )
                    }
                }
                GlassCircle(68.dp, { vm.next() }) {
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        if (vm.repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        null,
                        tint = if (vm.repeat != 0) Pink else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // bottom: segmented pill (cover | lyrics | effects | info)
            val segShape = RoundedCornerShape(50)
            val segIcons = listOf(Icons.Filled.Album, Icons.Filled.Subject, Icons.Filled.Tune, Icons.Filled.Info)
            Row(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 10.dp)
                    .glassBar(segShape)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                segIcons.forEachIndexed { i, ic ->
                    val on = page == i
                    Box(
                        Modifier
                            .width(70.dp)
                            .height(44.dp)
                            .clip(segShape)
                            .then(if (on) Modifier.glassActiveTab(segShape) else Modifier)
                            .clickable { PlayerUi.page = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            ic, null,
                            tint = if (on) Color.White else Color(0xB3FFFFFF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
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
            .background(Color(0xF21E1D1D))
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
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(50))
                .glassFill()
                .clickable {
                    vm.dismissQueue()
                    onClose()
                }
                .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Dismiss queue", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
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
