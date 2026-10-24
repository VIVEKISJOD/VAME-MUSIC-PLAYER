package com.ameos.music

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.roundToInt

// Where the cover of the small player (the pill) is on screen. The mini player writes it,
// the player animation reads it so the big player can be "sucked" into exactly that spot.
object PillPos {
    var rect by mutableStateOf<Rect?>(null)
}

// Phone: Now Playing gets sucked into the mini player pill when it closes and pops out of it
// when it opens. Two things happen at once:
//  1. the whole screen squeezes towards the pill (thin and flat first, like a slot)
//  2. the big cover leaves the screen as a small flying picture and lands on the pill cover
// Values are easy to tune: the 0.55f (how long it stays wide) and the 0.04f (how flat it gets).
@Composable
fun PlayerMorph(
    visible: Boolean,
    cover: String?,
    fly: Boolean,
    content: @Composable () -> Unit
) {
    val spec: AnimationSpec<Float> =
        if (UiOpts.reduceMotion) snap() else spring(dampingRatio = 0.9f, stiffness = 240f)
    val p by animateFloatAsState(if (visible) 1f else 0f, spec, label = "playerMorph")
    if (!(visible || p > 0.002f)) return
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val pill = PillPos.rect
        val fallback = with(density) { 44.dp.toPx() }
        val ex = pill?.left ?: with(density) { 26.dp.toPx() }
        val ey = pill?.top ?: (hPx - with(density) { 170.dp.toPx() })
        val ew = pill?.width ?: fallback
        val eh = pill?.height ?: fallback
        val cx = ex + ew / 2f
        val cy = ey + eh / 2f
        // where the big artwork sits in Now Playing (same maths as NowPlayingScreen)
        val colW = minOf(wPx, with(density) { 520.dp.toPx() })
        val artH = minOf(colW, hPx * 0.56f)
        val sx = (wPx - colW) / 2f
        val q = p.coerceIn(0f, 1f)

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(
                        (cx / wPx).coerceIn(0f, 1f), (cy / hPx).coerceIn(0f, 1f)
                    )
                    scaleX = 0.10f + 0.90f * q.pow(0.55f)
                    scaleY = 0.04f + 0.96f * q
                    alpha = if (fly) ((q - 0.14f) / 0.5f).coerceIn(0f, 1f) else ((q - 0.06f) / 0.5f).coerceIn(0f, 1f)
                    shape = RoundedCornerShape(160.dp * (1f - q) * (1f - q))
                    clip = q < 0.999f
                }
        ) {
            content()
        }

        // the flying cover: big artwork <-> pill cover
        val f = 1f - q
        val fa = minOf(1f, f * 7f) * minOf(1f, q * 20f)
        if (fly && cover != null && fa > 0.01f) {
            val x = sx + (ex - sx) * f
            val y = 0f + (ey - 0f) * f
            val w = colW + (ew - colW) * f
            val h = artH + (eh - artH) * f
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                    .graphicsLayer { alpha = fa }
            ) {
                Cover(cover, Modifier.fillMaxSize(), RoundedCornerShape((12f * f).dp))
            }
        }
    }
}
