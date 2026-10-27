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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
private fun SettingsField(value: String, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onChange,
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
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    BackHandler { vm.showSettings = false }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        vm.refreshCacheSize()
        vm.refreshDownloadBytes()
    }
    val tabletNow = isTabletLandscape()
    val phoneNow = isPhoneLandscape()
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
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.showSettings = false }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Column {
                    Text(
                        "Settings", fontSize = 34.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        "VAME " + Updater.currentVersion(ctx) +
                            (if (vm.current != null) " - your music stays on at the bottom" else ""),
                        fontSize = 12.sp, color = Pal.muted
                    )
                }
            }

            // ---------------- look ----------------
            KitHeader(Icons.Filled.Palette, "Look", "Colours, glass, icon and size")
            KitCard {
                AppearanceSection(false)
            }
            Spacer(Modifier.height(10.dp))
            KitCard {
                AppIconSection(false)
            }
            Spacer(Modifier.height(10.dp))
            KitCard {
                KitLabel("Interface size", "Makes everything in the app a little smaller or bigger.")
                KitChips(UiOpts.uiSize, listOf("Small", "Normal", "Large")) {
                    UiOpts.uiSize = it
                    UiOpts.save(ctx)
                }
                KitDivider()
                KitLabel("Glass strength", "How see-through the cards and buttons are (colour glass modes).")
                KitChips(UiOpts.glass, listOf("Soft", "Normal", "Strong")) {
                    UiOpts.glass = it
                    UiOpts.save(ctx)
                }
                KitDivider()
                KitSwitch(
                    "Bottom bar names", "Show the word under each icon in the bottom bar.",
                    UiOpts.tabLabels
                ) {
                    UiOpts.tabLabels = it
                    UiOpts.save(ctx)
                }
                KitSwitch(
                    "Calmer animations", "Screens open at once instead of growing and sliding.",
                    UiOpts.reduceMotion
                ) {
                    UiOpts.reduceMotion = it
                    UiOpts.save(ctx)
                }
                KitSwitch(
                    "Vibration on tap", "A soft tick when you tap cards, buttons and the player.",
                    UiOpts.haptics
                ) {
                    UiOpts.haptics = it
                    UiOpts.save(ctx)
                }
                KitSwitch(
                    "Cover colour on pages", "The colour of the playing song tints the top of every page.",
                    UiOpts.pageTint
                ) {
                    UiOpts.pageTint = it
                    UiOpts.save(ctx)
                }
            }

            // ---------------- player ----------------
            KitHeader(Icons.Filled.MusicNote, "Player", "Small player, tablet and Standby")
            KitCard {
                KitLabel(
                    "Tablet small player" + (if (tabletNow) "" else " (turn the tablet sideways to see it)"),
                    "Box: a draggable cover box with previous / play / next. Pill: a medium bar at the bottom."
                )
                KitChips(if (UiOpts.tabletPill) 1 else 0, listOf("Box", "Pill")) {
                    UiOpts.tabletPill = it == 1
                    UiOpts.save(ctx)
                }
                KitLabel("Pill width")
                KitChips(UiOpts.pillWidth, listOf("Compact", "Medium", "Wide")) {
                    UiOpts.pillWidth = it
                    UiOpts.save(ctx)
                }
                KitDivider()
                KitLabel("Tablet player width")
                KitChips(if (vm.panelWide) 1 else 0, listOf("Normal", "Wide")) { vm.setPanelWideOn(it == 1) }
                KitDivider()
                KitSwitch(
                    "Standby mode",
                    "Turn the phone sideways while music plays and the big landscape player opens by itself. " +
                        "Turn it back and it closes." + (if (phoneNow) " (on now)" else ""),
                    UiOpts.standby
                ) {
                    UiOpts.standby = it
                    UiOpts.save(ctx)
                }
                KitLabel("Lyrics blur", "Blur: the lines far from the song fade out. Light blur: a little. No blur: every line is sharp.")
                KitChips(UiOpts.lyricsBlur, listOf("Blur", "Light blur", "No blur")) {
                    UiOpts.lyricsBlur = it
                    UiOpts.save(ctx)
                }
                KitLabel("Standby opens with")
                KitChips(if (UiOpts.standbyLyrics) 1 else 0, listOf("Player", "Lyrics")) {
                    UiOpts.standbyLyrics = it == 1
                    UiOpts.save(ctx)
                }
                KitSwitch(
                    "Keep screen on in Standby", "The screen stays awake while the landscape player is open.",
                    UiOpts.standbyAwake
                ) {
                    UiOpts.standbyAwake = it
                    UiOpts.save(ctx)
                }
                KitSwitch(
                    "Hide status bar in Standby", "Swipe from the edge to bring it back for a moment.",
                    UiOpts.standbyHideBars
                ) {
                    UiOpts.standbyHideBars = it
                    UiOpts.save(ctx)
                }
                KitSwitch(
                    "Standby button on tablet", "Shows a Standby button in the left rail of the tablet. " +
                        "The side player stays as it is.",
                    UiOpts.tabStandbyBtn
                ) {
                    UiOpts.tabStandbyBtn = it
                    UiOpts.save(ctx)
                }
                KitSwitch(
                    "12-hour clock in Standby", "Shows 3:21 PM instead of 15:21.",
                    UiOpts.standby12h
                ) {
                    UiOpts.standby12h = it
                    UiOpts.save(ctx)
                }
                KitSwitch(
                    "Let other devices play here", "Your other VAME devices on the same Wi-Fi can send music to this device and press its buttons.",
                    Connect.allow
                ) {
                    Connect.setAllow(ctx, it)
                }
            }

            // ---------------- home ----------------
            KitHeader(Icons.Filled.Home, "Home and you", "What Home shows")
            KitCard {
                KitLabel("Home title", "Whatever you type replaces the big word at the top of Home.")
                Spacer(Modifier.height(6.dp))
                SettingsField(vm.title) { vm.updateTitle(it) }
                Spacer(Modifier.height(10.dp))
                KitLabel("Your name", "Shown on your Library stats cards.")
                Spacer(Modifier.height(6.dp))
                SettingsField(vm.userName) { vm.updateUserName(it) }
                KitDivider()
                KitLabel("Open app on")
                KitChips(vm.startTab, listOf("Home", "Explore", "Your type", "Library")) { vm.updateStartTab(it) }
                KitDivider()
                KitLabel("Home songs language", "Home shelves are built from the genres you picked.")
                KitChips(vm.homeLang, listOf("English + Hindi", "English", "Hindi")) { vm.chooseHomeLang(it) }
                KitDivider()
                KitSwitch("Recents", "Shelf with the songs you played last (grid button shows all).", UiOpts.homeLastAlbums) {
                    UiOpts.homeLastAlbums = it
                    UiOpts.save(ctx)
                }
                KitSwitch("YouTube Music shelves", "Listen again, Featured playlists, Trending and more from YouTube Music.", UiOpts.homeYt) {
                    UiOpts.homeYt = it
                    UiOpts.save(ctx)
                }
                KitSwitch("Quick picks", "A shelf of extra songs to tap.", UiOpts.homeQuick) {
                    UiOpts.homeQuick = it
                    UiOpts.save(ctx)
                }
                KitSwitch("Genre shelves", "One shelf for each genre you picked.", UiOpts.homeShelves) {
                    UiOpts.homeShelves = it
                    UiOpts.save(ctx)
                }
                KitSwitch("Your artists", "Round pictures of the artists you liked.", UiOpts.homeArtists) {
                    UiOpts.homeArtists = it
                    UiOpts.save(ctx)
                }
                KitDivider()
                KitAction(
                    "Change music taste",
                    if (vm.genres.isEmpty()) "Pick the types of songs you like." else vm.genres.joinToString(", ")
                ) { vm.showOnboarding = true }
            }

            KitHeader(Icons.Filled.Person, "Artist page")
            KitCard {
                KitLabel("Picture")
                KitChips(if (vm.artistLatestPic) 0 else 1, listOf("Latest release", "Song cover")) {
                    vm.chooseArtistLatestPic(it == 0)
                }
                KitSwitch(
                    "Popular album card", "Shows the artist's top album under the play buttons.",
                    vm.artistAlbumCardOn
                ) { vm.chooseArtistAlbumCard(it) }
                KitLabel("Top songs shown")
                KitChips(vm.artistTop, listOf("10", "25", "All")) { vm.updateArtistTop(it) }
            }

            // ---------------- playback ----------------
            KitHeader(Icons.Filled.PlayArrow, "Playback")
            KitCard {
                KitSwitch(
                    "Endless queue", "When the queue ends, keep adding similar songs.",
                    vm.endless
                ) { vm.setEndlessOn(it) }
                KitSwitch(
                    "Stop when removed from recents", "Swipe the app away and the music stops completely.",
                    vm.stopOnSwipe
                ) { vm.updateStopOnSwipe(it) }
                KitSwitch(
                    "Keep player in notification",
                    "Keeps the notification / Xiaomi island when paused for a long time.",
                    vm.keepAlive
                ) { vm.setKeepAliveOn(it) }
                KitDivider()
                KitLabel("Audio quality", "Lower quality uses less data. Applies from the next song.")
                KitChips(vm.quality, listOf("High", "Normal", "Data saver")) { vm.updateQuality(it) }
                KitDivider()
                KitAction(
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
            }

            KitHeader(Icons.Filled.Subject, "Songs and lyrics")
            KitCard {
                KitSwitch(
                    "Official songs only",
                    "Hides remixes, slowed + reverb, lofi, covers and lyrics-channel uploads.",
                    vm.officialOnly
                ) { vm.setOfficialOnlyOn(it) }
                KitSwitch(
                    "Hinglish lyrics", "Shows Hindi lyrics written in English letters.",
                    vm.hinglish
                ) { vm.setHinglishOn(it) }
                KitDivider()
                KitLabel("Lyrics size")
                KitChips(vm.lyricsSize, listOf("Small", "Medium", "Large")) { vm.updateLyricsSize(it) }
                KitLabel("Lyrics source", "Auto tries LRCLIB, then NetEase. You can also change it from the lyrics screen.")
                KitChips(vm.lyricsProvider, LyricsRepo.providerNames) { vm.chooseLyricsProvider(it) }
                KitLabel("Hindi songs translate to", "The translate button on the lyrics screen uses this when the song is in Hindi.")
                KitChips(vm.lyricsTransHi, LyricsRepo.langNames) { vm.chooseTransHi(it) }
                KitLabel("Other songs translate to", "Used for English and all other songs.")
                KitChips(vm.lyricsTransEn, LyricsRepo.langNames) { vm.chooseTransEn(it) }
                KitLabel("Lyrics alignment")
                KitChips(if (vm.lyricsLeft) 1 else 0, listOf("Centered", "Left")) { vm.setLyricsLeftOn(it == 1) }
                KitSwitch(
                    "Keep screen on in lyrics", "The screen stays awake while the lyrics are open.",
                    vm.keepScreenLyrics
                ) { vm.setKeepScreenLyricsOn(it) }
            }

            KitHeader(Icons.Filled.FileDownload, "Downloads")
            KitCard {
                KitAction(
                    "Open Downloads",
                    vm.downloads.size.toString() + " songs - " +
                        String.format("%.1f MB", vm.downloadBytes / 1048576.0)
                ) {
                    vm.showSettings = false
                    vm.downloadsOpen = true
                }
                KitDivider()
                KitLabel("Download quality", "Applies to new downloads.")
                KitChips(vm.downloadQuality, listOf("High", "Normal", "Data saver")) { vm.updateDownloadQuality(it) }
            }

            // ---------------- accounts and moving ----------------
            KitHeader(Icons.Filled.Cloud, "Accounts", "Optional. VAME works fully without them")
            KitCard {
                AccountSection(vm, false)
            }
            Spacer(Modifier.height(10.dp))
            KitCard {
                KitAction(
                    "Spotify",
                    "Listen to your Spotify inside VAME and bring your Spotify playlists into VAME."
                ) { SpotifyHub.open = true }
            }

            KitHeader(Icons.Filled.Storage, "Storage")
            KitCard {
                KitAction(
                    "Clear cache",
                    if (vm.cacheSize.isEmpty()) "Cached covers and song links"
                    else vm.cacheSize + " used by covers and song links"
                ) { vm.clearCache() }
                KitDivider()
                KitAction(
                    "Clear listening history", "Resets Last played and Your Type suggestions."
                ) { vm.clearHistory() }
            }

            KitHeader(Icons.Filled.Update, "Updates")
            KitCard {
                KitAction(
                    "Check for updates",
                    if (vm.updateStatus.isEmpty()) "Looks for a newer VAME on GitHub" else vm.updateStatus
                ) { vm.checkUpdate(true) }
                PlayProtectHelp()
            }

            KitHeader(Icons.Filled.Info, "About")
            KitCard {
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
                        "Coil, OkHttp and Kotlin (Apache 2.0). Lyrics from LRCLIB and NetEase, translation by Google Translate. " +
                        "VAME is not affiliated with YouTube, Spotify or Apple.",
                    fontSize = 12.sp, color = Pal.textDim
                )
            }
            // room so the mini player on top of Settings never covers the last row
            Spacer(Modifier.height(if (vm.current != null) 110.dp else 40.dp))
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
