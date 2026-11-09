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

// Phone: Now Playing slides up from the bottom (where the mini player pill is) and slides back
// down when it closes. ONE cover only: the old "flying cover" and the squash into the pill were
// the cause of the double cover glitch, so both are gone. The screen is fully opaque almost at
// once (no half-faded player showing Home through it).
// To tune: durations in morphSpec(), the 0.18f (how fast it becomes opaque), the 0.10f travel.
@Composable
fun PlayerMorph(
    visible: Boolean,
    cover: String?,
    fly: Boolean,
    content: @Composable () -> Unit
) {
    val p by animateFloatAsState(if (visible) 1f else 0f, morphSpec(visible), label = "playerMorph")
    if (!(visible || p > 0.002f)) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val hPx = with(LocalDensity.current) { maxHeight.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val q = p.coerceIn(0f, 1f)
                    translationY = (1f - q) * hPx * 0.94f
                    alpha = (q / 0.18f).coerceIn(0f, 1f)
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    clip = q < 0.999f
                }
        ) {
            content()
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
