package com.ameos.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class StbTm(val hour: String, val minute: String, val ampm: String, val date: String, val dateShort: String, val sec: Int)

private fun stbTm(ms: Long): StbTm {
    val d = Date(ms)
    val h12 = UiOpts.standby12h
    val hm = SimpleDateFormat(if (h12) "h:mm" else "HH:mm", Locale.getDefault()).format(d)
    val ap = if (h12) SimpleDateFormat("a", Locale.getDefault()).format(d).uppercase() else ""
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(d)
    val dateShort = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(d)
    val k = hm.indexOf(':')
    val hh = if (k > 0) hm.substring(0, k) else hm
    val mm = if (k > 0) hm.substring(k + 1) else ""
    return StbTm(hh, mm, ap, date, dateShort, ((ms / 1000) % 60).toInt())
}

// small clock card, top right of the standby player; tap = full screen clock
@Composable
fun StbClockPill(nowMs: Long, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val t = stbTm(nowMs)
    val shape = RoundedCornerShape(20.dp)
    val colon by animateFloatAsState(if (t.sec % 2 == 0) 1f else 0.3f, tween(400), label = "stbColon")
    Column(
        modifier
            .clip(shape)
            .background(lerp(tint, Color.Black, 0.6f).copy(alpha = 0.45f))
            .glassFill()
            .border(1.dp, Color(0x33FFFFFF), shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(t.hour, fontSize = 22.sp, fontWeight = FontWeight.Light, color = Color.White)
            Text(":", fontSize = 22.sp, fontWeight = FontWeight.Light, color = Color.White, modifier = Modifier.alpha(colon))
            Text(t.minute, fontSize = 22.sp, fontWeight = FontWeight.Light, color = Color.White)
            if (t.ampm.isNotEmpty()) {
                Text(
                    t.ampm, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xCCFFFFFF),
                    modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
                )
            }
        }
        Text(t.dateShort, fontSize = 10.sp, color = Color(0xAAFFFFFF), maxLines = 1)
    }
}

// full screen clock: giant time, date, and the playing song at the bottom. Tap = back to the player.
@Composable
fun StbClockFull(nowMs: Long, tint: Color, vm: MainViewModel, onClose: () -> Unit) {
    val t = stbTm(nowMs)
    val song = vm.current
    val colon by animateFloatAsState(if (t.sec % 2 == 0) 1f else 0.25f, tween(450), label = "stbColonBig")
    BackHandler { onClose() }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Pal.bgBot,
                        lerp(tint, Color.Black, if (Pal.amoled) 0.92f else 0.6f),
                        lerp(tint, Color.Black, if (Pal.amoled) 1f else 0.85f)
                    )
                )
            )
            .clickable(interactionSource = remember2(), indication = null) { onClose() }
    ) {
        if (song != null) {
            Cover(song.cover, Modifier.fillMaxSize().alpha(0.18f).blur(100.dp), RoundedCornerShape(0.dp), hiRes = true)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(24.dp)
        ) {
            val big = minOf(maxHeight.value * 0.42f, maxWidth.value * 0.19f)
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.hour, fontSize = big.sp, fontWeight = FontWeight.ExtraLight, color = Color.White)
                    Text(
                        ":", fontSize = big.sp, fontWeight = FontWeight.ExtraLight, color = Color.White,
                        modifier = Modifier.alpha(colon)
                    )
                    Text(t.minute, fontSize = big.sp, fontWeight = FontWeight.ExtraLight, color = Color.White)
                    if (t.ampm.isNotEmpty()) {
                        Text(
                            t.ampm, fontSize = (big * 0.2f).sp, fontWeight = FontWeight.Medium,
                            color = Color(0xCCFFFFFF), modifier = Modifier.padding(start = 10.dp, top = (big * 0.35f).dp)
                        )
                    }
                }
                Text(
                    t.date.uppercase(), fontSize = (big * 0.16f).coerceIn(14f, 30f).sp,
                    fontWeight = FontWeight.Medium, color = Color(0xB3FFFFFF), letterSpacing = 3.sp
                )
            }
            if (song != null) {
                val shape = RoundedCornerShape(50)
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 420.dp)
                        .clip(shape)
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), shape)
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cover(song.cover, Modifier.size(44.dp), CircleShape)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            artistName(song.artist), fontSize = 12.sp, color = Color(0xB3FFFFFF),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable { vm.toggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = Color.White, modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun remember2(): MutableInteractionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
