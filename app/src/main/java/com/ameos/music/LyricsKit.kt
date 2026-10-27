package com.ameos.music

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private fun fmtT(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}

// round glass button used on the lyrics screen
@Composable
fun LyricsCircle(size: Dp, onClick: () -> Unit, lit: Boolean = false, content: @Composable () -> Unit) {
    Box2(size, onClick, lit, content)
}

@Composable
private fun Box2(size: Dp, onClick: () -> Unit, lit: Boolean, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (lit) Pal.accent.copy(alpha = 0.35f) else Color(0x26FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

// Seek bar made of three soft waves, a thumb dot and the times underneath.
// Tap or drag to seek. The waves move while the music plays.
@Composable
fun WaveScrubber(vm: MainViewModel, modifier: Modifier = Modifier) {
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var drag by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            pos = vm.posMs().coerceAtLeast(0L)
            dur = vm.durMs().coerceAtLeast(0L)
            delay(150)
        }
    }
    val frac = drag ?: if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
    val inf = rememberInfiniteTransition(label = "wave")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "phase"
    )
    val amp by animateFloatAsState(if (vm.isPlaying) 1f else 0.25f, tween(500), label = "amp")
    val accent = Pal.accent
    val dim = Color(0x40FFFFFF)
    val shown = if (dur > 0L) (frac * dur).toLong() else pos

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(dur) {
                    detectTapGestures { o ->
                        if (dur > 0L) {
                            val f = (o.x / size.width).coerceIn(0f, 1f)
                            vm.seekTo((f * dur).toLong())
                        }
                    }
                }
                .pointerInput(dur) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> drag = (o.x / size.width).coerceIn(0f, 1f) },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            drag = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val f = drag
                            drag = null
                            if (f != null && dur > 0L) vm.seekTo((f * dur).toLong())
                        },
                        onDragCancel = { drag = null }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val mid = h / 2f
            val px = (frac * w).coerceIn(0f, w)
            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            val ph = phase
            val k = amp
            val specs = listOf(
                Triple(2.2f, ph, h * 0.30f),
                Triple(3.4f, -ph * 1.3f, h * 0.22f),
                Triple(1.5f, ph * 0.7f + 1f, h * 0.16f)
            )
            for ((idx, t) in specs.withIndex()) {
                val path = Path()
                var x = 0f
                while (x <= w) {
                    val env = sin(PI.toFloat() * x / w)
                    val y = mid + sin(x / w * t.first * 6.2831855f + t.second) * t.third * env * k
                    if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                    x += 4f
                }
                clipRect(0f, 0f, px, h) {
                    drawPath(path, accent.copy(alpha = 0.95f - idx * 0.25f), style = stroke)
                }
                clipRect(px, 0f, w, h) {
                    drawPath(path, dim, style = stroke)
                }
            }
            drawLine(dim, Offset(px, mid), Offset(w, mid), strokeWidth = 2.dp.toPx())
            drawLine(accent, Offset(0f, mid), Offset(px, mid), strokeWidth = 2.dp.toPx())
            val r = 7.dp.toPx()
            drawCircle(accent, r, Offset(px.coerceIn(r, w - r), mid))
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtT(shown), fontSize = 12.sp, color = Color(0xFFD0D0D0))
            Text("-" + fmtT(dur - shown), fontSize = 12.sp, color = Color(0xFFD0D0D0))
        }
    }
}

@Composable
private fun ToolBtn(icon: ImageVector, lit: Boolean, onClick: () -> Unit) {
    LyricsCircle(48.dp, onClick, lit) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

private val PanelBg = Color(0xE6141414)

// Row of round tools: provider, translate, Hinglish, share, text size, fullscreen.
// Size and translate open a small panel above the row.
@Composable
fun LyricsTools(
    vm: MainViewModel,
    full: Boolean,
    onFull: () -> Unit,
    modifier: Modifier = Modifier,
    showFull: Boolean = true
) {
    var panel by remember { mutableIntStateOf(0) }   // 0 none, 1 size
    val ctx = LocalContext.current
    Column(modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = panel == 1) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PanelBg)
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Lyrics size: " + vm.lyricsPct + "%", fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = Color(0xFFD0D0D0),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Reset", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD0D0D0),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { vm.updateLyricsPct(100, true) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", fontSize = 13.sp, color = Color(0xFFD0D0D0))
                    Slider(
                        value = vm.lyricsPct.toFloat(),
                        onValueChange = { vm.updateLyricsPct(it.roundToInt(), false) },
                        onValueChangeFinished = { vm.updateLyricsPct(vm.lyricsPct, true) },
                        valueRange = 50f..160f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White.copy(alpha = 0.85f),
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                    Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        if (vm.lyricsTransOn && (vm.lyricsTransBusy || vm.lyricsTransError.isNotBlank())) {
            Text(
                if (vm.lyricsTransBusy) "Translating..." else vm.lyricsTransError,
                fontSize = 13.sp,
                color = if (vm.lyricsTransBusy) Color(0xFFD0D0D0) else Color(0xFFFFB4A8),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolBtn(Icons.Filled.LibraryMusic, false) { vm.showProviderSheet = true }
            ToolBtn(Icons.Filled.Translate, vm.lyricsTransOn) { vm.toggleLyricsTrans(!vm.lyricsTransOn) }
            ToolBtn(Icons.Filled.Public, vm.hinglish) { vm.setHinglishOn(!vm.hinglish) }
            ToolBtn(Icons.Filled.Share, false) {
                if (vm.lyrics.isNotEmpty()) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, vm.lyricsShareText())
                    }
                    val chooser = Intent.createChooser(send, "Share lyrics")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        ctx.startActivity(chooser)
                    } catch (e: Exception) {
                    }
                }
            }
            ToolBtn(Icons.Filled.TextFields, panel == 1) { panel = if (panel == 1) 0 else 1 }
            if (showFull) {
                ToolBtn(if (full) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, full) { onFull() }
            }
        }
    }
}

// "Lyrics Provider" window: what gives the lyrics now, Try another source, pick a provider
@Composable
fun LyricsProviderDialog(vm: MainViewModel) {
    AnimDialog(onClosed = { vm.showProviderSheet = false }) { close ->
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(32.dp))
                .glassPanel(RoundedCornerShape(32.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text("Lyrics provider", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)

            val cur = if (vm.lyricsSource.isBlank()) "none" else vm.lyricsSource
            val kind = if (vm.lyrics.isEmpty()) "" else if (vm.lyricsSynced) " (synced)" else " (plain text)"
            Text(
                "Current: " + cur + kind, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(16.dp)
            )

            Row(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Pal.accent.copy(alpha = 0.22f))
                    .clickable(enabled = !vm.lyricsTrying) { vm.tryAnotherLyricsSource() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Refresh, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Try another source", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Skips the current provider and searches the others.",
                        fontSize = 13.sp, color = Pal.textDim
                    )
                }
                if (vm.lyricsTrying) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                }
            }
            if (vm.lyricsTryFail && !vm.lyricsTrying) {
                Text(
                    "No other source has lyrics for this song.", fontSize = 13.sp,
                    color = Color(0xFFFFB4A8), modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            Text(
                "Or choose a provider directly", fontSize = 14.sp, color = Pal.textDim,
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
            )
            LyricsRepo.providerNames.forEachIndexed { i, n ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            vm.chooseLyricsProvider(i)
                            close()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = vm.lyricsProvider == i,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Pal.accent,
                            unselectedColor = Color(0x99FFFFFF)
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(n, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            LyricsRepo.providerNotes[i], fontSize = 13.sp, color = Pal.textDim,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Text(
                "Done", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Pal.accent,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { close() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
