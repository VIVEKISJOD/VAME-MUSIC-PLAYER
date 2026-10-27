package com.ameos.music

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val plSortNames = listOf("Manual ordering", "Title A-Z", "Artist A-Z", "Newest first")

// sort + "find in playlist" filter for a song list
fun sortFind(list: List<Song>, mode: Int, q: String): List<Song> {
    var out = when (mode) {
        1 -> list.sortedBy { it.title.lowercase() }
        2 -> list.sortedBy { it.artist.lowercase() }
        3 -> list.reversed()
        else -> list
    }
    val k = q.trim().lowercase()
    if (k.isNotEmpty()) {
        out = out.filter { it.title.lowercase().contains(k) || it.artist.lowercase().contains(k) }
    }
    return out
}

// the search box that "Find in playlist" opens
@Composable
fun FindBar(q: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x26FFFFFF))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
            if (q.isEmpty()) Text("Find in playlist", fontSize = 15.sp, color = Pal.textDim)
            BasicTextField(
                value = q,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(24.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@Composable
private fun BigAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF333333))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.padding(top = 8.dp))
    }
}

// The "..." menu of a playlist / album (like YouTube Music):
// Play next, Share, Shuffle play, Sort, Find, Start mix, Add to queue, Save to playlist,
// Delete playlist, Pin to speed dial, Help.
// onDelete = null hides "Delete playlist" (albums and YouTube playlists cannot be deleted here).
@Composable
fun PlaylistMenuSheet(
    vm: MainViewModel,
    title: String,
    subtitle: String,
    cover: String?,
    songs: List<Song>,
    dial: Dial,
    sortMode: Int,
    onSort: (Int) -> Unit,
    onFind: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var pick by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
    BackHandler { if (pick) pick = false else onDismiss() }

    fun say(t: String) = Toast.makeText(ctx, t, Toast.LENGTH_SHORT).show()
    val pinned = vm.isDialed(dial.kind, dial.id)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxH)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color(0xFF1E1E1E))
                .blockTaps()
                .navigationBarsPadding()
        ) {
            // header
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cover(cover, Modifier.size(46.dp), RoundedCornerShape(10.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(subtitle, fontSize = 14.sp, color = Pal.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(50)).clickable { if (pick) pick = false else onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x33FFFFFF)))

            if (pick) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
                    Text(
                        "Save to playlist", fontSize = 15.sp, color = Pal.textDim,
                        modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp)
                    )
                    MenuRow(Icons.Filled.PlaylistAdd, "New playlist") {
                        vm.newPlaylistFrom(title, songs)
                        say("Playlist made (" + songs.size + " songs)")
                        onDismiss()
                    }
                    for (pl in vm.playlists.toList()) {
                        MenuRow(Icons.Filled.PlaylistPlay, pl.name) {
                            val n = vm.addAllToPlaylist(pl.id, songs)
                            say(n.toString() + " songs added")
                            onDismiss()
                        }
                    }
                }
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BigAction(Icons.Filled.QueuePlayNext, "Play next", Modifier.weight(1f)) {
                            vm.playAllNext(songs)
                            say("Will play next")
                            onDismiss()
                        }
                        BigAction(Icons.Filled.Share, "Share", Modifier.weight(1f)) {
                            try {
                                val send = Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Listening to " + title + " (" + songs.size + " songs) on VAME Music"
                                    )
                                ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (e: Exception) {
                            }
                            onDismiss()
                        }
                    }
                    MenuRow(Icons.Filled.Shuffle, "Shuffle play") {
                        if (songs.isNotEmpty()) {
                            val q = songs.shuffled()
                            vm.play(q.first(), q)
                        }
                        onDismiss()
                    }
                    MenuRow(Icons.Filled.Sort, "Sort: " + plSortNames[sortMode.coerceIn(0, plSortNames.size - 1)]) {
                        onSort((sortMode + 1) % plSortNames.size)
                    }
                    MenuRow(Icons.Filled.Search, "Find in playlist") {
                        onFind()
                        onDismiss()
                    }
                    MenuRow(Icons.Filled.Sensors, "Start mix") {
                        vm.startMix(songs)
                        onDismiss()
                    }
                    MenuRow(Icons.Filled.QueuePlayNext, "Add to queue") {
                        vm.addAllToQueue(songs)
                        say("Added to queue")
                        onDismiss()
                    }
                    MenuRow(Icons.Filled.PlaylistAdd, "Save to playlist") { pick = true }
                    if (onDelete != null) {
                        MenuRow(Icons.Filled.Delete, "Delete playlist") { confirm = true }
                    }
                    MenuRow(Icons.Filled.PushPin, if (pinned) "Remove from speed dial" else "Pin to speed dial") {
                        pinToggle(vm, ctx, dial)
                        onDismiss()
                    }
                    MenuRow(Icons.Outlined.Help, "Help and feedback") {
                        try {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/VIVEKISJOD/VAME-MUSIC-PLAYER/issues"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) {
                        }
                        onDismiss()
                    }
                }
            }
        }
    }
    if (confirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete playlist?") },
            text = { Text("\"" + title + "\" will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}
