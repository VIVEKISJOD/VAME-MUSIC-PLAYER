package com.ameos.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subject
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ===================== TV (Android TV / remote control) =====================
// One APK for phone, tablet and TV. On a TV (or Settings > Interface > TV mode = On) AmeosRoot
// shows TvShell instead of the phone / tablet layout. Same data, same screens (Home, Explore,
// Library, album / artist pages, Settings), plus a remote friendly player and now playing bar.

// ---------- focus ring ----------
// Every clickable thing in the app gets this when the remote lands on it (Theme.kt provides it
// while TV mode is on): a soft white fill and a white ring drawn just inside its edge.
object TvFocusIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        TvFocusNode(interactionSource)

    override fun hashCode(): Int = 7

    override fun equals(other: Any?): Boolean = other === this
}

private class TvFocusNode(private val src: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private var focused = false

    override fun onAttach() {
        coroutineScope.launch {
            src.interactions.collect { i ->
                if (i is FocusInteraction.Focus) {
                    focused = true
                    invalidateDraw()
                } else if (i is FocusInteraction.Unfocus) {
                    focused = false
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (focused) {
            val sw = 4.dp.toPx()
            val h = size.height
            val r = if (h <= 76.dp.toPx()) h / 2f else 20.dp.toPx()
            drawRoundRect(Color(0x26FFFFFF), cornerRadius = CornerRadius(r, r))
            val inner = (r - sw / 2f).coerceAtLeast(0f)
            drawRoundRect(
                Color.White,
                topLeft = Offset(sw / 2f, sw / 2f),
                size = Size(size.width - sw, size.height - sw),
                cornerRadius = CornerRadius(inner, inner),
                style = Stroke(sw)
            )
        }
    }
}

// ---------- small pieces ----------

@Composable
private fun TvIconBtn(
    icon: ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    filled: Boolean = false,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) Pal.accent else Color(0x33FFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, desc,
            tint = if (filled) Pal.bgTop else tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
private fun TvRailBtn(
    name: String,
    icon: ImageVector,
    on: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (on) Pal.accent.copy(alpha = 0.28f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, name, tint = if (on) Color.White else Pal.muted, modifier = Modifier.size(30.dp))
        Text(
            name, fontSize = 13.sp,
            color = if (on) Color.White else Pal.muted,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ---------- the TV screen ----------

@Composable
fun TvShell(vm: MainViewModel) {
    val homeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(400)
        try {
            homeFocus.requestFocus()
        } catch (e: Exception) {
        }
    }
    Row(Modifier.fillMaxSize()) {
        // left rail
        Column(
            Modifier
                .width(132.dp)
                .fillMaxHeight()
                .padding(start = 14.dp, end = 10.dp, top = 28.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("VAME", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Pal.accent)
            Spacer(Modifier.weight(1f))
            TvRailBtn("Home", Icons.Filled.Home, vm.tab == 0, Modifier.focusRequester(homeFocus)) {
                vm.goHome()
            }
            TvRailBtn("Explore", Icons.Filled.Explore, vm.tab == 1) { vm.tab = 1 }
            TvRailBtn("Your type", Icons.Filled.RadioButtonChecked, vm.tab == 2) { vm.tab = 2 }
            TvRailBtn("Library", Icons.Filled.LibraryMusic, vm.tab == 3) { vm.tab = 3 }
            Spacer(Modifier.weight(1f))
            TvRailBtn("Settings", Icons.Filled.Settings, false) { vm.showSettings = true }
        }
        // pages + now playing bar
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(end = 28.dp, bottom = if (vm.current != null) 104.dp else 0.dp)
            ) {
                MainPages(vm, true, false) { }
            }
            if (vm.current != null) {
                TvNowBar(vm, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
    // full screen player and lyrics (lyrics use the tablet lyrics screen: cover left, lyrics right)
    AnimatedVisibility(
        visible = vm.showPlayer && vm.current != null,
        enter = fadeIn(tween(260)),
        exit = fadeOut(tween(200))
    ) {
        TvPlayer(vm)
    }
    AnimatedVisibility(
        visible = vm.showLyrics && vm.current != null,
        enter = fadeIn(tween(260)),
        exit = fadeOut(tween(200))
    ) {
        TabletLyricsScreen(vm)
    }
}

// the song that is playing, always at the bottom while browsing. OK on the bar opens the player.
@Composable
fun TvNowBar(vm: MainViewModel, modifier: Modifier = Modifier) {
    val song = vm.current ?: return
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier
            .fillMaxWidth()
            .padding(end = 28.dp, bottom = 16.dp)
            .clip(shape)
            .glassFill()
            .border(1.dp, Color(0x22FFFFFF), shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .clickable { vm.showPlayer = true }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(song.cover, Modifier.size(64.dp), RoundedCornerShape(16.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist, fontSize = 15.sp, color = Pal.textDim,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        TvIconBtn(Icons.Filled.SkipPrevious, "Previous") { vm.prev() }
        Spacer(Modifier.width(10.dp))
        TvIconBtn(
            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            "Play or pause", filled = true
        ) { vm.toggle() }
        Spacer(Modifier.width(10.dp))
        TvIconBtn(Icons.Filled.SkipNext, "Next") { vm.next() }
        Spacer(Modifier.width(6.dp))
    }
}

private fun tvTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}

// progress bar for the remote: Left / Right on it = back / forward 10 seconds (hold to keep going)
@Composable
private fun TvSeek(vm: MainViewModel) {
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(vm.current?.url) {
        while (true) {
            pos = vm.posMs()
            dur = vm.durMs().coerceAtLeast(0L)
            delay(250)
        }
    }
    val frac = if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .onFocusChanged { focused = it.isFocused }
                .focusable()
                .onKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                        vm.seekTo((vm.posMs() - 10000L).coerceAtLeast(0L))
                        true
                    } else if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) {
                        val d = vm.durMs()
                        val to = vm.posMs() + 10000L
                        vm.seekTo(if (d > 0L) minOf(to, d - 500L).coerceAtLeast(0L) else to)
                        true
                    } else {
                        false
                    }
                }
                .pointerInput(dur) {
                    detectTapGestures { off ->
                        if (dur > 0L && size.width > 0) {
                            vm.seekTo((off.x / size.width.toFloat() * dur).toLong())
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val h = if (focused) 14.dp else 8.dp
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(h)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x33FFFFFF))
            )
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(h)
                    .clip(RoundedCornerShape(50))
                    .background(if (focused) Color.White else Pal.accent)
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Text(tvTime(pos), fontSize = 16.sp, color = Pal.textDim)
            Spacer(Modifier.weight(1f))
            Text(
                if (focused) "Left / Right = 10 seconds" else "-" + tvTime(dur - pos),
                fontSize = 16.sp, color = Pal.textDim
            )
        }
    }
}

@Composable
fun TvPlayer(vm: MainViewModel) {
    val song = vm.current ?: return
    BackHandler { vm.showPlayer = false }
    val tint = rememberCoverTint(song.cover)
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(350)
        try {
            playFocus.requestFocus()
        } catch (e: Exception) {
        }
    }
    // the TV must not go to sleep while the player is open
    val view = LocalView.current
    DisposableEffect(Unit) {
        val old = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = old }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(lerp(tint, Color.Black, if (Pal.amoled) 0.9f else 0.45f), Color.Black)
                )
            )
            .blockTouches(true)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp)) {
            val side = minOf(maxHeight, maxWidth * 0.4f)
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Cover(song.cover, Modifier.size(side), RoundedCornerShape(28.dp), hiRes = true)
                Spacer(Modifier.width(56.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist, fontSize = 24.sp, color = Pal.textDim,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(32.dp))
                    TvSeek(vm)
                    Spacer(Modifier.height(28.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvIconBtn(
                            Icons.Filled.Shuffle, "Shuffle up next"
                        ) { vm.shuffleUpNext() }
                        TvIconBtn(Icons.Filled.SkipPrevious, "Previous", size = 64.dp) { vm.prev() }
                        TvIconBtn(
                            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play or pause",
                            Modifier.focusRequester(playFocus),
                            size = 84.dp, filled = true
                        ) { vm.toggle() }
                        TvIconBtn(Icons.Filled.SkipNext, "Next", size = 64.dp) { vm.next() }
                        TvIconBtn(
                            if (vm.repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            "Repeat",
                            tint = if (vm.repeat != 0) Pal.accent else Color.White
                        ) { vm.cycleRepeat() }
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        val liked = vm.isLiked(song)
                        TvIconBtn(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Like",
                            tint = if (liked) Pal.accent else Color.White
                        ) { vm.toggleLike(song) }
                        TvIconBtn(Icons.Filled.Subject, "Lyrics") { vm.showLyrics = true }
                        TvIconBtn(Icons.Filled.Cast, "Play on another device") { vm.showOutputDialog = true }
                    }
                }
            }
        }
    }
}
