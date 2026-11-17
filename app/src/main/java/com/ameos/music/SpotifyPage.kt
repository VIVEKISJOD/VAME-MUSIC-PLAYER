package com.ameos.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// step72: the Spotify-style page for playlists and albums.
// A big cover on a colour taken from the cover. While you scroll up the big cover shrinks and fades,
// and a small cover + the name slide into the top bar (with a small play button), like Spotify.
@Composable
fun SpotifyPage(
    cover: String?,
    title: String,
    line1: String,
    line2: String,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    topActions: @Composable RowScope.() -> Unit = {},
    leftActions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit
) {
    val tint = rememberCoverTint(cover)
    val screenH = LocalConfiguration.current.screenHeightDp
    val coverSize = minOf(250f, screenH * 0.32f).dp
    val collapsePx = with(LocalDensity.current) { (coverSize + 24.dp).toPx() }
    val prog by remember(collapsePx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapsePx).coerceIn(0f, 1f)
        }
    }
    val miniOn by remember { derivedStateOf { prog > 0.85f } }
    val top = if (Pal.light) lerp(tint, Pal.bgMid, 0.55f) else lerp(tint, Color.Black, if (Pal.lime) 0.45f else 0.3f)

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to top, 0.55f to Pal.bgMid, 1f to Pal.bgBot))
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 200.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.statusBarsPadding().height(56.dp))
                    Box(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Cover(
                            cover,
                            Modifier
                                .size(coverSize)
                                .graphicsLayer {
                                    val p = prog
                                    scaleX = 1f - 0.35f * p
                                    scaleY = 1f - 0.35f * p
                                    alpha = (1f - 1.3f * p).coerceIn(0f, 1f)
                                    translationY = 30f * p
                                },
                            RoundedCornerShape(14.dp),
                            hiRes = true
                        )
                    }
                    Text(
                        title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Pal.fg,
                        letterSpacing = (-0.5).sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    if (line1.isNotBlank()) {
                        Text(
                            line1, fontSize = 14.sp, color = Pal.textDim,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (line2.isNotBlank()) {
                        Text(line2, fontSize = 12.sp, color = Pal.muted, modifier = Modifier.padding(top = 2.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        leftActions()
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onShuffle) {
                            Icon(Icons.Filled.Shuffle, null, tint = Pal.fg, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(58.dp)
                                .pressScale(0.92f)
                                .clip(CircleShape)
                                .background(Pal.accent)
                                .clickable { onPlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Pal.onAccent, modifier = Modifier.size(34.dp))
                        }
                    }
                }
            }
            content()
        }

        // top bar: solid colour fades in while scrolling; small cover + name + small play button appear
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(Pal.bgMid.copy(alpha = prog)) }
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, null, tint = Pal.fg)
            }
            Row(
                Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = ((prog - 0.6f) / 0.4f).coerceIn(0f, 1f) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cover(cover, Modifier.size(36.dp), RoundedCornerShape(8.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Pal.fg,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Pal.accent)
                        .clickable(enabled = miniOn) { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Pal.onAccent, modifier = Modifier.size(24.dp))
                }
            }
            topActions()
        }
    }
}
