package com.ameos.music

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import coil.request.ImageRequest

val CardDark = Color(0xFF141010)
val Stroke = Color(0x22FFFFFF)

data class GenreTile(val name: String, val covers: List<String>, val artists: String)

// Small spring "press" effect: the item shrinks a little while a finger is on it.
// It only watches touches, so clicks still work normally.
fun Modifier.pressScale(target: Float = 0.96f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val s by animateFloatAsState(
        if (pressed) target else 1f,
        spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "press"
    )
    this
        .graphicsLayer {
            scaleX = s
            scaleY = s
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                waitForUpOrCancellation(PointerEventPass.Initial)
                pressed = false
            }
        }
}

// Glass surface: see-through layer + light edge on top.
private val GlassEdge = Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x11FFFFFF)))

fun Modifier.glass(shape: Shape, top: Color = Color(0xCC2A1A1D), bottom: Color = Color(0xCC120C0C)): Modifier =
    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, GlassEdge, shape)

@Composable
fun Cover(url: String?, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(12.dp)) {
    Box(modifier.clip(shape).background(Color(0xFF2A1215))) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, badge: String? = null) {
    Row(
        Modifier.padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (badge != null) {
            Text(
                badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x44FFFFFF))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(song: Song, vm: MainViewModel, list: List<Song>) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pressScale()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, Stroke, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { vm.play(song, list) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.songMenu = song
                }
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(song.cover, Modifier.size(52.dp), RoundedCornerShape(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, fontSize = 16.sp, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        val ctx = LocalContext.current
        IconButton(onClick = {
            val idle = vm.current == null
            vm.playNext(song)
            if (!idle) Toast.makeText(ctx, "Will play next", Toast.LENGTH_SHORT).show()
        }) {
            Icon(Icons.Filled.QueuePlayNext, null, tint = Color(0xFFD9C0C2))
        }
        IconButton(onClick = { vm.toggleLike(song) }) {
            if (vm.isLiked(song)) {
                Icon(Icons.Filled.Favorite, null, tint = Color(0xFFFFB3B9))
            } else {
                Icon(Icons.Outlined.FavoriteBorder, null, tint = Color(0xFFD9C0C2))
            }
        }
    }
}

@Composable
fun MiniPlayer(vm: MainViewModel, modifier: Modifier = Modifier) {
    val song = vm.current ?: return
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .pressScale(0.97f)
            .clip(shape)
            .clickable { vm.showPlayer = true }
            .background(Brush.horizontalGradient(listOf(Color(0xE65A1218), Color(0xE62A0C10))))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x66FF8A95), Color(0x22FF6B7A))), shape)
    ) {
        Cover(
            song.cover,
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(170.dp).alpha(0.40f).blur(10.dp),
            RoundedCornerShape(0.dp)
        )
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(song.cover, Modifier.size(44.dp), RoundedCornerShape(12.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist, fontSize = 12.sp, color = Color(0xFFE6BDC0),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { vm.prev() }) {
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White)
            }
            IconButton(onClick = { vm.toggle() }) {
                if (vm.loading) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null, tint = Color.White
                    )
                }
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Filled.SkipNext, null, tint = Color.White)
            }
        }
    }
}

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        TabItem("Home", Icons.Filled.Home),
        TabItem("Search", Icons.Filled.Search),
        TabItem("Your type", Icons.Filled.RadioButtonChecked),
        TabItem("Library", Icons.Filled.LibraryMusic)
    )
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .glass(shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { i, item ->
            val on = i == selected
            val col = if (on) Color.White else Color(0xFF8C7C7E)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pressScale(0.9f)
                    .clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(item.icon, null, tint = col, modifier = Modifier.size(22.dp))
                Text(
                    item.label, fontSize = 11.sp, color = col,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

// round Home button (next to the mini player on detail pages, like the design)
@Composable
fun HomeFab(vm: MainViewModel) {
    Box(
        Modifier
            .size(64.dp)
            .pressScale(0.92f)
            .glass(CircleShape)
            .clickable { vm.goHome() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Home, null, tint = Color(0xFFD9C0C2), modifier = Modifier.size(26.dp))
    }
}

// bottom area of detail pages: mini player + Home button
@Composable
fun DetailBottomBar(vm: MainViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (vm.current != null) {
            MiniPlayer(vm, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
        } else {
            Spacer(Modifier.weight(1f))
        }
        HomeFab(vm)
    }
}
