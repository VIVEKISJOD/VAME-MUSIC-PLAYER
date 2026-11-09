package com.ameos.music

import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

// Real frosted glass (Android 12+). The pages are drawn into one recorded layer (frostSource).
// Every glass bar (dock, mini player) draws a blurred copy of that layer behind itself
// (frostBehind), moved so it lines up with what is really behind the bar.
// Older phones, or Glass > Material = Fastest, keep the milky look (nothing here runs).
object Frost {
    var layer by mutableStateOf<GraphicsLayer?>(null)
    var origin by mutableStateOf(Offset.Zero)
    val supported: Boolean get() = Build.VERSION.SDK_INT >= 31
    val active: Boolean get() = supported && Pal.glassReal && Pal.glassMat >= 1 && Pal.glassBlur > 0.5f
}

// put on the box that holds the pages (NOT on the dock)
fun Modifier.frostSource(): Modifier = composed {
    if (!Frost.supported) {
        this
    } else {
        val l = rememberGraphicsLayer()
        DisposableEffect(l) {
            Frost.layer = l
            onDispose { if (Frost.layer === l) Frost.layer = null }
        }
        this
            .onGloballyPositioned { Frost.origin = it.positionInRoot() }
            .drawWithContent {
                l.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                    this@drawWithContent.drawContent()
                }
                drawLayer(l)
            }
    }
}

// put on a glass bar (after clip): draws the blurred pages behind it
fun Modifier.frostBehind(): Modifier = composed {
    val src = Frost.layer
    if (!Frost.active || src == null) {
        this
    } else {
        val blurLayer = rememberGraphicsLayer()
        var pos by remember { mutableStateOf(Offset.Zero) }
        val rad = with(LocalDensity.current) { Pal.glassBlur.coerceIn(1f, 40f) * density }
        this
            .onGloballyPositioned { pos = it.positionInRoot() }
            .drawWithContent {
                val dx = Frost.origin.x - pos.x
                val dy = Frost.origin.y - pos.y
                blurLayer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                    translate(dx, dy) { drawLayer(src) }
                }
                blurLayer.renderEffect = BlurEffect(rad, rad, TileMode.Clamp)
                drawLayer(blurLayer)
                drawContent()
            }
    }
}
