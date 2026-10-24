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
import androidx.compose.animation.scaleOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.Dp
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
        UiOpts.load(applicationContext)
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
    val tablet = isTabletLandscape()
    val phoneLand = isPhoneLandscape()
    // Standby: a phone turned sideways while music plays opens the big landscape player by itself,
    // and closes it again when the phone is turned back (only if it was opened this way)
    LaunchedEffect(vm.showPlayer) {
        if (!vm.showPlayer) UiOpts.standbyByRotate = false
    }
    LaunchedEffect(phoneLand) {
        if (phoneLand) {
            val busy = vm.showSettings || vm.showOnboarding || vm.showLogin || vm.recapOpen ||
                SpotifyHub.open || DataMove.open
            if (UiOpts.standby && vm.current != null && vm.isPlaying && !vm.showPlayer &&
                !vm.showLyrics && !busy
            ) {
                vm.showPlayer = true
                if (UiOpts.standbyLyrics) vm.showLyrics = true
                UiOpts.standbyByRotate = true
            }
        } else if (UiOpts.standbyByRotate) {
            UiOpts.standbyByRotate = false
            vm.showLyrics = false
            vm.showPlayer = false
        }
    }
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
        // the colour of the playing cover tints the top of every page
        if (UiOpts.pageTint && vm.current != null) {
            val pageTint = rememberCoverTint(vm.current?.cover)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(pageTint.copy(alpha = if (Pal.amoled) 0.30f else 0.42f), Color.Transparent),
                        endY = 1100f
                    )
                )
            )
        }
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
                    // the panel grows out of the small box / pill and shrinks back into it
                    val origin = panelOrigin(vm)
                    PanelMorph(vm.showPlayer && vm.current != null, origin) {
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
                // the player opens out of the mini player pill and collapses back into it
                PlayerMorph(
                    visible = vm.showPlayer && vm.current != null,
                    cover = vm.current?.cover,
                    fly = !phoneLand
                ) {
                    if (phoneLand) StandbyPlayer(vm) else NowPlayingScreen(vm)
                }
                // lyrics grow from the bottom left (where the lyrics button is) and shrink back
                MorphLayer(
                    visible = vm.showLyrics && vm.current != null,
                    originX = 0.15f, originY = 0.95f, startScaleX = 0.3f, startScaleY = 0.2f
                ) {
                    // sideways phone: cover + title on the left, lyrics on the right
                    if (phoneLand) TabletLyricsScreen(vm) else LyricsScreen(vm)
                }
            }
            MorphLayer(
                visible = vm.showSettings,
                originX = 0.92f, originY = 0.04f, startScaleX = 0.25f, startScaleY = 0.25f
            ) {
                SettingsScreen(vm)
            }
            // the music stays on top of Settings (pill or box), so it can be stopped from there
            if (tablet) {
                TabletMini(vm)
            } else {
                AnimatedVisibility(
                    visible = vm.showSettings && vm.current != null,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(240)) { it } + fadeOut(tween(160))
                ) {
                    MiniPlayer(
                        vm,
                        Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                        force = true,
                        onOpen = {
                            vm.showSettings = false
                            vm.showPlayer = true
                        }
                    )
                }
            }
            MorphLayer(
                visible = vm.recapOpen,
                originX = 0.5f, originY = 0.5f, startScaleX = 0.8f, startScaleY = 0.8f
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
            MorphLayer(
                visible = vm.showLogin,
                originX = 0.5f, originY = 0.7f, startScaleX = 0.7f, startScaleY = 0.7f
            ) {
                LoginScreen(vm)
            }
            MorphLayer(
                visible = SpotifyHub.open,
                originX = 0.5f, originY = 0.6f, startScaleX = 0.8f, startScaleY = 0.8f
            ) {
                SpotifyScreen(vm)
            }
            MorphLayer(
                visible = DataMove.open,
                originX = 0.5f, originY = 0.6f, startScaleX = 0.8f, startScaleY = 0.8f
            ) {
                SwitchAppScreen(vm)
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
            if (!tablet) {
                Dock(vm, dockFolded, onExpand)
            }
        }
        AnimatedVisibility(
            visible = vm.playlistOpen,
            enter = detailIn(),
            exit = detailOut()
        ) {
            PlaylistScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.likedOpen,
            enter = detailIn(),
            exit = detailOut()
        ) {
            LikedScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.albumOpen,
            enter = detailIn(),
            exit = detailOut()
        ) {
            AlbumScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.downloadsOpen,
            enter = detailIn(),
            exit = detailOut()
        ) {
            DownloadsScreen(vm)
        }
        AnimatedVisibility(
            visible = vm.artistOpen,
            enter = detailIn(),
            exit = detailOut()
        ) {
            ArtistScreen(vm)
        }
    }
}

// Tablet: Now Playing (and the lyrics view) sit in a glass panel on the right,
// the pages stay usable on the left.
@Composable
private fun PlayerPanel(vm: MainViewModel) {
    val panelW = panelWidthDp(vm, LocalConfiguration.current.screenWidthDp)
    // full height, touches the top, right and bottom edge (the player draws its own insets)
    Box(
        Modifier
            .width(panelW)
            .fillMaxHeight()
    ) {
        NowPlayingScreen(vm)
    }
}

private fun panelWidthDp(vm: MainViewModel, w: Int): Dp =
    if (vm.panelWide) (w * 0.46f).dp.coerceIn(400.dp, 620.dp)
    else (w * 0.36f).dp.coerceIn(360.dp, 470.dp)

// Tablet: the point (inside the side panel) where the small box or pill sits,
// so the panel can grow out of it and shrink back into it.
@Composable
private fun panelOrigin(vm: MainViewModel): TransformOrigin {
    val c = LocalConfiguration.current
    val w = c.screenWidthDp.toFloat()
    val h = c.screenHeightDp.toFloat()
    val pw = panelWidthDp(vm, c.screenWidthDp).value
    val left = w - pw
    val cx: Float
    val cy: Float
    if (UiOpts.tabletPill) {
        cx = 92f + (w - 92f) / 2f
        cy = h - 48f
    } else {
        cx = if (vm.boxRight) w - 16f - 78f else 104f + 78f
        cy = h - vm.boxBottomDp - 102f
    }
    return TransformOrigin(((cx - left) / pw).coerceIn(0f, 1f), (cy / h).coerceIn(0f, 1f))
}

// detail pages (album, artist, playlist, liked, downloads) grow out of the card you tapped
private fun detailIn(): EnterTransition {
    if (UiOpts.reduceMotion) return fadeIn(tween(160))
    return fadeIn(tween(240)) + scaleIn(
        tween(400, easing = FastOutSlowInEasing), initialScale = 0.78f,
        transformOrigin = TransformOrigin(DetailOrigin.fx, DetailOrigin.fy)
    )
}

private fun detailOut(): ExitTransition {
    if (UiOpts.reduceMotion) return fadeOut(tween(140))
    return fadeOut(tween(200)) + scaleOut(
        tween(300, easing = FastOutSlowInEasing), targetScale = 0.86f,
        transformOrigin = TransformOrigin(DetailOrigin.fx, DetailOrigin.fy)
    )
}

// One smooth "grow out of a spot / shrink back into it" animation, used for the player,
// lyrics, settings, recap and sign-in. The layer scales from (originX, originY) with a soft
// spring, fades in while it grows, and its corners round off while it is small.
@Composable
private fun MorphLayer(
    visible: Boolean,
    originX: Float,
    originY: Float,
    startScaleX: Float,
    startScaleY: Float,
    content: @Composable () -> Unit
) {
    val morphSpec: androidx.compose.animation.core.AnimationSpec<Float> =
        if (UiOpts.reduceMotion) androidx.compose.animation.core.snap()
        else tween(if (visible) 560 else 480, easing = MorphEase)
    val p by animateFloatAsState(
        if (visible) 1f else 0f,
        morphSpec,
        label = "morph"
    )
    if (visible || p > 0.002f) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val q = p.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(originX, originY)
                    scaleX = startScaleX + (1f - startScaleX) * p
                    scaleY = startScaleY + (1f - startScaleY) * p
                    alpha = ((q - 0.12f) / 0.45f).coerceIn(0f, 1f)
                    shape = RoundedCornerShape(140.dp * (1f - q) * (1f - q))
                    clip = q < 0.999f
                    compositingStrategy =
                        if (q < 0.999f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                }
        ) {
            content()
        }
    }
}
