package com.ameos.music

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(vm: MainViewModel) {
    BackHandler { vm.showSettings = false }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        vm.refreshCacheSize()
        vm.refreshDownloadBytes()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Pal.bgTop, Pal.bgMid, Pal.bgBot),
                    endY = 1500f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.showSettings = false }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Text(
                    "Settings", fontSize = 34.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            AppearanceSection()
            AppIconSection()
            AccountSection(vm)

            SectionLabel("Home title")
            TextField(
                value = vm.title,
                onValueChange = { vm.updateTitle(it) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x33FFFFFF),
                    unfocusedContainerColor = Color(0x33FFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Whatever you type replaces the big word at the top of Home.",
                fontSize = 12.sp, color = Pal.muted,
                modifier = Modifier.padding(top = 6.dp)
            )

            SectionLabel("Your name")
            TextField(
                value = vm.userName,
                onValueChange = { vm.updateUserName(it) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x33FFFFFF),
                    unfocusedContainerColor = Color(0x33FFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Shown on your Library stats cards.",
                fontSize = 12.sp, color = Pal.muted,
                modifier = Modifier.padding(top = 6.dp)
            )

            SectionLabel("Your taste")
            Row(
                Modifier.fillMaxWidth().clickable { vm.showOnboarding = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Change music taste", fontSize = 16.sp, color = Color.White)
                    Text(
                        if (vm.genres.isEmpty()) "Pick the types of songs you like."
                        else vm.genres.joinToString(", "),
                        fontSize = 12.sp, color = Pal.muted
                    )
                }
            }

            SectionLabel("Playback")
            SwitchRow(
                "Endless queue", "When the queue ends, keep adding similar songs.",
                vm.endless
            ) { vm.setEndlessOn(it) }
            SwitchRow(
                "Stop when removed from recents", "Swipe the app away and the music stops completely.",
                vm.stopOnSwipe
            ) { vm.updateStopOnSwipe(it) }
            Text(
                "Audio quality", fontSize = 16.sp, color = Color.White,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                "Lower quality uses less data. Applies from the next song.",
                fontSize = 12.sp, color = Pal.muted
            )
            QualityChips(vm.quality) { vm.updateQuality(it) }

            SwitchRow(
                "Keep player in notification",
                "Keeps the notification / Xiaomi island when paused for a long time.",
                vm.keepAlive
            ) { vm.setKeepAliveOn(it) }
            ActionRow(
                "Allow background use (battery)",
                "Stops the phone from closing the player. Tap, then choose Allow."
            ) {
                try {
                    val i = Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + ctx.packageName)
                    )
                    ctx.startActivity(i)
                } catch (e: Exception) {
                    try {
                        val i2 = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + ctx.packageName)
                        )
                        ctx.startActivity(i2)
                    } catch (e2: Exception) {
                    }
                }
            }

            SectionLabel("Downloads")
            ActionRow(
                "Open Downloads",
                vm.downloads.size.toString() + " songs - " +
                    String.format("%.1f MB", vm.downloadBytes / 1048576.0)
            ) {
                vm.showSettings = false
                vm.downloadsOpen = true
            }
            Text(
                "Download quality", fontSize = 16.sp, color = Color.White,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                "Applies to new downloads.",
                fontSize = 12.sp, color = Pal.muted
            )
            QualityChips(vm.downloadQuality) { vm.updateDownloadQuality(it) }

            SectionLabel("Songs")
            SwitchRow(
                "Official songs only",
                "Hides remixes, slowed + reverb, lofi, covers and lyrics-channel uploads.",
                vm.officialOnly
            ) { vm.setOfficialOnlyOn(it) }
            SwitchRow(
                "Hinglish lyrics", "Shows Hindi lyrics written in English letters.",
                vm.hinglish
            ) { vm.setHinglishOn(it) }
            Text(
                "Lyrics size", fontSize = 16.sp, color = Color.White,
                modifier = Modifier.padding(top = 10.dp)
            )
            QualityChips(vm.lyricsSize, listOf("Small", "Medium", "Large")) { vm.updateLyricsSize(it) }
            SwitchRow(
                "Keep screen on in lyrics", "The screen stays awake while the lyrics are open.",
                vm.keepScreenLyrics
            ) { vm.setKeepScreenLyricsOn(it) }

            SectionLabel("Storage")
            ActionRow(
                "Clear cache",
                if (vm.cacheSize.isEmpty()) "Cached covers and song links"
                else vm.cacheSize + " used by covers and song links"
            ) { vm.clearCache() }
            ActionRow(
                "Clear listening history", "Resets Last played and Your Type suggestions."
            ) { vm.clearHistory() }

            SectionLabel("Updates")
            ActionRow(
                "Check for updates",
                if (vm.updateStatus.isEmpty()) "Looks for a newer VAME on GitHub" else vm.updateStatus
            ) { vm.checkUpdate(true) }

            SectionLabel("About")
            Text(
                "VAME Music v" + Updater.currentVersion(ctx) + " - made by vxvekggs",
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
            Spacer(Modifier.height(10.dp))
            InstagramButton(ctx)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .glassFill()
                    .clickable { openUrl(ctx, "https://github.com/VIVEKISJOD/VAME-MUSIC-PLAYER") }
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Source code on GitHub", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Open source under GPLv3. Interface inspired by AmeOS 16 by the AmeOS team " +
                    "(CC BY 4.0). Built with NewPipeExtractor (GPLv3), AndroidX Media3, Compose, " +
                    "Coil, OkHttp and Kotlin (Apache 2.0). Lyrics from LRCLIB. " +
                    "VAME is not affiliated with YouTube, Spotify or Apple.",
                fontSize = 12.sp, color = Pal.textDim
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color.White)
            Text(sub, fontSize = 12.sp, color = Pal.muted)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(title: String, sub: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(title, fontSize = 16.sp, color = Color.White)
        Text(sub, fontSize = 12.sp, color = Pal.muted)
    }
}

@Composable
private fun QualityChips(
    selected: Int,
    names: List<String> = listOf("High", "Normal", "Data saver"),
    onPick: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        names.forEachIndexed { i, n ->
            val on = selected == i
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Pal.accent else Color(0x33FFFFFF))
                    .clickable { onPick(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    n, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (on) Pal.bgTop else Color.White
                )
            }
        }
    }
}


private fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Link nahi khul paya", Toast.LENGTH_SHORT).show()
    }
}

// opens the profile in the Instagram app (or the browser if the app is missing).
// Instagram does not let apps press Follow for you, so the Follow button is one tap away there.
private fun openInstagram(ctx: Context) {
    try {
        val app = Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/_u/vxvekggs"))
            .setPackage("com.instagram.android")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(app)
    } catch (e: Exception) {
        openUrl(ctx, "https://www.instagram.com/vxvekggs/")
    }
}

@Composable
private fun InstagramButton(ctx: Context) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                )
            )
            .clickable { openInstagram(ctx) }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.PersonAdd, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Follow @vxvekggs on Instagram", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
