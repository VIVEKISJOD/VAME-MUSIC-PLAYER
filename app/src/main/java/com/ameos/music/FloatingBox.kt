package com.ameos.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// A tablet (short side 600dp or more) held sideways has the side rail + side player.
// A tablet held upright works like a phone. A phone held sideways gets the Standby player.
@Composable
fun isTabletLandscape(): Boolean {
    val c = LocalConfiguration.current
    return minOf(c.screenWidthDp, c.screenHeightDp) >= 600 && c.screenWidthDp > c.screenHeightDp
}

@Composable
fun isPhoneLandscape(): Boolean {
    val c = LocalConfiguration.current
    return minOf(c.screenWidthDp, c.screenHeightDp) < 600 && c.screenWidthDp > c.screenHeightDp
}

// Tablet (sideways) small player. Settings > Tablet mini player picks the box or the pill.
// It also stays on top of Settings so the music can be stopped while changing something there.
@Composable
fun TabletMini(vm: MainViewModel) {
    val show = vm.current != null && (vm.showSettings || (!vm.showPlayer && !vm.showLyrics))
    val open: () -> Unit = {
        vm.showSettings = false
        vm.showPlayer = true
    }
    if (UiOpts.tabletPill) TabletPill(vm, show, open) else TabletMiniBox(vm, show, open)
}

// Medium size pill (not full width), centred in the page area.
@Composable
private fun TabletPill(vm: MainViewModel, visible: Boolean, onOpen: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(300)) { it },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { it }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(start = if (vm.showSettings) 0.dp else 92.dp, bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            MiniPlayer(vm, Modifier.width(UiOpts.pillDp.dp), force = true, onOpen = onOpen)
        }
    }
}

@Composable
private fun BoxBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Int, onClick: () -> Unit) {
    val tick = rememberTick()
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .clickable {
                tick()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size((size * 0.62f).dp))
    }
}

// Small box with the cover, title and previous / play / next. Drag it to the left or right
// edge, it remembers where you left it. Tap the cover to open the side player.
@Composable
private fun TabletMiniBox(vm: MainViewModel, visible: Boolean, onOpen: () -> Unit) {
    val song = vm.current
    AnimatedVisibility(
        visible = visible && song != null,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.6f)
    ) {
        val cover = song?.cover
        val tick = rememberTick()
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val shape = RoundedCornerShape(26.dp)
        BoxWithConstraints(Modifier.fillMaxSize().navigationBarsPadding()) {
            val boxW = with(density) { 156.dp.toPx() }
            val boxH = with(density) { 204.dp.toPx() } // 156 cover + 48 buttons
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val rightMargin = with(density) { 16.dp.toPx() }
            val leftMargin = with(density) { 104.dp.toPx() } // clear of the side rail
            val minY = 0f
            val maxY = (hPx - boxH).coerceAtLeast(0f)
            fun homeX(right: Boolean): Float = if (right) wPx - boxW - rightMargin else leftMargin
            val startY = (hPx - boxH - with(density) { vm.boxBottomDp.dp.toPx() }).coerceIn(minY, maxY)
            val ox = remember(wPx, hPx) { Animatable(homeX(vm.boxRight)) }
            val oy = remember(wPx, hPx) { Animatable(startY) }
            Column(
                Modifier
                    .offset { IntOffset(ox.value.roundToInt(), oy.value.roundToInt()) }
                    .width(156.dp)
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(Pal.miniA, Pal.miniB)))
                    .glassFill()
                    .border(1.dp, Pal.accent.copy(alpha = 0.4f), shape)
                    .pointerInput(wPx, hPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val right = ox.value + boxW / 2f > wPx / 2f
                                val bottomDp = ((hPx - oy.value - boxH).coerceAtLeast(0f) / density.density).roundToInt()
                                vm.saveBoxPos(right, bottomDp)
                                scope.launch { ox.animateTo(homeX(right), spring(dampingRatio = 0.75f, stiffness = 380f)) }
                            },
                            onDragCancel = {
                                scope.launch { ox.animateTo(homeX(vm.boxRight)) }
                            }
                        ) { change, drag ->
                            change.consume()
                            scope.launch {
                                ox.snapTo((ox.value + drag.x).coerceIn(0f, (wPx - boxW).coerceAtLeast(0f)))
                                oy.snapTo((oy.value + drag.y).coerceIn(minY, maxY))
                            }
                        }
                    }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onOpen() }
                ) {
                    Cover(cover, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.45f to Color.Transparent,
                                    1f to Color(0xCC000000)
                                )
                            )
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                    ) {
                        Text(
                            song?.title ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            artistName(song?.artist ?: ""), fontSize = 11.sp, color = Color(0xCCFFFFFF),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(48.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxBtn(Icons.Filled.SkipPrevious, 40) { vm.prev() }
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x40FFFFFF))
                            .clickable {
                                tick()
                                vm.toggle()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (vm.loading) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null, tint = Color.White, modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    BoxBtn(Icons.Filled.SkipNext, 40) { vm.next() }
                }
            }
        }
    }
}
