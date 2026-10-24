package com.ameos.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

// A tablet held sideways has the side player. A tablet held upright works like a phone.
@Composable
fun isTabletLandscape(): Boolean {
    val c = LocalConfiguration.current
    return c.screenWidthDp >= 600 && c.screenWidthDp > c.screenHeightDp
}

// Tablet (sideways), side player closed: a small box with the cover that you can drag to the
// left or right edge. It remembers where you left it. Tap it to open the side player.
@Composable
fun TabletMiniBox(vm: MainViewModel) {
    val song = vm.current
    AnimatedVisibility(
        visible = song != null && !vm.showPlayer && !vm.showLyrics,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.6f)
    ) {
        val cover = song?.cover
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        BoxWithConstraints(Modifier.fillMaxSize().navigationBarsPadding()) {
            val boxPx = with(density) { 84.dp.toPx() }
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val rightMargin = with(density) { 16.dp.toPx() }
            val leftMargin = with(density) { 104.dp.toPx() } // clear of the side rail
            val minY = 0f
            val maxY = (hPx - boxPx).coerceAtLeast(0f)
            fun homeX(right: Boolean): Float = if (right) wPx - boxPx - rightMargin else leftMargin
            val startY = (hPx - boxPx - with(density) { vm.boxBottomDp.dp.toPx() }).coerceIn(minY, maxY)
            val ox = remember(wPx, hPx) { Animatable(homeX(vm.boxRight)) }
            val oy = remember(wPx, hPx) { Animatable(startY) }
            Box(
                Modifier
                    .offset { IntOffset(ox.value.roundToInt(), oy.value.roundToInt()) }
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Pal.miniA)
                    .border(1.dp, Pal.accent.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .pointerInput(wPx, hPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val right = ox.value + boxPx / 2f > wPx / 2f
                                val bottomDp = ((hPx - oy.value - boxPx).coerceAtLeast(0f) / density.density).roundToInt()
                                vm.saveBoxPos(right, bottomDp)
                                scope.launch { ox.animateTo(homeX(right), spring(dampingRatio = 0.75f, stiffness = 380f)) }
                            },
                            onDragCancel = {
                                scope.launch { ox.animateTo(homeX(vm.boxRight)) }
                            }
                        ) { change, drag ->
                            change.consume()
                            scope.launch {
                                ox.snapTo((ox.value + drag.x).coerceIn(0f, (wPx - boxPx).coerceAtLeast(0f)))
                                oy.snapTo((oy.value + drag.y).coerceIn(minY, maxY))
                            }
                        }
                    }
                    .clickable { vm.showPlayer = true }
            ) {
                Cover(cover, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { vm.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = Color.White, modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
