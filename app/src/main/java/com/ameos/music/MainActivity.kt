package com.ameos.music

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            vm.refreshOutputs()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        val need = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33) need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 31) need.add(Manifest.permission.BLUETOOTH_CONNECT)
        if (need.isNotEmpty()) permLauncher.launch(need.toTypedArray())
        setContent {
            AmeosTheme {
                AmeosRoot(vm)
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // tapping the notification / lock screen / island opens the app on Now Playing
    private fun handleIntent(i: Intent?) {
        if (i != null && i.getBooleanExtra("open_player", false)) {
            i.removeExtra("open_player")
            if (vm.current != null) vm.showPlayer = true
        }
    }
}

@Composable
fun AmeosRoot(vm: MainViewModel) {
    // on Search / Your Type / Library, Back goes to Home (Home itself leaves the app).
    // This is registered first, so open pages and sheets still close before it.
    BackHandler(enabled = vm.tab != 0) { vm.tab = 0 }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3A1511), Color(0xFF0E0B0B), Color(0xFF0A0808)),
                    endY = 1500f
                )
            )
    ) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            Crossfade(targetState = vm.tab, animationSpec = tween(260), label = "tabs") { t ->
                when (t) {
                    0 -> HomeScreen(vm)
                    1 -> SearchScreen(vm)
                    2 -> YourTypeScreen(vm)
                    else -> LibraryScreen(vm)
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val err = vm.error
            if (err != null) {
                Text(err, color = Color(0xFFFFB4AB), fontSize = 12.sp)
            }
            MiniPlayer(vm)
            BottomBar(vm.tab, { vm.tab = it })
        }
        AnimatedVisibility(
            visible = vm.playlistOpen,
            enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            PlaylistScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.likedOpen,
            enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            LikedScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.albumOpen,
            enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            AlbumScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.downloadsOpen,
            enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            DownloadsScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.artistOpen,
            enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            ArtistScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.showPlayer && vm.current != null,
            enter = slideInVertically(tween(380)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(320)) { it } + fadeOut(tween(200))
        ) {
            NowPlayingScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.showLyrics && vm.current != null,
            enter = slideInVertically(tween(380)) { it / 3 } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(300)) { it / 3 } + fadeOut(tween(220))
        ) {
            LyricsScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.showSettings,
            enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            SettingsScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.showOnboarding,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            OnboardingScreen(vm)
        }
        QuickSheet(vm)
        SongSheet(vm)
        if (vm.showPlaylistDialog) {
            PlaylistEditDialog(vm)
        }
        if (vm.addTarget != null) {
            AddToPlaylistDialog(vm)
        }
        if (vm.showSleepDialog) {
            SleepDialog(vm)
        }
        if (vm.showOutputDialog) {
            OutputDialog(vm)
        }
        if (vm.showImportDialog) {
            ImportDialog(vm)
        }
    }
}
