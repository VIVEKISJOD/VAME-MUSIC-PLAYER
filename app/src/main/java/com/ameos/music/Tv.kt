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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.unit.coerceIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ===================== TV (Android TV / remote control) =====================
// One APK for phone, tablet and TV. On a TV (or Settings > Interface > TV mode = On) VameRoot
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

// ---------- remote helpers used by every TV page ----------

// Puts the remote on `first` when a page opens (it retries for a few seconds because the screen can
// still be in touch mode) and catches the first D-pad key when nothing on the page is focused.
// It stops by itself as soon as anything on the page has focus, so it never steals focus later.
fun Modifier.tvPageFocus(first: FocusRequester, active: Boolean = true): Modifier = composed {
    var has by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (UiOpts.tv && active) {
            var n = 0
            while (n < 24 && !has) {
                delay(350)
                if (!has) {
                    try {
                        first.requestFocus()
                    } catch (e: Exception) {
                    }
                }
                n++
            }
        }
    }
    this
        .onFocusChanged { has = it.hasFocus }
        .onPreviewKeyEvent { e ->
            val k = e.key
            val pad = k == Key.DirectionUp || k == Key.DirectionDown ||
                k == Key.DirectionLeft || k == Key.DirectionRight || k == Key.DirectionCenter
            if (UiOpts.tv && active && pad && !has && e.type == KeyEventType.KeyDown) {
                try {
                    first.requestFocus()
                } catch (ex: Exception) {
                }
                true
            } else {
                false
            }
        }
}

// A clickable surface that has its own background. On a phone / tablet it is the normal
// clip + background + clickable, on a TV it is a tvClick (ring + grow while the remote is on it).
fun Modifier.padClick(
    shape: Shape,
    bg: Color = Color.Transparent,
    grow: Float = 1.03f,
    onClick: () -> Unit
): Modifier =
    if (UiOpts.tv) this.tvClick(shape, grow, bg, onClick)
    else this.clip(shape).background(bg).clickable { onClick() }

// A clickable for something that is already drawn with its own background / border: on a TV it only
// adds the white focus ring on top, on a phone / tablet it is a plain clickable.
fun Modifier.padRing(shape: Shape, enabled: Boolean = true, onClick: () -> Unit): Modifier =
    if (UiOpts.tv) {
        if (enabled) this.tvClick(shape, 1f, Color.Transparent, onClick) else this
    } else {
        this.clickable(enabled = enabled) { onClick() }
    }

// back / close button: a normal IconButton on phone / tablet, a big focus ring button on a TV
@Composable
fun BackBtn(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.ArrowBack,
    onClick: () -> Unit
) {
    if (UiOpts.tv) {
        Box(
            modifier
                .size(52.dp)
                .tvClick(CircleShape, 1.1f, Color(0x33FFFFFF)) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, "Back", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(icon, null, tint = Color.White)
        }
    }
}

// slider: the normal Slider on phone / tablet; on a TV Left / Right change the value in 5% steps
@Composable
fun PadSlider(
    value: Float,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    if (!UiOpts.tv) {
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onFinished,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Pal.accent,
                activeTrackColor = Pal.accent,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
        return
    }
    var f by remember { mutableStateOf(false) }
    val span = range.endInclusive - range.start
    val frac = ((value - range.start) / span).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .onFocusChanged { f = it.isFocused }
            .focusable()
            .onKeyEvent { e ->
                val left = e.key == Key.DirectionLeft
                val right = e.key == Key.DirectionRight
                if (left || right) {
                    if (e.type == KeyEventType.KeyDown) {
                        val step = span / 20f
                        onChange((value + (if (right) step else -step)).coerceIn(range.start, range.endInclusive))
                    } else if (e.type == KeyEventType.KeyUp) {
                        onFinished()
                    }
                    true
                } else {
                    false
                }
            }
            .then(if (f) Modifier.background(Color(0x2EFFFFFF), shape).border(3.dp, Color.White, shape) else Modifier)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x33FFFFFF))
        )
        Box(
            Modifier
                .fillMaxWidth(frac)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Pal.accent)
        )
    }
}

// ---------- left bar ----------

@Composable
private fun TvRailItem(
    name: String,
    icon: ImageVector,
    on: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .tvClick(
                RoundedCornerShape(16.dp), 1.03f,
                if (on) Color(0x33FFFFFF) else Color.Transparent
            ) { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (on) Pal.accent else Pal.textDim, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            name, fontSize = 16.sp,
            color = if (on) Color.White else Pal.textDim,
            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

// The left bar: logo, the pages, Settings, and at the bottom the song that is playing now.
// OK on the song opens the player.
@Composable
private fun TvRail(vm: MainViewModel, homeFocus: FocusRequester) {
    BoxWithConstraints(
        Modifier
            .width(210.dp)
            .fillMaxHeight()
            .background(Color(0x40000000))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        val coverSide = (maxHeight - 360.dp).coerceIn(64.dp, 170.dp)
        Column(Modifier.fillMaxSize()) {
            Text(
                "VAME", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Pal.accent,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TvRailItem("Home", Icons.Filled.Home, vm.tab == 0, Modifier.focusRequester(homeFocus)) {
                    vm.goHome()
                }
                TvRailItem("Explore", Icons.Filled.Explore, vm.tab == 1) {
                    vm.goHome()
                    vm.tab = 1
                }
                TvRailItem("Your type", Icons.Filled.RadioButtonChecked, vm.tab == 2) {
                    vm.goHome()
                    vm.tab = 2
                }
                TvRailItem("Library", Icons.Filled.LibraryMusic, vm.tab == 3) {
                    vm.goHome()
                    vm.tab = 3
                }
                TvRailItem("Settings", Icons.Filled.Settings, false) { vm.showSettings = true }
            }
            Spacer(Modifier.weight(1f))
            val song = vm.current
            if (song == null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(14.dp)
                ) {
                    Icon(Icons.Filled.MusicNote, null, tint = Pal.accent, modifier = Modifier.size(26.dp))
                    Text(
                        "Nothing playing", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White, modifier = Modifier.padding(top = 6.dp)
                    )
                    Text("Pick a song", fontSize = 12.sp, color = Pal.textDim)
                }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .tvClick(RoundedCornerShape(18.dp), 1.04f, Color(0x2EFFFFFF)) {
                            vm.showPlayer = true
                        }
                        .padding(8.dp)
                ) {
                    Box(Modifier.size(coverSide)) {
                        Cover(song.cover, Modifier.fillMaxSize(), RoundedCornerShape(12.dp))
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Pal.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null, tint = Pal.bgTop, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        song.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        song.artist, fontSize = 11.sp, color = Pal.textDim,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------- the TV screen ----------

@Composable
fun TvShell(vm: MainViewModel) {
    val homeFocus = remember { FocusRequester() }
    // while the player or the lyrics cover the screen, the pages underneath are removed (the remote
    // must only be able to reach what is on top)
    val full = vm.showPlayer || vm.showLyrics
    Box(Modifier.fillMaxSize().tvPageFocus(homeFocus, !full)) {
        if (!full) {
            Row(Modifier.fillMaxSize()) {
                TvRail(vm, homeFocus)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp, end = 26.dp)
                ) {
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
            contentPadding = PaddingValues(top = h * 0.36f, bottom = 48.dp)
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

// left part of the player: the cover as a big "canvas" with a very slow zoom
@Composable
private fun TvCanvas(cover: String?) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val t = rememberInfiniteTransition(label = "tvCanvas")
        val z by t.animateFloat(
            1.02f, 1.10f,
            infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
            label = "tvCanvasZoom"
        )
        Box(
            Modifier
                .size(side)
                .clip(RoundedCornerShape(28.dp))
                .background(Pal.surface)
        ) {
            Cover(
                cover,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = z
                    scaleY = z
                },
                RoundedCornerShape(0.dp), hiRes = true
            )
        }
    }
}

// right part of the player: the queue. The X button (top right) closes it.
@Composable
private fun TvQueue(vm: MainViewModel, w: Dp, closeFocus: FocusRequester, onClose: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (vm.index - 1).coerceAtLeast(0))
    Column(
        Modifier
            .width(w)
            .fillMaxHeight()
            .clip(shape)
            .background(Color(0x66000000))
            .border(1.dp, Color(0x22FFFFFF), shape)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Up next", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(vm.queue.size.toString() + " songs", fontSize = 13.sp, color = Pal.textDim)
            }
            TvIconBtn(Icons.Filled.Shuffle, "Shuffle up next", size = 48.dp) { vm.shuffleUpNext() }
            Spacer(Modifier.width(8.dp))
            TvIconBtn(Icons.Filled.Close, "Close queue", Modifier.focusRequester(closeFocus), size = 48.dp) {
                onClose()
            }
        }
        Spacer(Modifier.height(12.dp))
        if (vm.queue.isEmpty()) {
            Text("The queue is empty.", fontSize = 16.sp, color = Pal.textDim)
        } else {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(vm.queue) { i, s ->
                    val cur = i == vm.index
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .tvClick(
                                RoundedCornerShape(14.dp), 1.02f,
                                if (cur) Pal.accent.copy(alpha = 0.22f) else Color.Transparent
                            ) { vm.playFromQueue(i) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cover(s.cover, Modifier.size(52.dp), RoundedCornerShape(8.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = if (cur) Pal.accent else Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                s.artist, fontSize = 12.sp, color = Pal.textDim,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (cur) {
                            Icon(
                                Icons.Filled.PlayArrow, null, tint = Pal.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// The player on a TV: three parts. Left = canvas (cover), middle = controls, right = queue.
// The queue button is always there. The X on the queue closes it and the player becomes two parts.
@Composable
fun TvPlayer(vm: MainViewModel) {
    val song = vm.current ?: return
    var queueOpen by remember { mutableStateOf(false) }
    BackHandler {
        if (queueOpen) queueOpen = false else vm.showPlayer = false
    }
    val tint = rememberCoverTint(song.cover)
    val playFocus = remember { FocusRequester() }
    val queueBtnFocus = remember { FocusRequester() }
    val closeFocus = remember { FocusRequester() }
    // when the queue opens the remote goes to its X button, when it closes it goes back to the queue button
    val skipFirst = remember { booleanArrayOf(true) }
    LaunchedEffect(queueOpen) {
        if (skipFirst[0]) {
            skipFirst[0] = false
        } else {
            delay(320)
            try {
                if (queueOpen) closeFocus.requestFocus() else queueBtnFocus.requestFocus()
            } catch (e: Exception) {
            }
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
            .tvPageFocus(playFocus)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 36.dp)) {
            val queueW = maxWidth * 0.32f
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                // 1. canvas
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    TvCanvas(song.cover)
                }
                Spacer(Modifier.width(40.dp))
                // 2. controls
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        song.title, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist, fontSize = 22.sp, color = Pal.textDim,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(26.dp))
                    TvSeek(vm)
                    Spacer(Modifier.height(18.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvIconBtn(Icons.Filled.SkipPrevious, "Previous", size = 64.dp) { vm.prev() }
                        TvIconBtn(
                            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play or pause",
                            Modifier.focusRequester(playFocus),
                            size = 84.dp, filled = true
                        ) { vm.toggle() }
                        TvIconBtn(Icons.Filled.SkipNext, "Next", size = 64.dp) { vm.next() }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val liked = vm.isLiked(song)
                        TvIconBtn(Icons.Filled.Shuffle, "Shuffle up next", size = 52.dp) { vm.shuffleUpNext() }
                        TvIconBtn(
                            if (vm.repeat == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            "Repeat", size = 52.dp,
                            tint = if (vm.repeat != 0) Pal.accent else Color.White
                        ) { vm.cycleRepeat() }
                        TvIconBtn(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Like", size = 52.dp,
                            tint = if (liked) Pal.accent else Color.White
                        ) { vm.toggleLike(song) }
                        TvIconBtn(Icons.Filled.Subject, "Lyrics", size = 52.dp) { vm.showLyrics = true }
                        TvIconBtn(Icons.Filled.Cast, "Play on another device", size = 52.dp) {
                            vm.showOutputDialog = true
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    // the queue button: always here
                    Row(
                        Modifier
                            .focusRequester(queueBtnFocus)
                            .tvClick(
                                RoundedCornerShape(50), 1.04f,
                                if (queueOpen) Pal.accent else Color(0x33FFFFFF)
                            ) { queueOpen = !queueOpen }
                            .padding(horizontal = 28.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.QueueMusic, null,
                            tint = if (queueOpen) Pal.bgTop else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (queueOpen) "Hide queue" else "Queue",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = if (queueOpen) Pal.bgTop else Color.White
                        )
                    }
                    val upcoming = vm.queue.getOrNull(vm.index + 1)
                    if (upcoming != null) {
                        Text(
                            "Next: " + upcoming.title, fontSize = 15.sp, color = Pal.textDim,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }
                }
                // 3. queue (only while it is open)
                AnimatedVisibility(
                    visible = queueOpen,
                    enter = expandHorizontally(tween(260)) + fadeIn(tween(260)),
                    exit = shrinkHorizontally(tween(220)) + fadeOut(tween(200))
                ) {
                    Row(Modifier.fillMaxHeight()) {
                        Spacer(Modifier.width(32.dp))
                        TvQueue(vm, queueW, closeFocus) { queueOpen = false }
                    }
                }
            }
        }
    }
}
