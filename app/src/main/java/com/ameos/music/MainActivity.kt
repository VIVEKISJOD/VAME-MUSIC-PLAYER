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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.coerceIn
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
        Pal.load(applicationContext)
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
    if (vm.showUpdate && vm.updateInfo != null) UpdateDialog(vm)
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    // scroll down = bottom bar folds into one slim row, scroll up = opens again
    var dockFolded by remember { mutableStateOf(false) }
    LaunchedEffect(vm.tab) { dockFolded = false }
    val dockScroll = remember {
        object : NestedScrollConnection {
            // needs a real swipe in one direction before it folds / opens (no flicker)
            private var acc = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy == 0f) return Offset.Zero
                if ((dy < 0f) != (acc < 0f)) acc = 0f
                acc += dy
                if (acc < -90f) dockFolded = true
                else if (acc > 60f) dockFolded = false
                return Offset.Zero
            }
        }
    }
    // glass: while a sheet or dialog is open, the page behind it is blurred (Android 12+)
    val overlayOpen = vm.showMenu || vm.songMenu != null || vm.showSleepDialog ||
        vm.showOutputDialog || vm.addTarget != null || vm.showPlaylistDialog || vm.showImportDialog
    val blurAmount by animateDpAsState(if (overlayOpen) 16.dp else 0.dp, tween(260), label = "pageBlur")

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Pal.bgTop, Pal.bgMid, Pal.bgBot),
                    endY = 1500f
                )
            )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(if (blurAmount > 0.dp) Modifier.blur(blurAmount) else Modifier)
        ) {
            if (tablet) {
                Row(Modifier.fillMaxSize()) {
                    SideRail(vm)
                    Box(Modifier.weight(1f).fillMaxHeight().nestedScroll(dockScroll)) {
                        MainPages(vm, true, dockFolded) { dockFolded = false }
                    }
                    AnimatedVisibility(
                        visible = vm.showPlayer && vm.current != null,
                        enter = slideInHorizontally(tween(380)) { it } + fadeIn(tween(250)),
                        exit = slideOutHorizontally(tween(320)) { it } + fadeOut(tween(200))
                    ) {
                        PlayerPanel(vm)
                    }
                }
                // lyrics on a tablet: full screen, cover + name on the left, lyrics on the right
                AnimatedVisibility(
                    visible = vm.showLyrics && vm.current != null,
                    enter = fadeIn(tween(320)) + scaleIn(tween(360, easing = FastOutSlowInEasing), initialScale = 0.96f),
                    exit = fadeOut(tween(240))
                ) {
                    TabletLyricsScreen(vm)
                }
            } else {
                Box(Modifier.fillMaxSize().nestedScroll(dockScroll)) {
                    MainPages(vm, false, dockFolded) { dockFolded = false }
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
            }
            AnimatedVisibility(
                visible = vm.showSettings,
                enter = slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
                exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
            ) {
                SettingsScreen(vm)
            }
            AnimatedVisibility(
                visible = vm.recapOpen,
                enter = slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(320)) { it / 2 } + fadeOut(tween(240))
            ) {
                RecapScreen(vm)
            }
            AnimatedVisibility(
                visible = vm.showOnboarding,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                OnboardingScreen(vm)
            }
            AnimatedVisibility(
                visible = vm.showLogin,
                enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(260)),
                exit = slideOutVertically(tween(300)) { it / 2 } + fadeOut(tween(200))
            ) {
                LoginScreen(vm)
            }
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

// Pages (tabs + detail pages) and the bottom dock. Phone: mini player + tab bar that folds.
// Tablet: the tab bar lives in the side rail, so only a floating glass mini player is here.
@Composable
private fun MainPages(vm: MainViewModel, tablet: Boolean, dockFolded: Boolean, onExpand: () -> Unit) {
    // depth: the page underneath shrinks and dims a little while a detail page is open,
    // and eases back when it closes (so opening and Back both feel animated)
    val covered = vm.playlistOpen || vm.likedOpen || vm.albumOpen || vm.downloadsOpen || vm.artistOpen
    val depth by animateFloatAsState(
        if (covered) 1f else 0f, tween(380, easing = FastOutSlowInEasing), label = "pageDepth"
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .graphicsLayer {
                    val sc = 1f - 0.05f * depth
                    scaleX = sc
                    scaleY = sc
                    alpha = 1f - 0.4f * depth
                }
        ) {
            AnimatedContent(
                targetState = vm.tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(260, delayMillis = 70)) +
                        slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { dir * (it / 12) } +
                        scaleIn(tween(340, easing = FastOutSlowInEasing), initialScale = 0.95f)) togetherWith
                        (fadeOut(tween(140)) + slideOutHorizontally(tween(260)) { -dir * (it / 14) })
                },
                label = "tabs"
            ) { t ->
                when (t) {
                    0 -> HomeScreen(vm)
                    1 -> ExploreScreen(vm)
                    2 -> YourTypeScreen(vm)
                    else -> LibraryScreen(vm)
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(max = if (tablet) 640.dp else 2000.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val err = vm.error
            if (err != null) {
                Text(err, color = Color(0xFFFFB4AB), fontSize = 12.sp)
            }
            if (tablet) {
                AnimatedVisibility(
                    visible = !(vm.showPlayer && vm.current != null),
                    enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 2 },
                    exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 2 }
                ) {
                    MiniPlayer(vm)
                }
            } else {
                Dock(vm, dockFolded, onExpand)
            }
        }
        AnimatedVisibility(
            visible = vm.playlistOpen,
            enter = slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            PlaylistScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.likedOpen,
            enter = slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            LikedScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.albumOpen,
            enter = slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            AlbumScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.downloadsOpen,
            enter = slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            DownloadsScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.artistOpen,
            enter = slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
        ) {
            ArtistScreen(vm)
        }
    }
}

// Tablet: Now Playing (and the lyrics view) sit in a glass panel on the right,
// the pages stay usable on the left.
@Composable
private fun PlayerPanel(vm: MainViewModel) {
    val w = LocalConfiguration.current.screenWidthDp
    val panelW = if (vm.panelWide) (w * 0.46f).dp.coerceIn(400.dp, 620.dp)
    else (w * 0.36f).dp.coerceIn(360.dp, 470.dp)
    // full height, touches the top, right and bottom edge (the player draws its own insets)
    Box(
        Modifier
            .width(panelW)
            .fillMaxHeight()
    ) {
        NowPlayingScreen(vm)
    }
}
