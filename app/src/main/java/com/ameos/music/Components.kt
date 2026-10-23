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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.LaunchedEffect
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
val Stroke = Color(0x33FFFFFF)

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
    get() = if (Pal.mode == 1) SolidColor(Color(0x2AFFFFFF)) else Brush.linearGradient(
        listOf(Color(0xAAFFFFFF), Color(0x22FFFFFF), Color(0x11FFFFFF), Color(0x55FFFFFF))
    )
private val GlassSheen: Brush = Brush.linearGradient(
    listOf(Color(0x33FFFFFF), Color(0x08FFFFFF), Color.Transparent, Color(0x14FFFFFF))
)

fun Modifier.glass(shape: Shape, top: Color = Pal.glassTop, bottom: Color = Pal.glassBottom): Modifier =
    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .then(if (Pal.glassy) Modifier.background(GlassSheen) else Modifier)
        .border(1.dp, GlassEdge, shape)

// light glass fill for cards, rows, round buttons (put .clip(shape) before it, add a border if wanted)
fun Modifier.glassFill(): Modifier =
    if (Pal.mode == 1) this.background(Pal.surface)
    else this
        .background(Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x14FFFFFF))))
        .background(GlassSheen)

// glass for sheets and dialogs: dark glass with a sheen and a light edge
fun Modifier.glassPanel(shape: Shape): Modifier =
    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Pal.panelTop, Pal.panelBottom)))
        .then(if (Pal.glassy) Modifier.background(GlassSheen) else Modifier)
        .border(1.dp, GlassEdge, shape)

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
                color = Color.White,
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
                song.title, fontSize = 16.sp, color = Color.White,
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
fun MiniPlayer(vm: MainViewModel, modifier: Modifier = Modifier) {
    val song = vm.current ?: return
    val shape = RoundedCornerShape(32.dp)
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 300f)) }
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                alpha = enter.value.coerceIn(0f, 1f)
                translationY = (1f - enter.value) * 80f
            }
            .pressScale(0.97f)
            .clip(shape)
            .clickable { vm.showPlayer = !vm.showPlayer }
            .background(Brush.horizontalGradient(listOf(Pal.miniA, Pal.miniB)))
            .glassFill()
            .border(1.dp, Brush.verticalGradient(listOf(Pal.accent.copy(alpha = 0.4f), Pal.accent.copy(alpha = 0.13f))), shape)
    ) {
        Cover(
            song.cover,
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(170.dp).alpha(0.40f).blur(10.dp),
            RoundedCornerShape(0.dp)
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(song.cover, Modifier.size(44.dp), RoundedCornerShape(12.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist, fontSize = 12.sp, color = Pal.textDim,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { vm.prev() }) {
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White)
            }
            IconButton(onClick = { vm.toggle() }) {
                if (vm.loading) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Crossfade(vm.isPlaying, animationSpec = tween(180), label = "pp") { pl ->
                        Icon(
                            if (pl) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color.White
                        )
                    }
                }
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Filled.SkipNext, null, tint = Color.White)
            }
        }
    }
}

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        TabItem("Home", Icons.Filled.Home),
        TabItem("Explore", Icons.Filled.Explore),
        TabItem("Your type", Icons.Filled.RadioButtonChecked),
        TabItem("Library", Icons.Filled.LibraryMusic)
    )
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .glass(shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { i, item ->
            val on = i == selected
            val col = if (on) Color.White else Pal.muted
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pressScale(0.9f)
                    .clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(item.icon, null, tint = col, modifier = Modifier.size(22.dp))
                Text(
                    item.label, fontSize = 11.sp, color = col,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                )
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
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
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

// bottom area of detail pages: mini player + Home button
@Composable
fun DetailBottomBar(vm: MainViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (vm.current != null) {
            MiniPlayer(vm, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
        } else {
            Spacer(Modifier.weight(1f))
        }
        HomeFab(vm)
    }
}


// Bottom dock: mini player + tab bar. Scrolling down folds it into one slim row
// (Home circle, mini player pill, Search circle); scrolling up opens it again.
// One spring drives fade, slide, scale and height. Nothing re-composes while it moves:
// the spring value is only read inside graphicsLayer / layout, so the fold stays smooth.
@Composable
fun Dock(vm: MainViewModel, collapsed: Boolean, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val hasSong = vm.current != null
    val folded = collapsed && hasSong
    val pState: State<Float> = animateFloatAsState(
        if (folded) 1f else 0f,
        spring(dampingRatio = 1f, stiffness = 380f),
        label = "dockFold"
    )
    val fullHState: State<androidx.compose.ui.unit.Dp> = animateDpAsState(
        if (hasSong) 138.dp else 64.dp,
        spring(dampingRatio = 1f, stiffness = 380f),
        label = "dockFull"
    )
    // only flips once, when the fold passes the middle (cheap)
    val foldedHalf by remember { derivedStateOf { pState.value > 0.5f } }
    val fullTarget = if (hasSong) 138.dp else 64.dp
    Box(
        modifier
            .fillMaxWidth()
            .layout { m, c ->
                val h = androidx.compose.ui.unit.lerp(fullHState.value, 56.dp, pState.value)
                    .roundToPx().coerceAtLeast(0)
                val pl = m.measure(c.copy(minHeight = h, maxHeight = h))
                layout(pl.width, h) { pl.placeRelative(0, 0) }
            }
    ) {
        // the full dock stays in the tree (so the mini player does not replay its spring-in)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .requiredHeight(fullTarget)
                .graphicsLayer {
                    val p = pState.value
                    alpha = (1f - p * 1.5f).coerceIn(0f, 1f)
                    translationY = p * 40f
                    scaleX = 1f - 0.05f * p
                    scaleY = 1f - 0.05f * p
                }
                .blockTouches(foldedHalf),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasSong) MiniPlayer(vm)
            BottomBar(vm.tab, { vm.tab = it })
        }
        if (foldedHalf) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .requiredHeight(56.dp)
                    .graphicsLayer {
                        val p = pState.value
                        alpha = ((p - 0.3f) * 1.7f).coerceIn(0f, 1f)
                        translationY = (1f - p) * -26f
                        scaleX = 0.95f + 0.05f * p
                        scaleY = 0.95f + 0.05f * p
                    }
            ) {
                FoldedDock(vm, onExpand)
            }
        }
    }
}

@Composable
private fun FoldedDock(vm: MainViewModel, onExpand: () -> Unit) {
    val song = vm.current
    Row(
        Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(56.dp)
                .pressScale(0.9f)
                .glass(CircleShape)
                .clickable { vm.tab = 0; onExpand() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Home, null,
                tint = if (vm.tab == 0) Color.White else Pal.muted,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier
                .weight(1f)
                .height(56.dp)
                .pressScale(0.97f)
                .glass(RoundedCornerShape(28.dp))
                .clickable { vm.showPlayer = true }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (song != null) {
                Cover(song.cover, Modifier.size(40.dp), CircleShape)
                Spacer(Modifier.width(10.dp))
                Text(
                    song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = { vm.toggle() }) {
                if (vm.loading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Crossfade(vm.isPlaying, animationSpec = tween(180), label = "pp") { pl ->
                        Icon(
                            if (pl) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color.White
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(56.dp)
                .pressScale(0.9f)
                .glass(CircleShape)
                .clickable { vm.tab = 1; onExpand() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Search, null,
                tint = if (vm.tab == 1) Color.White else Pal.muted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


// Tablet: navigation rail on the left (replaces the bottom tab bar)
private data class RailItem(val label: String, val icon: ImageVector)

@Composable
fun SideRail(vm: MainViewModel) {
    val items = listOf(
        RailItem("Home", Icons.Filled.Home),
        RailItem("Explore", Icons.Filled.Explore),
        RailItem("Your type", Icons.Filled.RadioButtonChecked),
        RailItem("Library", Icons.Filled.LibraryMusic)
    )
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
            val on = vm.tab == i
            val pill by animateFloatAsState(
                if (on) 1f else 0f, spring(dampingRatio = 0.75f, stiffness = 400f), label = "railPill"
            )
            Column(
                Modifier
                    .padding(vertical = 6.dp)
                    .pressScale(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { vm.tab = i },
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
                        tint = if (on) Color.White else Pal.muted,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    item.label, fontSize = 11.sp,
                    color = if (on) Color.White else Pal.muted,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 3.dp)
                )
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
