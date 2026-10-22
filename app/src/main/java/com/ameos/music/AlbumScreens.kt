package com.ameos.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val APink = Color(0xFFFFB3B9)
private val ADim = Color(0xFFC9B3B6)
private val ACard = Color(0xFF382C2F)

private fun mmss(sec: Long): String {
    val s = sec.coerceAtLeast(0L)
    return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}

// ---------------- ALBUM PAGE ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(n: Int, s: Song, vm: MainViewModel, list: List<Song>) {
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
                onClick = { vm.play(s, list) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.songMenu = s
                }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            n.toString(), fontSize = 20.sp, color = Color(0xFF8C7C7E),
            modifier = Modifier.width(34.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(s.title, fontSize = 16.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                artistName(s.artist), fontSize = 13.sp, color = Color(0xFF8C7C7E),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (s.durationSec > 0L) {
            Text(mmss(s.durationSec), fontSize = 13.sp, color = Color(0xFF8C7C7E))
        }
    }
}

@Composable
fun AlbumScreen(vm: MainViewModel) {
    BackHandler(enabled = vm.albumOpen) { vm.closeAlbum() }
    val album = vm.albumInfo ?: return
    val songs = vm.albumSongs
    val tint = rememberCoverTint(album.cover)

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(lerp(tint, Color.Black, 0.35f), Color(0xFF120D0D), Color(0xFF0A0808))
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
                IconButton(onClick = { vm.closeAlbum() }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                if (songs.isNotEmpty()) {
                    IconButton(onClick = {
                        vm.downloadAll(songs)
                    }) {
                        Icon(Icons.Filled.Download, null, tint = Color.White)
                    }
                }
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 170.dp)
            ) {
                item {
                    Cover(
                        album.cover,
                        Modifier.fillMaxWidth(0.78f).aspectRatio(1f),
                        RoundedCornerShape(20.dp),
                        hiRes = true
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                album.title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                                color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (album.artist.isNotBlank()) album.artist
                                else songs.firstOrNull()?.artist?.let { artistName(it) } ?: "",
                                fontSize = 15.sp, color = Color(0xFFC9B3B6),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { vm.playAlbum(true) }) {
                            Icon(Icons.Filled.Shuffle, null, tint = Color.White)
                        }
                        Box(
                            Modifier
                                .size(58.dp)
                                .pressScale(0.92f)
                                .clip(CircleShape)
                                .background(Color(0xFFD9381E))
                                .clickable { vm.playAlbum(false) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow, null, tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                if (vm.albumLoading && songs.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                Modifier.size(28.dp), color = APink, strokeWidth = 3.dp
                            )
                        }
                    }
                }
                itemsIndexed(songs) { i, s -> TrackRow(i + 1, s, vm, songs) }
            }
        }
        DetailBottomBar(vm, Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------- DOWNLOADS PAGE ----------------

@Composable
fun DownloadsScreen(vm: MainViewModel) {
    BackHandler(enabled = vm.downloadsOpen) { vm.downloadsOpen = false }
    LaunchedEffect(Unit) { vm.refreshDownloadBytes() }
    val songs = vm.downloads.toList()
    val mb = String.format("%.1f MB", vm.downloadBytes / 1048576.0)

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3A1511), Color(0xFF120D0D), Color(0xFF0A0808))
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
                IconButton(onClick = { vm.downloadsOpen = false }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Downloads", fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        songs.size.toString() + " songs - " + mb, fontSize = 12.sp,
                        color = Color(0xFF8C7C7E)
                    )
                }
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF5A1A1A)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.playDownloads(true) }) {
                        Icon(Icons.Filled.Shuffle, null, tint = Color.White)
                    }
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD9381E))
                            .clickable { vm.playDownloads(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow, null, tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 170.dp)
            ) {
                val active = vm.downloadingSongs.values.toList()
                if (active.isNotEmpty()) {
                    item { SectionLabel("Downloading") }
                    itemsIndexed(active) { _, s ->
                        val p = vm.downloading[s.url] ?: 0f
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardDark)
                                .padding(12.dp)
                        ) {
                            Text(
                                s.title, fontSize = 15.sp, color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier.fillMaxWidth().height(5.dp)
                                    .clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF))
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(p.coerceIn(0.02f, 1f)).fillMaxHeight()
                                        .clip(RoundedCornerShape(50)).background(APink)
                                )
                            }
                        }
                    }
                }
                if (songs.isEmpty() && active.isEmpty()) {
                    item {
                        Text(
                            "Nothing downloaded yet. Open the \"...\" menu on a song and tap Download.",
                            fontSize = 15.sp, color = ADim,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                if (songs.isNotEmpty()) {
                    item { SectionLabel("Saved on this phone") }
                }
                itemsIndexed(songs) { _, s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardDark)
                            .border(1.dp, Stroke, RoundedCornerShape(16.dp))
                            .clickable { vm.play(s, songs) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cover(s.cover, Modifier.size(52.dp), RoundedCornerShape(10.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.title, fontSize = 16.sp, color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                artistName(s.artist), fontSize = 13.sp, color = Color(0xFF8C7C7E),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { vm.deleteDownload(s) }) {
                            Icon(Icons.Filled.Delete, null, tint = Color(0xFFD9C0C2))
                        }
                    }
                }
            }
        }
        DetailBottomBar(vm, Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------- IMPORT PLAYLIST ----------------

@Composable
fun ImportDialog(vm: MainViewModel) {
    var link by remember { mutableStateOf("") }
    val clip = LocalClipboardManager.current
    val close: () -> Unit = {
        if (!vm.importBusy) {
            vm.showImportDialog = false
        }
    }
    // if a playlist link is already copied, put it in the box straight away
    LaunchedEffect(Unit) {
        val t = clip.getText()?.text
        if (t != null && t.contains("http") &&
            (t.contains("spotify", ignoreCase = true) || t.contains("list="))
        ) {
            link = t.trim()
        }
    }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF2A1F21))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Import playlist", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Paste a YouTube Music, YouTube or Spotify playlist / album link " +
                    "(must be public). Spotify links bring the first ~100 songs. " +
                    "For a bigger list or any other app, paste the songs instead: " +
                    "one per line, like Title - Artist.",
                fontSize = 13.sp, color = ADim, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
            TextField(
                value = link,
                onValueChange = { link = it },
                placeholder = { Text("Link, or songs one per line", color = ADim) },
                singleLine = false,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ACard,
                    unfocusedContainerColor = ACard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = APink
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Paste from clipboard", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = APink,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val t = clip.getText()?.text
                        if (t != null) link = t
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
            if (vm.importStatus.isNotEmpty()) {
                Text(
                    vm.importStatus, fontSize = 14.sp, color = Color.White,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (vm.importBusy) {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(26.dp), color = APink, strokeWidth = 3.dp)
                }
            }
            val doneId = vm.importedId
            if (doneId != null) {
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x33FFFFFF))
                        .clickable {
                            vm.showImportDialog = false
                            vm.openPlaylist(doneId)
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Open playlist", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(ACard)
                        .clickable { close() }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Close", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (vm.importBusy) Color(0x66FFB3B9) else APink)
                        .clickable { vm.importPlaylist(link) }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Import", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF3A1014)
                    )
                }
            }
        }
    }
}
