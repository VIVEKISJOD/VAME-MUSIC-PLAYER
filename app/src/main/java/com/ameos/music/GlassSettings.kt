package com.ameos.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Settings > Look > Glass: material, colour, frost, tint, film and active tab of the glass bars.
// Opened with GlassUi.open = true (MainActivity shows it in a MorphLayer).
object GlassUi {
    var open by mutableStateOf(false)
}

private val TintHues = listOf(0f, 28f, 52f, 125f, 185f, 218f, 270f, 312f)

@Composable
private fun GlassSection(text: String) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp,
        color = Pal.muted, modifier = Modifier.padding(start = 14.dp, top = 26.dp, bottom = 10.dp)
    )
}

@Composable
private fun GlassPreview() {
    val ink = if (Pal.light) Color(0xFF16161A) else Color.White
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFD9692E), Color(0xFFEFA070), Color(0xFF7FD6CC), Color(0xFF5CC8BF), Color(0xFF123B3C))
                )
            )
    ) {
        // stripes behind the glass, so the see-through effect can be seen
        Column(Modifier.fillMaxSize().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            repeat(5) {
                Box(
                    Modifier
                        .fillMaxWidth(if (it % 2 == 0) 0.9f else 0.6f)
                        .height(5.dp)
                        .background(Color.White.copy(alpha = 0.55f))
                )
            }
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // mini player pill
            val pillShape = RoundedCornerShape(30.dp)
            Row(
                Modifier.fillMaxWidth().height(52.dp).glassBar(pillShape).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(ink.copy(alpha = 0.25f)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.width(120.dp).height(8.dp).clip(CircleShape).background(ink.copy(alpha = 0.55f)))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(70.dp).height(6.dp).clip(CircleShape).background(ink.copy(alpha = 0.3f)))
                }
                Icon(Icons.Filled.PlayArrow, null, tint = ink, modifier = Modifier.size(28.dp))
                Icon(Icons.Filled.SkipNext, null, tint = ink, modifier = Modifier.size(28.dp))
            }
            // dock pill, Home selected
            Row(
                Modifier.fillMaxWidth().height(58.dp).glassBar(pillShape).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabs.forEachIndexed { i, t ->
                    Box(
                        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (i == 0) Box(Modifier.matchParentSize().glassActiveTab(RoundedCornerShape(24.dp)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(t.icon, null, tint = ink, modifier = Modifier.size(20.dp))
                            Text(t.label, fontSize = 9.sp, color = ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassRowHead(icon: ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Pal.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Pal.accent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 20.sp, color = Pal.fg, modifier = Modifier.weight(1f))
        Text(value, fontSize = 17.sp, color = Pal.textDim)
    }
}

@Composable
private fun GlassSlider(
    icon: ImageVector,
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    shown: String,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        GlassRowHead(icon, title, shown)
        PadSlider(value = value, onChange = onChange, onFinished = onFinished, range = range)
    }
}

@Composable
private fun TintStrip(onPick: (Float) -> Unit, onDone: () -> Unit) {
    val hues = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f)
    val colors = hues.map { Color.hsv(it.coerceAtMost(359.9f), 0.85f, 1f) }
    BoxWithConstraints(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.CenterStart) {
        val wPx = with(LocalDensity.current) { maxWidth.toPx() }
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(colors))
                .pointerInput(Unit) {
                    detectTapGestures { o ->
                        onPick((o.x / size.width * 359f).coerceIn(0f, 359f))
                        onDone()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { onDone() },
                        onDrag = { change, _ ->
                            onPick((change.position.x / size.width * 359f).coerceIn(0f, 359f))
                        }
                    )
                }
        )
        if (Pal.glassTint >= 0f) {
            val x = ((Pal.glassTint / 359f) * wPx - with(LocalDensity.current) { 13.dp.toPx() }).roundToInt()
            Box(
                Modifier
                    .offset { IntOffset(x, 0) }
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, Color.hsv(Pal.glassTint.coerceIn(0f, 359.9f), 0.85f, 1f), CircleShape)
            )
        }
    }
}

@Composable
fun GlassSettingsScreen(vm: MainViewModel) {
    BackHandler(enabled = GlassUi.open) { GlassUi.open = false }
    val ctx = LocalContext.current
    val done: () -> Unit = { Pal.save(ctx) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.bgMid)
            .blockTaps()
    ) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(54.dp)
                        .glassBar(CircleShape)
                        .clickable { GlassUi.open = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Pal.fg, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(18.dp))
                Text("Glass", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Pal.fg)
            }

            GlassPreview()

            GlassSection("MATERIAL")
            KitCard {
                val names = listOf("Fastest", "Balanced", "Quality")
                names.forEachIndexed { i, n ->
                    val on = Pal.glassMat == i
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .padRing(RoundedCornerShape(12.dp)) {
                                Pal.glassMat = i
                                Pal.save(ctx)
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            n, fontSize = 22.sp, color = if (on) Pal.fg else Pal.textDim,
                            modifier = Modifier.weight(1f)
                        )
                        if (on) Icon(Icons.Filled.Check, null, tint = Pal.accent, modifier = Modifier.size(26.dp))
                    }
                    if (i < names.size - 1) KitDivider()
                }
                KitDivider()
                GlassSlider(
                    Icons.Filled.Opacity, "Colour", Pal.glassColour, 0f..1f,
                    (Pal.glassColour * 100f).roundToInt().toString() + "%",
                    { Pal.glassColour = it }, done
                )
                KitDivider()
                GlassSlider(
                    Icons.Filled.BlurOn, "Frost", Pal.glassFrost, 0f..24f,
                    Pal.glassFrost.roundToInt().toString() + " dp",
                    { Pal.glassFrost = it }, done
                )
                KitDivider()
                KitSwitch(
                    "Real blur",
                    if (Frost.supported) "The dock and the mini player blur the page behind them like frosted glass. Material Fastest turns it off."
                    else "Needs Android 12 or newer. On this phone the glass stays milky.",
                    Pal.glassReal
                ) {
                    Pal.glassReal = it
                    Pal.save(ctx)
                }
                if (Frost.supported && Pal.glassReal) {
                    GlassSlider(
                        Icons.Filled.BlurCircular, "Blur strength", Pal.glassBlur, 0f..30f,
                        Pal.glassBlur.roundToInt().toString() + " dp",
                        { Pal.glassBlur = it }, done
                    )
                }
            }

            GlassSection("COLOUR")
            KitCard {
                GlassRowHead(
                    Icons.Filled.Palette, "Tint",
                    if (Pal.glassTint < 0f) "None" else Pal.glassTint.roundToInt().toString() + "°"
                )
                Spacer(Modifier.height(10.dp))
                TintStrip(onPick = { Pal.glassTint = it }, onDone = done)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val none = Pal.glassTint < 0f
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8E8EC0))
                            .border(if (none) 3.dp else 0.dp, Pal.fg, CircleShape)
                            .clickable {
                                Pal.glassTint = -1f
                                Pal.save(ctx)
                            }
                    )
                    TintHues.forEach { h ->
                        val on = Pal.glassTint >= 0f && kotlin.math.abs(Pal.glassTint - h) < 6f
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.hsv(h, 0.62f, 1f))
                                .border(if (on) 3.dp else 0.dp, Pal.fg, CircleShape)
                                .clickable {
                                    Pal.glassTint = h
                                    Pal.save(ctx)
                                }
                        )
                    }
                }
                KitDivider()
                GlassSlider(
                    Icons.Filled.Tonality, "Film", Pal.glassFilm, 0f..1f,
                    (Pal.glassFilm * 100f).roundToInt().toString() + "%",
                    { Pal.glassFilm = it }, done
                )
                KitDivider()
                GlassSlider(
                    Icons.Filled.Tab, "Active tab", Pal.glassActive, 0f..1f,
                    (Pal.glassActive * 100f).roundToInt().toString() + "%",
                    { Pal.glassActive = it }, done
                )
            }
            Spacer(Modifier.height(18.dp))
            KitButton("Reset glass") {
                Pal.resetGlass()
                Pal.save(ctx)
            }
            KitNote(
                "Real blur shows on Android 12+ behind the dock and the mini player. Frost and Film add a milky layer on top; on older phones they are the whole glass look."
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}
