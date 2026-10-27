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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.Dp
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

// A full screen page that must catch touches (so nothing underneath is tapped) but must NOT be a
// remote focus stop. (The old way, an empty clickable, made the remote land on the whole page and
// nothing moved.)
fun Modifier.blockTaps(): Modifier = this.pointerInput(Unit) { detectTapGestures { } }

// the Home list remembers where it was while an album / the player / Settings is open on top
object TvKeep {
    val home = androidx.compose.foundation.lazy.LazyListState()
}

// ---------- small pieces ----------

// A focusable / clickable surface for the remote: grows a little and gets a white ring + soft fill
// while the remote is on it. Put it AFTER size / width and BEFORE padding.
fun Modifier.tvClick(
    shape: Shape,
    grow: Float = 1.06f,
    bg: Color = Color.Transparent,
    onClick: () -> Unit
): Modifier = composed {
    var f by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (f) grow else 1f, tween(120), label = "tvGrow")
    this
        .onFocusChanged { f = it.hasFocus }
        .graphicsLayer {
            scaleX = sc
            scaleY = sc
        }
        .clip(shape)
        .background(bg)
        .then(
            if (f) Modifier.background(Color(0x2EFFFFFF)).border(3.dp, Color.White, shape)
            else Modifier
        )
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
}

@Composable
private fun TvIconBtn(
    icon: ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    filled: Boolean = false,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier
            .size(size)
            .tvClick(CircleShape, 1.12f, if (filled) Pal.accent else Color(0x33FFFFFF)) { onClick() },
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
private fun TvTab(
    name: String,
    icon: ImageVector,
    on: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .tvClick(
                RoundedCornerShape(24.dp), 1.05f,
                if (on) Color(0x33FFFFFF) else Color.Transparent
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (on) Color.White else Pal.textDim, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            name, fontSize = 15.sp,
            color = if (on) Color.White else Pal.textDim,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

// top bar like the Spotify TV app: logo, tabs in the middle, "Welcome name" (opens Settings) on the right
@Composable
private fun TvTopBar(vm: MainViewModel, homeFocus: FocusRequester) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            Text(
                "VAME", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Pal.accent,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TvTab("Home", Icons.Filled.Home, vm.tab == 0, Modifier.focusRequester(homeFocus)) { vm.goHome() }
            TvTab("Explore", Icons.Filled.Explore, vm.tab == 1) {
                vm.goHome()
                vm.tab = 1
            }
            TvTab("Your type", Icons.Filled.RadioButtonChecked, vm.tab == 2) {
                vm.goHome()
                vm.tab = 2
            }
            TvTab("Library", Icons.Filled.LibraryMusic, vm.tab == 3) {
                vm.goHome()
                vm.tab = 3
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            val nm = vm.userName.ifBlank { vm.ytName }
            Row(
                Modifier
                    .tvClick(RoundedCornerShape(14.dp), 1.04f, Color(0x55000000)) { vm.showSettings = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileButton(vm, 30.dp, false)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (nm.isBlank()) "Settings" else "Welcome " + nm,
                    fontSize = 14.sp, color = Color.White, maxLines = 1
                )
            }
        }
    }
}

// ---------- the TV screen ----------

@Composable
fun TvShell(vm: MainViewModel) {
    val homeFocus = remember { FocusRequester() }
    var anyFocus by remember { mutableStateOf(false) }
    // while the player or the lyrics cover the screen, the pages underneath are removed (the remote
    // must only be able to reach what is on top)
    val full = vm.showPlayer || vm.showLyrics
    // keep trying to put the remote on Home until it works (it cannot while the screen is still
    // in touch mode, the first remote key switches that off)
    LaunchedEffect(Unit) {
        var n = 0
        while (n < 12) {
            delay(500)
            if (!anyFocus) {
                try {
                    homeFocus.requestFocus()
                } catch (e: Exception) {
                }
            }
            n++
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .onFocusChanged { anyFocus = it.hasFocus }
            .onPreviewKeyEvent { e ->
                val k = e.key
                val pad = k == Key.DirectionUp || k == Key.DirectionDown ||
                    k == Key.DirectionLeft || k == Key.DirectionRight || k == Key.DirectionCenter
                if (pad && e.type == KeyEventType.KeyDown && !anyFocus && !full) {
                    try {
                        homeFocus.requestFocus()
                    } catch (ex: Exception) {
                    }
                    true
                } else {
                    false
                }
            }
    ) {
        if (!full) {
            Column(Modifier.fillMaxSize().padding(horizontal = 26.dp)) {
                TvTopBar(vm, homeFocus)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    MainPages(vm, true, false) { }
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
}

// The pill at the bottom, always there on a TV (also over Settings). OK on the song opens the player.
@Composable
fun TvPill(vm: MainViewModel, modifier: Modifier = Modifier) {
    val song = vm.current
    val shape = RoundedCornerShape(40.dp)
    Row(
        modifier
            .padding(bottom = 16.dp)
            .width(560.dp)
            .clip(shape)
            .glassFill()
            .border(1.dp, Color(0x33FFFFFF), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (song == null) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color(0x22FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, null, tint = Pal.accent, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                "Pick a song to start playing", fontSize = 16.sp, color = Pal.textDim,
                modifier = Modifier.weight(1f)
            )
        } else {
            Row(
                Modifier
                    .weight(1f)
                    .tvClick(RoundedCornerShape(30.dp), 1.0f) { vm.showPlayer = true }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cover(song.cover, Modifier.size(48.dp), CircleShape)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist, fontSize = 12.sp, color = Pal.textDim,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            TvIconBtn(Icons.Filled.SkipPrevious, "Previous", size = 44.dp) { vm.prev() }
            Spacer(Modifier.width(6.dp))
            TvIconBtn(
                if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                "Play or pause", size = 48.dp, filled = true
            ) { vm.toggle() }
            Spacer(Modifier.width(6.dp))
            TvIconBtn(Icons.Filled.SkipNext, "Next", size = 44.dp) { vm.next() }
        }
    }
}

// ---------- TV Home, like the Spotify TV app ----------
// big picture of the card the remote is on (with its name), a grid of wide tiles, then rows of cards

@Composable
private fun TvTile(item: ShelfItem, modifier: Modifier, onFocus: (ShelfItem) -> Unit) {
    Row(
        modifier
            .height(58.dp)
            .onFocusChanged { if (it.hasFocus) onFocus(item) }
            .tvClick(RoundedCornerShape(8.dp), 1.03f, Color(0x2EFFFFFF)) { item.onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(item.cover, Modifier.size(58.dp), RoundedCornerShape(0.dp))
        Text(
            item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (item.active) Pal.accent else Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun TvCard(item: ShelfItem, w: Dp, onFocus: (ShelfItem) -> Unit) {
    Column(
        Modifier
            .width(w)
            .onFocusChanged { if (it.hasFocus) onFocus(item) }
            .tvClick(RoundedCornerShape(14.dp), 1.07f) { item.onClick() }
            .padding(7.dp),
        horizontalAlignment = if (item.round) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Cover(
            item.cover, Modifier.size(w - 14.dp),
            if (item.round) CircleShape else RoundedCornerShape(10.dp), hiRes = true
        )
        Text(
            item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (item.active) Pal.accent else Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )
        if (item.sub.isNotBlank()) {
            Text(
                item.sub, fontSize = 11.sp, color = Pal.textDim,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvShelf(title: String, sub: String, cards: List<ShelfItem>, onFocus: (ShelfItem) -> Unit) {
    if (cards.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        Text(
            title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp, top = 12.dp)
        )
        if (sub.isNotBlank()) {
            Text(
                sub.uppercase(), fontSize = 10.sp, color = Pal.textDim,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(cards) { c -> TvCard(c, 150.dp, onFocus) }
        }
    }
}

@Composable
fun TvHome(vm: MainViewModel) {
    val yt = if (UiOpts.homeYt) vm.ytHome else emptyList()
    val listState = TvKeep.home
    var hero by remember { mutableStateOf<ShelfItem?>(null) }
    val quick = (vm.played.toList() + vm.liked.toList() + vm.recommended).distinctBy { it.url }.take(8)
    val fallback = if (quick.isNotEmpty()) songItem(vm, quick[0], quick) else null
    val shown = hero ?: fallback
    val setHero: (ShelfItem) -> Unit = { hero = it }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 120
        val a by animateFloatAsState(if (atTop) 1f else 0f, tween(250), label = "heroA")
        // the big picture on the right, faded into the page on the left and at the bottom
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.3f + 0.7f * a }) {
            if (shown != null) {
                Crossfade(shown.cover, label = "heroImg") { c ->
                    Box(Modifier.fillMaxSize()) {
                        Cover(
                            c,
                            Modifier.align(Alignment.TopEnd).fillMaxHeight(0.8f).aspectRatio(1f),
                            RoundedCornerShape(0.dp), hiRes = true
                        )
                    }
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Pal.bgMid, 0.4f to Pal.bgMid.copy(alpha = 0.92f), 0.8f to Color.Transparent
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.4f to Color.Transparent, 0.75f to Pal.bgMid)
                )
            )
        }
        if (shown != null) {
            Column(
                Modifier
                    .padding(start = 8.dp, top = h * 0.12f)
                    .fillMaxWidth(0.55f)
                    .graphicsLayer { alpha = a }
            ) {
                Text(
                    shown.title, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    shown.sub, fontSize = 16.sp, color = Pal.textDim,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = h * 0.36f, bottom = 120.dp)
        ) {
            if (quick.isNotEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                        for (row in quick.chunked(4)) {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (s in row) TvTile(songItem(vm, s, quick), Modifier.weight(1f), setHero)
                                for (k in row.size until 4) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            for (sh in yt.take(2)) {
                item { TvShelf(sh.title, sh.sub, ytItems(vm, sh), setHero) }
            }
            item {
                val shelf = if (vm.liked.isNotEmpty()) vm.liked.toList().take(20) else vm.recommended.take(12)
                TvShelf(
                    if (vm.liked.isNotEmpty()) "Liked songs" else "Made for you", "",
                    shelf.map { s -> songItem(vm, s, shelf) }, setHero
                )
            }
            if (vm.recommendedAlbums.isNotEmpty()) {
                item {
                    TvShelf(
                        "Albums for you", "",
                        vm.recommendedAlbums.map { al ->
                            ShelfItem(al.cover, al.title, "Album \u2022 " + al.artist) { vm.openAlbum(al) }
                        },
                        setHero
                    )
                }
            }
            val picks = vm.recommended.drop(4).take(12)
            if (UiOpts.homeQuick && picks.isNotEmpty()) {
                item { TvShelf("Quick picks", "", picks.map { s -> songItem(vm, s, picks) }, setHero) }
            }
            for (sh in (if (UiOpts.homeShelves) vm.genreShelves else emptyList())) {
                item { TvShelf(sh.name, "", sh.songs.map { s -> songItem(vm, s, sh.songs) }, setHero) }
            }
            for (sh in yt.drop(2)) {
                item { TvShelf(sh.title, sh.sub, ytItems(vm, sh), setHero) }
            }
            if (UiOpts.homeArtists && vm.likedArtists.isNotEmpty()) {
                item {
                    TvShelf(
                        "Your artists", "",
                        vm.likedArtists.toList().map { ar ->
                            ShelfItem(ar.cover, ar.name, "Artist", true) { vm.openArtist(ar.name) }
                        },
                        setHero
                    )
                }
            }
            if (quick.isEmpty() && yt.isEmpty()) {
                item {
                    Text(
                        if (vm.ytHomeLoading) "Loading your music..." else "Nothing to show yet. Check the internet.",
                        fontSize = 16.sp, color = Pal.textDim,
                        modifier = Modifier.padding(start = 8.dp, top = 24.dp)
                    )
                }
            }
        }
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
        var n = 0
        while (n < 8) {
            delay(400)
            try {
                playFocus.requestFocus()
            } catch (e: Exception) {
            }
            n++
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
            .blockTaps()
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
