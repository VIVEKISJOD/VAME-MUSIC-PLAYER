package com.ameos.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Small card at the top while the music plays on another device:
// song, previous / play / next, volume, and a button to bring the music back to this device.
@Composable
fun RemoteCard(vm: MainViewModel, modifier: Modifier = Modifier) {
    val t = Connect.target ?: return
    val r = Connect.remote
    val song = vm.current
    var pos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(t.id) {
        while (true) {
            pos = vm.posMs()
            delay(250)
        }
    }
    val dur = vm.durMs().coerceAtLeast(1L)
    val vol = r?.vol ?: 50

    Column(
        modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xF2200E13))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(song?.cover, Modifier.size(44.dp), RoundedCornerShape(10.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song?.title ?: "Choose a song", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Playing on " + t.name, fontSize = 12.sp, color = Pal.accent,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { Connect.cmd(t, "prev") }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White)
            }
            IconButton(onClick = { Connect.cmd(t, "toggle") }, modifier = Modifier.size(42.dp)) {
                Icon(
                    if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    null, tint = Color.White, modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = { Connect.cmd(t, "next") }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.SkipNext, null, tint = Color.White)
            }
        }
        LinearProgressIndicator(
            progress = { (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp),
            color = Pal.accent,
            trackColor = Color(0x33FFFFFF)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.connectTo(null) }) {
                Icon(Icons.Filled.PhoneAndroid, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text("This device", fontSize = 12.sp, color = Color(0xAAFFFFFF))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { Connect.cmd(t, "vol", (vol - 10).coerceIn(0, 100).toLong()) }) {
                Icon(Icons.Filled.VolumeDown, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(vol.toString() + "%", fontSize = 12.sp, color = Color(0xAAFFFFFF))
            IconButton(onClick = { Connect.cmd(t, "vol", (vol + 10).coerceIn(0, 100).toLong()) }) {
                Icon(Icons.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}
