package com.ameos.music

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Where the small player is on screen. The mini player writes these, the player animation
// reads them so the big player can melt into exactly that spot.
//  pill  = the whole pill bar      rect = the little cover inside it
object PillPos {
    var pill by mutableStateOf<Rect?>(null)
    var rect by mutableStateOf<Rect?>(null)
}

// slow start, long soft landing (like iOS). Used for the player open / close.
val MorphEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private fun morphSpec(visible: Boolean): AnimationSpec<Float> =
    if (UiOpts.reduceMotion) snap() else tween(if (visible) 600 else 520, easing = MorphEase)

// Phone: Now Playing melts into the mini player pill when it closes and grows out of it when it
// opens. The whole screen is mapped onto the pill rectangle (so at the end it IS the pill),
// while the big cover leaves the screen as a flying picture and lands on the pill cover.
// To tune: durations in morphSpec(), the 0.08f / 0.5f in alpha (when the screen fades),
// the 10f / 14f in the flying cover fade.
@Composable
fun PlayerMorph(
    visible: Boolean,
    cover: String?,
    fly: Boolean,
    content: @Composable () -> Unit
) {
    val p by animateFloatAsState(if (visible) 1f else 0f, morphSpec(visible), label = "playerMorph")
    if (!(visible || p > 0.002f)) return
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        // the pill (fallback: a bar at the bottom) and its cover
        val pillR = PillPos.pill
        val pl = pillR?.left ?: with(density) { 16.dp.toPx() }
        val pt = pillR?.top ?: (hPx - with(density) { 170.dp.toPx() })
        val pw = pillR?.width ?: (wPx - with(density) { 32.dp.toPx() })
        val ph = pillR?.height ?: with(density) { 64.dp.toPx() }
        val coverR = PillPos.rect
        val fallback = with(density) { 44.dp.toPx() }
        val ex = coverR?.left ?: (pl + with(density) { 10.dp.toPx() })
        val ey = coverR?.top ?: (pt + (ph - fallback) / 2f)
        val ew = coverR?.width ?: fallback
        val eh = coverR?.height ?: fallback
        // the screen shrinks onto the pill rectangle
        val s0x = (pw / wPx).coerceIn(0.05f, 1f)
        val s0y = (ph / hPx).coerceIn(0.03f, 1f)
        val ox = if (s0x < 0.999f) (pl / (wPx * (1f - s0x))).coerceIn(0f, 1f) else 0.5f
        val oy = if (s0y < 0.999f) (pt / (hPx * (1f - s0y))).coerceIn(0f, 1f) else 0.5f
        // where the big artwork sits in Now Playing (same maths as NowPlayingScreen)
        val colW = minOf(wPx, with(density) { 520.dp.toPx() })
        val artH = minOf(colW, hPx * 0.56f)
        val sx = (wPx - colW) / 2f
        val q = p.coerceIn(0f, 1f)

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(ox, oy)
                    scaleX = s0x + (1f - s0x) * q
                    scaleY = s0y + (1f - s0y) * q
                    alpha = ((q - 0.08f) / 0.5f).coerceIn(0f, 1f)
                    shape = RoundedCornerShape(160.dp * (1f - q) * (1f - q))
                    clip = q < 0.999f
                    // draw the screen once into a texture, then only move that texture (smooth)
                    compositingStrategy =
                        if (q < 0.999f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                }
        ) {
            content()
        }

        // the flying cover: big artwork <-> pill cover
        val f = 1f - q
        val fa = minOf(1f, f * 10f) * minOf(1f, q * 14f)
        if (fly && cover != null && fa > 0.01f) {
            val x = sx + (ex - sx) * f
            val y = ey * f
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

// Tablet (sideways): the side player grows out of the small box / pill and shrinks back into it.
@Composable
fun PanelMorph(visible: Boolean, origin: TransformOrigin, content: @Composable () -> Unit) {
    val p by animateFloatAsState(if (visible) 1f else 0f, morphSpec(visible), label = "panelMorph")
    if (!(visible || p > 0.002f)) return
    Box(
        Modifier.graphicsLayer {
            val q = p.coerceIn(0f, 1f)
            transformOrigin = origin
            val sc = 0.14f + 0.86f * q
            scaleX = sc
            scaleY = sc
            alpha = ((q - 0.03f) / 0.35f).coerceIn(0f, 1f)
            shape = RoundedCornerShape(120.dp * (1f - q) * (1f - q))
            clip = q < 0.999f
            compositingStrategy =
                if (q < 0.999f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        }
    ) {
        content()
    }
}
