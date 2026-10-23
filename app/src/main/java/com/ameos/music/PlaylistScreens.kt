package com.ameos.music

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val PBg: Color get() = Pal.surface
private val PCard: Color get() = Pal.surface2
private val PAccent: Color get() = Pal.accent
private val PDim: Color get() = Pal.textDim

// ---------------- playlist page ----------------

@Composable
fun PlaylistScreen(vm: MainViewModel) {
    val pl = vm.playlists.firstOrNull { it.id == vm.openPlaylistId } ?: return
    BackHandler(enabled = vm.playlistOpen) { vm.closePlaylist() }
    val mins = (pl.songs.sumOf { it.durationSec } / 60).toInt()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Pal.bgTop, Pal.bgMid, Pal.bgBot)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.closePlaylist() }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    vm.editPlaylistId = pl.id
                    vm.pendingSong = null
                    vm.showPlaylistDialog = true
                }) {
                    Icon(Icons.Filled.Edit, null, tint = Color.White)
                }
            }

            // big cover with the name under it
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Cover(
                    pl.shownCover(),
                    Modifier.fillMaxWidth(0.55f).widthIn(max = 240.dp).aspectRatio(1f),
                    RoundedCornerShape(22.dp),
                    hiRes = true
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    pl.name, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(pl.songs.size.toString() + " songs", fontSize = 13.sp, color = PDim)
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CREATED BY", fontSize = 11.sp, color = Pal.muted)
                    Text("You", fontSize = 15.sp, color = Color.White)
                }
                Spacer(Modifier.width(24.dp))
                Column {
                    Text("LASTS", fontSize = 11.sp, color = Pal.muted)
                    Text(mins.toString() + " Mins", fontSize = 15.sp, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF5A1A1A)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.playPlaylist(pl.id, true) }) {
                        Icon(Icons.Filled.Shuffle, null, tint = Color.White)
                    }
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD9381E))
                            .clickable { vm.playPlaylist(pl.id, false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))

            if (pl.songs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No songs yet. Open the \"...\" menu on any song and tap Add to playlist.",
                        fontSize = 15.sp, color = PDim
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 150.dp)
                ) {
                    itemsIndexed(pl.songs) { _, s -> PlaylistSongRow(vm, pl, s) }
                }
            }
        }
        MiniPlayer(
            vm,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistSongRow(vm: MainViewModel, pl: Playlist, s: Song) {
    var menu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .appear()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .glassFill()
            .combinedClickable(
                onClick = { vm.play(s, pl.songs) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.songMenu = s
                }
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(s.cover, Modifier.size(52.dp), RoundedCornerShape(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title, fontSize = 16.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.artist, fontSize = 13.sp, color = Pal.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreHoriz, null, tint = Pal.textDim)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Play next") },
                    onClick = { menu = false; vm.playNext(s) }
                )
                DropdownMenuItem(
                    text = { Text("Remove from playlist") },
                    onClick = { menu = false; vm.removeFromPlaylist(pl.id, s) }
                )
            }
        }
    }
}

// ---------------- create / edit dialog ----------------

@Composable
fun PlaylistEditDialog(vm: MainViewModel) {
    val ctx = LocalContext.current
    val editing = vm.playlists.firstOrNull { it.id == vm.editPlaylistId }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var picked by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) picked = uri
    }
    val closeNow = {
        vm.showPlaylistDialog = false
        vm.editPlaylistId = null
        vm.pendingSong = null
    }

    AnimDialog(onClosed = closeNow) { close ->
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .glassPanel(RoundedCornerShape(32.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (editing != null) "Edit playlist" else "New playlist",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(PCard)
                    .clickable {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                val shown = picked?.toString() ?: editing?.shownCover()
                if (shown != null) {
                    Cover(shown, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddPhotoAlternate, null, tint = PAccent, modifier = Modifier.size(40.dp))
                        Text("Add cover", fontSize = 14.sp, color = PDim)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            TextField(
                value = name,
                onValueChange = { name = it.take(40) },
                placeholder = { Text("Playlist name", color = PDim) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PCard,
                    unfocusedContainerColor = PCard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PAccent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(PCard)
                        .clickable { close() }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(PAccent)
                        .clickable {
                            if (editing != null) {
                                vm.editPlaylist(editing.id, name, picked)
                            } else {
                                vm.createPlaylist(name, picked, vm.pendingSong)
                                Toast.makeText(ctx, "Playlist created", Toast.LENGTH_SHORT).show()
                            }
                            close()
                        }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (editing != null) "Save" else "Create",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A1014)
                    )
                }
            }
            if (editing != null) {
                Text(
                    "Delete playlist", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8A80),
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            vm.playlistOpen = false
                            vm.deletePlaylist(editing.id)
                            close()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// ---------------- "Add to playlist" picker ----------------

@Composable
fun AddToPlaylistDialog(vm: MainViewModel) {
    val song = vm.addTarget ?: return
    val ctx = LocalContext.current
    val closeNow = { vm.addTarget = null }

    AnimDialog(onClosed = closeNow) { close ->
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(32.dp))
                .glassPanel(RoundedCornerShape(32.dp))
                .padding(20.dp)
        ) {
            Text("Add to playlist", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                song.title, fontSize = 14.sp, color = PDim, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PCard)
                        .clickable {
                            vm.pendingSong = song
                            vm.editPlaylistId = null
                            vm.showPlaylistDialog = true
                            vm.addTarget = null
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Pal.accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, null, tint = PAccent)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("New playlist", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                vm.playlists.forEach { p ->
                    val has = p.songs.any { it.url == song.url }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(PCard)
                            .clickable {
                                val added = vm.addToPlaylist(p.id, song)
                                Toast.makeText(
                                    ctx,
                                    if (added) "Added to " + p.name else "Already in " + p.name,
                                    Toast.LENGTH_SHORT
                                ).show()
                                close()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cover(p.shownCover(), Modifier.size(48.dp), RoundedCornerShape(12.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(p.songs.size.toString() + " songs", fontSize = 13.sp, color = PDim)
                        }
                        if (has) Icon(Icons.Filled.Check, null, tint = PAccent)
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PCard)
                    .clickable { close() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Close", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
