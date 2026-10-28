package com.ameos.music

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Building blocks for the new Home: big cover cards (name under the picture, never a tiny
// picture with a wide name), shelf titles, loading shimmer, empty / error states, tap vibration.

// where the last tapped card was (0..1 of the screen). Detail pages grow out of this spot.
object DetailOrigin {
    var fx by mutableFloatStateOf(0.5f)
    var fy by mutableFloatStateOf(0.4f)

    fun set(x: Float, y: Float) {
        fx = x.coerceIn(0f, 1f)
        fy = y.coerceIn(0f, 1f)
    }
}

// a soft tick on taps (Settings > Interface > Vibration on tap)
@Composable
fun rememberTick(): () -> Unit {
    val h = LocalHapticFeedback.current
    return remember(h) {
        {
            if (UiOpts.haptics) {
                try {
                    h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (e: Exception) {
                }
            }
        }
    }
}

data class ShelfItem(
    val cover: String?,
    val title: String,
    val sub: String,
    val round: Boolean = false,
    val active: Boolean = false,
    val onClick: () -> Unit
)

// card width so that two cards and a piece of the third show on a phone
@Composable
fun shelfW(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return if (w >= 600) 220.dp else ((w - 46) / 2.15f).dp
}

@Composable
fun BigCard(item: ShelfItem, w: Dp, modifier: Modifier = Modifier) {
    val tick = rememberTick()
    val view = LocalView.current
    var center by remember { mutableStateOf(Offset.Zero) }
    val shape = if (item.round) CircleShape else RoundedCornerShape(16.dp)
    Column(
        modifier
            .width(w)
            .onGloballyPositioned { center = it.boundsInRoot().center }
            .pressScale(0.97f)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                tick()
                DetailOrigin.set(
                    center.x / maxOf(1, view.width),
                    center.y / maxOf(1, view.height)
                )
                item.onClick()
            },
        horizontalAlignment = if (item.round) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Box(Modifier.size(w)) {
            Cover(item.cover, Modifier.size(w), shape, hiRes = true)
            Box(Modifier.size(w).clip(shape).border(1.dp, GlassEdge, shape))
        }
        Text(
            item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (item.active) Pal.accent else Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = if (item.round) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.padding(top = 10.dp)
        )
        if (item.sub.isNotBlank()) {
            Text(
                item.sub, fontSize = 12.sp, color = Color(0x99FFFFFF),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = if (item.round) TextAlign.Center else TextAlign.Start
            )
        }
    }
}

@Composable
fun CardShelf(list: List<ShelfItem>, w: Dp) {
    LazyRow(Modifier.appear(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(list) { card -> BigCard(card, w) }
    }
}

@Composable
fun CardGrid(list: List<ShelfItem>, cols: Int) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cw = (maxWidth - 14.dp * (cols - 1)) / cols
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (rowItems in list.chunked(cols)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (card in rowItems) BigCard(card, cw)
                    for (k in rowItems.size until cols) Spacer(Modifier.width(cw))
                }
            }
        }
    }
}

// big shelf title with a small dim line under it, and an optional button on the right
@Composable
fun ShelfTitle(title: String, sub: String = "", trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.appear().fillMaxWidth().padding(top = 30.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White,
                letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (sub.isNotBlank()) {
                Text(
                    sub.uppercase(), fontSize = 11.sp, letterSpacing = 0.8.sp, color = Color(0x99FFFFFF),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) trailing()
    }
}

// ---------- loading shimmer ----------

@Composable
fun shimmerBrush(): Brush {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(Color(0x12FFFFFF), Color(0x30FFFFFF), Color(0x12FFFFFF)),
        start = Offset(x * 1400f - 500f, 0f),
        end = Offset(x * 1400f, 300f)
    )
}

@Composable
fun ShimmerShelf(w: Dp) {
    val b = shimmerBrush()
    LazyRow(userScrollEnabled = false, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(3) {
            Column(Modifier.width(w)) {
                Box(Modifier.size(w).clip(RoundedCornerShape(20.dp)).background(b))
                Box(
                    Modifier.padding(top = 10.dp).width(w * 0.7f).height(12.dp)
                        .clip(RoundedCornerShape(6.dp)).background(b)
                )
                Box(
                    Modifier.padding(top = 6.dp).width(w * 0.45f).height(10.dp)
                        .clip(RoundedCornerShape(5.dp)).background(b)
                )
            }
        }
    }
}

// ---------- empty / error ----------

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    val tick = rememberTick()
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).glassFill(),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Pal.accent, modifier = Modifier.size(30.dp))
        }
        Text(
            title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text, fontSize = 13.sp, color = Pal.textDim, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (action != null && onAction != null) {
            Text(
                action, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Pal.accentDeep)
                    .clickable {
                        tick()
                        onAction()
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}
