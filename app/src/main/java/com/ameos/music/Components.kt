package com.ameos.music

import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.layout.layout
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.ui.graphics.TransformOrigin
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import coil.request.ImageRequest

val CardDark = Color(0xFF141010)
val Stroke: Color get() = if (Pal.light) Color(0x1F000000) else Color(0x33FFFFFF)

data class GenreTile(val name: String, val covers: List<String>, val artists: String)

// Small spring "press" effect: the item shrinks a little while a finger is on it.
// It only watches touches, so clicks still work normally.
fun Modifier.pressScale(target: Float = 0.96f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val s by animateFloatAsState(
        if (pressed) target else 1f,
        spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "press"
    )
    this
        .graphicsLayer {
            scaleX = s
            scaleY = s
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                waitForUpOrCancellation(PointerEventPass.Initial)
                pressed = false
            }
        }
}

// Glass surface: see-through layer + light edge on top.
// light edge of the glass: bright on the top-left, soft at the bottom-right
val GlassEdge: Brush
    get() = if (Pal.light) Brush.linearGradient(
        listOf(Color(0xF5FFFFFF), Color(0x40FFFFFF), Color(0x1A000000), Color(0xAAFFFFFF))
    ) else if (Pal.mode == 1) SolidColor(Color(0x2AFFFFFF)) else Brush.linearGradient(
        listOf(Color(0xAAFFFFFF), Color(0x22FFFFFF), Color(0x11FFFFFF), Color(0x55FFFFFF))
    )
private val GlassSheen: Brush = Brush.linearGradient(
    listOf(Color(0x33FFFFFF), Color(0x08FFFFFF), Color.Transparent, Color(0x14FFFFFF))
)

// text / icon colour that is right for where it is drawn (white inside the always-dark screens)
@Composable
fun inkColor(): Color = if (LocalDark.current) Color.White else Pal.fg

fun Modifier.glass(shape: Shape, top: Color = Pal.glassTop, bottom: Color = Pal.glassBottom): Modifier =
    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .then(if (Pal.glassy) Modifier.background(GlassSheen) else Modifier)
        .border(1.dp, GlassEdge, shape)

// light glass fill for cards, rows, round buttons (put .clip(shape) before it, add a border if wanted)
fun Modifier.glassFill(): Modifier = composed {
    val darkHere = LocalDark.current || !Pal.light
    if (!Pal.light && Pal.mode == 1) {
        this.background(Pal.surface)
    } else {
        // Settings > Glass strength: soft / normal / strong
        val k = when (UiOpts.glass) {
            0 -> 0.55f
            2 -> 1.6f
            else -> 1f
        }
        if (darkHere) {
            val a1 = (0x2E / 255f * k).coerceIn(0f, 1f)
            val a2 = (0x14 / 255f * k).coerceIn(0f, 1f)
            this
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = a1), Color.White.copy(alpha = a2))))
                .background(GlassSheen)
        } else {
            this.background(Color(0xFF16161A).copy(alpha = (0.055f * k).coerceIn(0f, 0.3f)))
        }
    }
}

// glass for sheets and dialogs: glass with a sheen and a light edge
fun Modifier.glassPanel(shape: Shape): Modifier =
    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Pal.panelTop, Pal.panelBottom)))
        .then(if (Pal.glassy) Modifier.background(GlassSheen) else Modifier)
        .border(1.dp, GlassEdge, shape)

// The floating glass bars (dock, mini player, player pills). Follows Settings > Glass:
// material (fastest = flat, balanced = + sheen, quality = + shine), colour, frost, tint, film.
fun Modifier.glassBar(shape: Shape, base: Color? = null, blur: Boolean = false): Modifier = composed {
    val dark = LocalDark.current || !Pal.light
    val film = Pal.glassFilm
    val frost0 = blur
    val frost = (Pal.glassFrost / 24f).coerceIn(0f, 1f)
    val real = frost0 && Frost.active && Frost.layer != null
    val milk0 = if (dark) 0.05f + 0.20f * film + 0.10f * frost else 0.16f + 0.50f * film + 0.20f * frost
    val milk = if (real) milk0 * 0.55f else milk0
    // dock + mini player: a solid frosted backing so text behind them never shows through
    val back = if (!frost0) 0f else if (real) 0.58f else 0.84f
    val tint = Pal.tintColor ?: base
    val tintA = if (tint == null) 0f else (0.06f + 0.42f * Pal.glassColour) * (if (dark) 0.8f else 1f)
    val edge = if (dark) Brush.linearGradient(
        listOf(Color(0xAAFFFFFF), Color(0x22FFFFFF), Color(0x11FFFFFF), Color(0x66FFFFFF))
    ) else GlassEdge
    var m = this.clip(shape).then(if (real) Modifier.frostBehind() else Modifier)
        .then(if (back > 0f) Modifier.background((if (dark) Color(0xFF17171B) else Color(0xFFF4F4F6)).copy(alpha = back)) else Modifier)
        .background(Color.White.copy(alpha = milk.coerceIn(0f, 0.92f)))
    if (tint != null) m = m.background(tint.copy(alpha = tintA.coerceIn(0f, 0.6f)))
    if (Pal.glassMat >= 1) m = m.background(GlassSheen)
    if (Pal.glassMat >= 2) {
        m = m.background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (dark) 0.18f else 0.45f),
                0.5f to Color.Transparent
            )
        )
    }
    m.border(1.dp, edge, shape)
}

// highlight behind the selected tab of a dock (strength = Settings > Glass > Active tab)
fun Modifier.glassActiveTab(shape: Shape): Modifier = composed {
    val dark = LocalDark.current || !Pal.light
    val a = (0.10f + 0.9f * Pal.glassActive).coerceIn(0f, 1f)
    this.clip(shape)
        .background(
            if (dark) Color.White.copy(alpha = 0.10f + 0.5f * Pal.glassActive)
            else Color.White.copy(alpha = a)
        )
        .border(1.dp, if (dark) Color(0x33FFFFFF) else Color(0x14000000), shape)
}

// fade + rise when an item first shows up
fun Modifier.appear(): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(340, easing = FastOutSlowInEasing)) }
    this.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 36f
    }
}

private val ytThumbRe = Regex("^(https?://i\\.ytimg\\.com/vi(?:_webp)?/[^/]+/)[A-Za-z0-9_]+\\.(?:jpg|webp)(?:\\?.*)?$")
private val lhSizeRe = Regex("=(?:w\\d+-h\\d+|s\\d+)[^/]*$")

// Sharper version of a cover link (used for big covers): Google art up to 1080px,
// YouTube video thumbnails in max resolution.
fun hiResCover(url: String?): String? {
    if (url == null) return null
    if (url.contains("googleusercontent.com") || url.contains("ggpht.com")) {
        return if (lhSizeRe.containsMatchIn(url)) url.replace(lhSizeRe, "=w1080-h1080-l90-rj") else url
    }
    val m = ytThumbRe.find(url)
    if (m != null) return m.groupValues[1] + "maxresdefault.jpg"
    return url
}

@Composable
fun Cover(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    hiRes: Boolean = false
) {
    // if the sharp link does not exist, fall back to the normal one
    var failed by remember(url) { mutableStateOf(false) }
    val data = if (hiRes && !failed) hiResCover(url) else url
    Box(modifier.clip(shape).background(Pal.surface)) {
        if (data != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(data)
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, badge: String? = null) {
    Row(
        Modifier.padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (badge != null) {
            Text(
                badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.fg,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x44FFFFFF))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(song: Song, vm: MainViewModel, list: List<Song>) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .appear()
            .padding(vertical = 4.dp)
            .then(if (Net.on && !vm.isDownloaded(song)) Modifier.graphicsLayer { alpha = 0.38f } else Modifier)
            .pressScale()
            .clip(RoundedCornerShape(16.dp))
            .glassFill()
            .border(1.dp, Stroke, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { vm.play(song, list) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.songMenu = song
                }
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(song.cover, Modifier.size(52.dp), RoundedCornerShape(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, fontSize = 16.sp, color = Pal.fg,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        val ctx = LocalContext.current
        IconButton(onClick = {
            val idle = vm.current == null
            vm.playNext(song)
            if (!idle) Toast.makeText(ctx, "Will play next", Toast.LENGTH_SHORT).show()
        }) {
            Icon(Icons.Filled.QueuePlayNext, null, tint = Pal.textDim)
        }
        IconButton(onClick = { vm.toggleLike(song) }) {
            if (vm.isLiked(song)) {
                Icon(Icons.Filled.Favorite, null, tint = Pal.accent)
            } else {
                Icon(Icons.Outlined.FavoriteBorder, null, tint = Pal.textDim)
            }
        }
    }
}

@Composable
fun MiniPlayer(
    vm: MainViewModel,
    modifier: Modifier = Modifier,
    force: Boolean = false,
    onOpen: (() -> Unit)? = null,
    track: Boolean = true
) {
    val song = vm.current ?: return
    // tablet held sideways: the side player and the small floating box replace the pill on every page
    // (force = true is used by the tablet pill and by the pill on top of Settings)
    if (!force && isTabletLandscape()) return
    val shape = RoundedCornerShape(32.dp)
    val tick = rememberTick()
    val ink = inkColor()
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 300f)) }
    // last known spots; track = false while the dock is folded (the slim row owns PillPos then)
    val last = remember { arrayOfNulls<androidx.compose.ui.geometry.Rect>(2) }
    LaunchedEffect(track) {
        if (track) {
            last[0]?.let { PillPos.pill = it }
            last[1]?.let { PillPos.rect = it }
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .onGloballyPositioned {
                last[0] = it.boundsInRoot()
                if (track) PillPos.pill = last[0]
            }
            .graphicsLayer {
                alpha = enter.value.coerceIn(0f, 1f)
                translationY = (1f - enter.value) * 80f
            }
            .pressScale(0.97f)
            .glassBar(shape, blur = true)
            .clickable {
                tick()
                val go = onOpen
                if (go != null) {
                    go()
                } else {
                    vm.showPlayer = !vm.showPlayer
                }
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(
            song.cover,
            Modifier.size(46.dp).onGloballyPositioned {
                last[1] = it.boundsInRoot()
                if (track) PillPos.rect = last[1]
            },
            RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist, fontSize = 13.sp, color = ink.copy(alpha = 0.6f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = {
            tick()
            vm.toggle()
        }) {
            if (vm.loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = ink, strokeWidth = 2.dp)
            } else {
                Crossfade(vm.isPlaying, animationSpec = tween(180), label = "pp") { pl ->
                    Icon(
                        if (pl) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = ink, modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
        IconButton(onClick = { vm.next() }) {
            Icon(Icons.Filled.SkipNext, null, tint = ink, modifier = Modifier.size(30.dp))
        }
    }
}

// The five dock tabs in the order they are shown. The numbers are the page ids used by vm.tab:
// 0 Home, 1 Search (was Explore), 2 Radio (was Your type), 3 Library, 4 New.
data class NavTab(val id: Int, val label: String, val icon: ImageVector)

val NavTabs: List<NavTab> = listOf(
    NavTab(0, "Home", Icons.Filled.Home),
    NavTab(4, "New", Icons.Filled.GridView),
    NavTab(2, "Radio", Icons.Filled.Sensors),
    NavTab(3, "Library", Icons.Filled.LibraryMusic),
    NavTab(1, "Search", Icons.Filled.Search)
)

// go to a tab and close every detail page that covers it
fun goTab(vm: MainViewModel, id: Int) {
    vm.artistOpen = false
    vm.albumOpen = false
    vm.likedOpen = false
    vm.downloadsOpen = false
    vm.playlistOpen = false
    vm.tab = id
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    val tick = rememberTick()
    val ink = inkColor()
    Row(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .glassBar(shape, blur = true)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavTabs.forEach { item ->
            val on = item.id == selected
            val col = if (on) ink else ink.copy(alpha = 0.78f)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable {
                        tick()
                        onSelect(item.id)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (on) Box(Modifier.matchParentSize().glassActiveTab(RoundedCornerShape(28.dp)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(item.icon, null, tint = col, modifier = Modifier.size(if (UiOpts.tabLabels) 26.dp else 28.dp))
                    if (UiOpts.tabLabels) {
                        Text(item.label, fontSize = 11.sp, color = col, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .glassFill()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Pal.fg, modifier = Modifier.size(20.dp))
    }
}

// round Home button (next to the mini player on detail pages, like the design)
@Composable
fun HomeFab(vm: MainViewModel) {
    Box(
        Modifier
            .size(64.dp)
            .pressScale(0.92f)
            .glass(CircleShape)
            .clickable { vm.goHome() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Home, null, tint = Pal.textDim, modifier = Modifier.size(26.dp))
    }
}

// bottom area of detail pages: the dock now floats above every page, so nothing is drawn here
@Composable
fun DetailBottomBar(vm: MainViewModel, modifier: Modifier = Modifier) {
}

// Bottom dock: a floating glass mini player pill on top of a floating glass tab pill.
// Scroll down = the mini player slides down INTO the bar and both fold into one slim row
// (Home, mini player, Search). Scroll up or tap Home / Search = it opens again.
// To tune the feel: the spring below (dampingRatio / stiffness), 30.dp slide, 0.1f shrink.
@Composable
fun Dock(vm: MainViewModel, collapsed: Boolean, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    // over the always-dark artist page the pills use white ink
    val dark = vm.artistOpen
    val fold = collapsed && vm.current != null
    // read only while drawing / measuring, so nothing is re-composed on every frame
    val pState = animateFloatAsState(
        if (fold) 1f else 0f,
        if (UiOpts.reduceMotion) snap() else spring(dampingRatio = 0.82f, stiffness = 320f),
        label = "dockFold"
    )
    val foldOn by remember { derivedStateOf { pState.value > 0.02f } }
    CompositionLocalProvider(LocalDark provides (dark || LocalDark.current)) {
        Box(
            modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val pl = measurable.measure(constraints.copy(minHeight = 0))
                    val foldH = minOf(pl.height, 68.dp.roundToPx())
                    val h = (pl.height + (foldH - pl.height) * pState.value).roundToInt()
                    layout(pl.width, h) { pl.place(0, h - pl.height) }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // open: mini player pill above the tab bar
            Box(
                Modifier.graphicsLayer {
                    val q = pState.value
                    alpha = (1f - q * 1.6f).coerceIn(0f, 1f)
                    translationY = q * 30.dp.toPx()
                    scaleX = 1f - 0.1f * q
                    scaleY = 1f - 0.1f * q
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (vm.current != null) MiniPlayer(vm, track = !fold)
                    BottomBar(vm.tab, { goTab(vm, it); onExpand() })
                }
                // the hidden half cannot be tapped by mistake
                if (fold) Box(Modifier.matchParentSize().blockTaps())
            }
            // folded: one slim row
            if (foldOn || fold) {
                Box(
                    Modifier.graphicsLayer {
                        val q = pState.value
                        alpha = ((q - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        translationY = (1f - q) * -24.dp.toPx()
                        scaleX = 0.92f + 0.08f * q
                        scaleY = 0.92f + 0.08f * q
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                ) {
                    FoldedDock(vm, fold, onExpand)
                    if (!fold) Box(Modifier.matchParentSize().blockTaps())
                }
            }
        }
    }
}

@Composable
private fun FoldedDock(vm: MainViewModel, active: Boolean, onExpand: () -> Unit) {
    val song = vm.current ?: return
    val tick = rememberTick()
    val ink = inkColor()
    Row(
        Modifier
            .fillMaxWidth()
            .height(68.dp)
            .glassBar(RoundedCornerShape(34.dp), blur = true)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FoldTab(Icons.Filled.Home, vm.tab == 0, ink) {
            tick()
            goTab(vm, 0)
            onExpand()
        }
        Row(
            Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
                .height(54.dp)
                .onGloballyPositioned { if (active) PillPos.pill = it.boundsInRoot() }
                .pressScale(0.97f)
                .clip(RoundedCornerShape(27.dp))
                .clickable {
                    tick()
                    vm.showPlayer = !vm.showPlayer
                }
                .padding(start = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(
                song.cover,
                Modifier.size(40.dp).onGloballyPositioned { if (active) PillPos.rect = it.boundsInRoot() },
                RoundedCornerShape(10.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist, fontSize = 11.sp, color = ink.copy(alpha = 0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = {
                tick()
                vm.toggle()
            }, modifier = Modifier.size(44.dp)) {
                if (vm.loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = ink, strokeWidth = 2.dp)
                } else {
                    Crossfade(vm.isPlaying, animationSpec = tween(180), label = "ppFold") { pl ->
                        Icon(
                            if (pl) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = ink, modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
        FoldTab(Icons.Filled.Search, vm.tab == 1, ink) {
            tick()
            goTab(vm, 1)
            onExpand()
        }
    }
}

@Composable
private fun FoldTab(icon: ImageVector, on: Boolean, ink: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (on) Box(Modifier.matchParentSize().glassActiveTab(CircleShape))
        Icon(
            icon, null,
            tint = if (on) ink else ink.copy(alpha = 0.78f),
            modifier = Modifier.size(26.dp)
        )
    }
}

// Tablet: navigation rail on the left (replaces the bottom tab bar)
private data class RailItem(val label: String, val icon: ImageVector)

@Composable
fun SideRail(vm: MainViewModel) {
    val items = NavTabs.map { RailItem(it.label, it.icon) }
    Column(
        Modifier
            .width(92.dp)
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val railCtx = LocalContext.current
        LaunchedEffect(Unit) { AppIcons.live = AppIcons.current(railCtx) }
        val railIcon = AppIcons.all.firstOrNull { it.id == AppIcons.live } ?: AppIcons.all[0]
        Box(
            Modifier
                .size(54.dp)
                .pressScale(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(railIcon.a, railIcon.b)))
                .clickable { vm.goHome() }
        ) {
            Image(painterResource(R.drawable.ic_launcher_fg), null, Modifier.fillMaxSize())
        }
        Spacer(Modifier.weight(1f))
        items.forEachIndexed { i, item ->
            val on = vm.tab == NavTabs[i].id
            val pill by animateFloatAsState(
                if (on) 1f else 0f, spring(dampingRatio = 0.75f, stiffness = 400f), label = "railPill"
            )
            Column(
                Modifier
                    .padding(vertical = 6.dp)
                    .pressScale(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { goTab(vm, NavTabs[i].id) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.width(64.dp).height(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .alpha(pill)
                            .glass(RoundedCornerShape(17.dp), Pal.accent.copy(alpha = 0.53f), Pal.accentDeep.copy(alpha = 0.5f))
                    )
                    Icon(
                        item.icon, null,
                        tint = if (on) Pal.fg else Pal.muted,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    item.label, fontSize = 11.sp,
                    color = if (on) Pal.fg else Pal.muted,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        if (vm.current != null && UiOpts.tabStandbyBtn) {
            // standby mode on the tablet (full screen player with clock, like the phone sideways)
            val tickS = rememberTick()
            Column(
                Modifier
                    .padding(top = 6.dp)
                    .pressScale(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        tickS()
                        vm.tabStandby = true
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.width(64.dp).height(34.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Bedtime, null,
                        tint = Pal.muted, modifier = Modifier.size(24.dp)
                    )
                }
                Text("Standby", fontSize = 11.sp, color = Pal.muted, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}


// Dialog that fades + scales in and also animates OUT (Back, tapping outside, or a button).
// content gets a close() that plays the exit animation first; onClosed runs after it.
@Composable
fun AnimDialog(
    onClosed: () -> Unit,
    canClose: () -> Boolean = { true },
    content: @Composable (close: () -> Unit) -> Unit
) {
    var show by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { show = true }
    val close: () -> Unit = {
        if (!closing && canClose()) {
            closing = true
            show = false
            scope.launch {
                delay(230)
                onClosed()
            }
        }
    }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn(tween(220)) + scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.9f),
            exit = fadeOut(tween(190)) + scaleOut(tween(220, easing = FastOutSlowInEasing), targetScale = 0.92f)
        ) {
            content(close)
        }
    }
}

// swallows every touch on this layer (used for the hidden half of the dock)
fun Modifier.blockTouches(on: Boolean): Modifier =
    if (!on) this else this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                e.changes.forEach { it.consume() }
            }
        }
    }
