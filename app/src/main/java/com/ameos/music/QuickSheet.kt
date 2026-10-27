package com.ameos.music

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SheetBg: Color get() = Pal.surface
private val RowBg: Color get() = Pal.surface2
private val SubTxt: Color get() = Pal.textDim

private fun mmss(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return m.toString() + ":" + s.toString().padStart(2, '0')
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SubTxt,
        modifier = Modifier.padding(start = 10.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun SheetRow(icon: ImageVector, title: String, sub: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(RowBg)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SubTxt, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (sub != null) {
                Text(sub, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SubTxt)
            }
        }
    }
}

@Composable
private fun ActionBtn(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(RowBg)
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// Quick settings bottom sheet (opened by the "..." button).
@Composable
fun QuickSheet(vm: MainViewModel) {
    val show = vm.showMenu && vm.current != null
    val ctx = LocalContext.current
    val close = { vm.showMenu = false }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { close() }
            )
        }
        AnimatedVisibility(
            visible = show,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(360)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(200))
        ) {
            val song = vm.current
            if (song != null) {
                // registered only while the sheet exists, so Back closes the sheet first
                BackHandler { close() }
                val scope = rememberCoroutineScope()
                val dragY = remember { Animatable(0f) }
                Column(
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .offset { IntOffset(0, dragY.value.roundToInt()) }
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .glassPanel(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .blockTaps()
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (dragY.value > 200f) close()
                                        else scope.launch { dragY.animateTo(0f) }
                                    },
                                    onDragCancel = { scope.launch { dragY.animateTo(0f) } },
                                    onVerticalDrag = { change, amount ->
                                        change.consume()
                                        scope.launch {
                                            dragY.snapTo((dragY.value + amount).coerceAtLeast(0f))
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(44.dp, 5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x66FFFFFF))
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        // header
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Cover(song.cover, Modifier.size(60.dp), RoundedCornerShape(16.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    song.title, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song.artist, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                                    color = SubTxt, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // three big buttons
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionBtn(Icons.Filled.PlaylistAdd, "Add to playlist", Modifier.weight(1f)) {
                                close()
                                vm.addTarget = song
                            }
                            val dlProgress = vm.downloading[song.url]
                            val dlDone = vm.isDownloaded(song)
                            ActionBtn(
                                if (dlDone) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                if (dlDone) "Downloaded"
                                else if (dlProgress != null) (dlProgress * 100).toInt().toString() + "%"
                                else "Download",
                                Modifier.weight(1f)
                            ) {
                                if (dlDone) {
                                    Toast.makeText(ctx, "Already downloaded", Toast.LENGTH_SHORT).show()
                                } else if (dlProgress == null) {
                                    vm.downloadSong(song)
                                    Toast.makeText(ctx, "Download started", Toast.LENGTH_SHORT).show()
                                }
                            }
                            ActionBtn(Icons.Filled.Share, "Share", Modifier.weight(1f)) {
                                close()
                                try {
                                    val send = Intent(Intent.ACTION_SEND)
                                    send.type = "text/plain"
                                    send.putExtra(Intent.EXTRA_TEXT, song.title + " - " + song.artist + "\n" + song.url)
                                    ctx.startActivity(Intent.createChooser(send, null))
                                } catch (e: Exception) {
                                }
                            }
                        }

                        SheetLabel("Playback")
                        SheetRow(Icons.Filled.Radio, "Start radio", "Play a station based on this song") {
                            close()
                            vm.startRadio()
                        }
                        SheetRow(Icons.Filled.QueuePlayNext, "Play next", "Plays after the current song") {
                            close()
                            vm.playNext(song)
                        }
                        SheetRow(Icons.Filled.QueueMusic, "Add to queue", "Add to end of queue") {
                            close()
                            vm.addToQueue(song)
                        }
                        SheetRow(
                            Icons.Filled.PushPin,
                            if (vm.isDialed("song", song.url)) "Remove from speed dial" else "Pin to speed dial",
                            "Quick tile on Home"
                        ) {
                            close()
                            pinToggle(vm, ctx, Dial("song", song.url, song.title, song.artist, song.cover))
                        }
                        SheetRow(Icons.Filled.GraphicEq, "Audio Effects") {
                            try {
                                val fx = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                                fx.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, vm.player.audioSessionId)
                                fx.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, ctx.packageName)
                                fx.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                                ctx.startActivity(fx)
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "No equalizer found on this phone", Toast.LENGTH_SHORT).show()
                            }
                        }
                        val sleepSub: String? =
                            if (vm.sleepLeftSec > 0) mmss(vm.sleepLeftSec) + " left"
                            else if (vm.timesLeft > 0) vm.timesLeft.toString() + " plays left"
                            else if (vm.sleepAfterSong) "Stops at end of song"
                            else null
                        SheetRow(Icons.Filled.Bedtime, "Sleep Timer", sleepSub) {
                            close()
                            vm.showSleepDialog = true
                        }
                        SheetRow(Icons.Filled.Shuffle, "Shuffle", "Shuffle the queue") {
                            close()
                            vm.shuffleQueue()
                        }
                        SheetRow(
                            Icons.Filled.Autorenew,
                            if (vm.endless) "Endless: On" else "Endless: Off",
                            "Add similar songs automatically"
                        ) {
                            vm.setEndlessOn(!vm.endless)
                        }

                        SheetRow(Icons.Filled.Bluetooth, "Audio output", vm.outputLabel) {
                            close()
                            vm.refreshOutputs()
                            vm.showOutputDialog = true
                        }
                        SheetRow(
                            Icons.Filled.Speed, "Playback speed",
                            if (vm.speed == 1f) "Normal (1x)" else vm.speed.toString() + "x"
                        ) {
                            vm.cycleSpeed()
                        }
                        SheetRow(
                            Icons.Filled.FastForward,
                            if (vm.skipSilence) "Skip silence: On" else "Skip silence: Off",
                            "Skips quiet gaps inside songs"
                        ) {
                            vm.setSkipSilenceOn(!vm.skipSilence)
                        }
                        SheetRow(
                            Icons.Filled.Translate,
                            if (vm.hinglish) "Hinglish lyrics: On" else "Hinglish lyrics: Off",
                            "Hindi lyrics in English letters"
                        ) {
                            vm.setHinglishOn(!vm.hinglish)
                        }

                        SheetLabel("Go to")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(RowBg)
                                .clickable {
                                    close()
                                    vm.showPlayer = false
                                    vm.showLyrics = false
                                    vm.openArtist(song.artist)
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(52.dp).clip(RoundedCornerShape(50)).background(Pal.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, null, tint = SubTxt, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(18.dp))
                            Text(
                                song.artist, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }

                        val recs = vm.suggestions.take(8)
                        if (recs.isNotEmpty()) {
                            SheetLabel("Recommendations")
                            recs.forEach { r ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(RowBg)
                                        .clickable {
                                            close()
                                            vm.play(r, vm.suggestions)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Cover(r.cover, Modifier.size(52.dp), RoundedCornerShape(12.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            r.title, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            r.artist, fontSize = 14.sp, color = SubTxt,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.navigationBarsPadding().height(28.dp))
                    }
                }
            }
        }
    }
}
