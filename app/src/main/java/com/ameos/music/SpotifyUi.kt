package com.ameos.music

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog

private const val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

private fun openOutside(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
    }
}

@Composable
private fun SpRow(cover: String?, title: String, sub: String, actions: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(cover, Modifier.size(50.dp), RoundedCornerShape(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(sub, fontSize = 12.sp, color = Pal.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        actions()
    }
}

// Spotify page: connect, listen in the app (web player), bring playlists into VAME
@Composable
fun SpotifyScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext
    BackHandler { SpotifyHub.open = false }
    var idText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        SpotifyHub.refreshState(app)
        idText = SpotifyHub.clientId
        if (SpotifyHub.signedIn && SpotifyHub.playlists.isEmpty() && !SpotifyHub.busy) SpotifyHub.loadPlaylists(app)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Pal.bgTop, Pal.bgMid, Pal.bgBot), endY = 1500f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
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
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { SpotifyHub.open = false }) {
                    Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                }
                Column {
                    Text("Spotify", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Listen and bring playlists into VAME", fontSize = 13.sp, color = Pal.muted)
                }
            }

            if (SpotifyHub.busy || SpotifyHub.status.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                KitCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (SpotifyHub.busy) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Pal.accent, strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            SpotifyHub.status, fontSize = 14.sp, color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (SpotifyHub.busy) {
                        Spacer(Modifier.height(8.dp))
                        KitButton("Stop") { SpotifyHub.cancel() }
                    }
                }
            }

            if (!SpotifyHub.signedIn) {
                Spacer(Modifier.height(12.dp))
                KitCard {
                    Text("Connect Spotify (one time)", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    KitNote(
                        "Spotify only lets an app talk to your account if you make a free app for it. " +
                            "It takes 2 minutes:"
                    )
                    Text(
                        "1. Open developer.spotify.com/dashboard and log in.\n" +
                            "2. Create app. Name: VAME. Tick \"Web API\".\n" +
                            "3. Redirect URI: " + Spotify.REDIRECT + "\n" +
                            "4. Settings > User management: add the email of your Spotify account.\n" +
                            "5. Copy the Client ID and paste it below.",
                        fontSize = 13.sp, color = Color(0xFFDDDDDD), lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    KitButton("Open Spotify dashboard") { openOutside(ctx, "https://developer.spotify.com/dashboard") }
                    TextField(
                        value = idText,
                        onValueChange = { idText = it },
                        singleLine = true,
                        placeholder = { Text("Client ID", color = Pal.muted) },
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0x33FFFFFF),
                            unfocusedContainerColor = Color(0x33FFFFFF),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().imePadding()
                    )
                    Spacer(Modifier.height(10.dp))
                    KitButton("Save Client ID", enabled = idText.isNotBlank()) {
                        SpotifyHub.saveClientId(app, idText)
                        SpotifyHub.status = "Client ID saved"
                    }
                    KitButton("Connect Spotify", primary = true, enabled = SpotifyHub.clientId.isNotBlank()) {
                        SpotifyHub.showLogin = true
                    }
                    KitNote(
                        "Spotify rules: the app owner needs Spotify Premium, and only the accounts added in " +
                            "step 4 can connect. VAME never sees your password."
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))
                KitCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Cover(SpotifyHub.photo, Modifier.size(46.dp), CircleShape)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                SpotifyHub.name.ifBlank { "Spotify" }, fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text("Connected", fontSize = 12.sp, color = Pal.muted)
                        }
                        KitSmallBtn("Refresh", enabled = !SpotifyHub.busy) { SpotifyHub.loadPlaylists(app) }
                        Spacer(Modifier.width(8.dp))
                        KitSmallBtn("Disconnect") { SpotifyHub.disconnect(app) }
                    }
                    Spacer(Modifier.height(10.dp))
                    KitButton("Open Spotify inside VAME", primary = true) {
                        SpotifyHub.webUrl = "https://open.spotify.com/"
                    }
                    KitNote(
                        "The web player needs Spotify Premium. Your VAME music pauses while it is open. " +
                            "If it does not play on your phone, use the open-in-app button on its top bar."
                    )
                }

                Spacer(Modifier.height(8.dp))
                KitHeader(Icons.Filled.LibraryMusic, "Your Spotify playlists", "Listen here or bring into VAME")
                KitCard {
                    if (SpotifyHub.playlists.isEmpty()) {
                        Text("No playlists loaded yet. Tap Refresh.", fontSize = 13.sp, color = Pal.muted)
                    }
                    for (p in SpotifyHub.playlists) {
                        SpRow(p.cover, p.name, p.total.toString() + " songs" + (if (p.mine) "" else " - not yours")) {
                            KitSmallBtn("Listen") { SpotifyHub.webUrl = "https://open.spotify.com/playlist/" + p.id }
                            Spacer(Modifier.width(6.dp))
                            KitSmallBtn("To VAME", enabled = !SpotifyHub.busy && p.mine) {
                                SpotifyHub.importToVame(app, vm, p)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(48.dp))
        }

        if (SpotifyHub.showLogin) {
            BackHandler { SpotifyHub.showLogin = false }
            SpotifyLoginWeb(app)
        }
        val web = SpotifyHub.webUrl
        if (web != null) {
            BackHandler { SpotifyHub.webUrl = null }
            SpotifyWebPlayer(vm, web)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SpotifyLoginWeb(app: Context) {
    var progress by remember { mutableIntStateOf(0) }
    val holder = remember { arrayOfNulls<WebView>(1) }
    DisposableEffect(Unit) {
        onDispose {
            holder[0]?.stopLoading()
            holder[0]?.destroy()
            holder[0] = null
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { SpotifyHub.showLogin = false }) {
                    Icon(Icons.Filled.Close, null, tint = Color.White)
                }
                Text(
                    "Connect Spotify", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White, modifier = Modifier.weight(1f)
                )
            }
            if (progress in 1..99) {
                Box(Modifier.fillMaxWidth(progress / 100f).height(3.dp).background(Pal.accent))
            } else {
                Spacer(Modifier.height(3.dp))
            }
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { c ->
                    val web = WebView(c)
                    web.settings.javaScriptEnabled = true
                    web.settings.domStorageEnabled = true
                    web.settings.userAgentString = Account.UA
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(web, true)
                    var done = false
                    web.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val u = request?.url?.toString() ?: return false
                            if (u.startsWith(Spotify.REDIRECT)) {
                                if (!done) {
                                    done = true
                                    val uri = Uri.parse(u)
                                    val code = uri.getQueryParameter("code")
                                    if (code != null) {
                                        SpotifyHub.onCode(app, code)
                                    } else {
                                        SpotifyHub.onLoginError(uri.getQueryParameter("error") ?: "cancelled")
                                    }
                                }
                                return true
                            }
                            return false
                        }
                    }
                    web.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    holder[0] = web
                    web.loadUrl(Spotify.authUrl(app))
                    web
                }
            )
        }
    }
}

// Spotify's own web player inside VAME. Needs Premium; some phones cannot play its protected audio
// in a WebView, so there is a button that opens the Spotify app instead.
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SpotifyWebPlayer(vm: MainViewModel, url: String) {
    val ctx = LocalContext.current
    var progress by remember { mutableIntStateOf(0) }
    val holder = remember { arrayOfNulls<WebView>(1) }
    val curUrl by rememberUpdatedState(url)
    LaunchedEffect(url) { vm.player.pause() }
    DisposableEffect(Unit) {
        onDispose {
            holder[0]?.loadUrl("about:blank")
            holder[0]?.stopLoading()
            holder[0]?.destroy()
            holder[0] = null
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { SpotifyHub.webUrl = null }) {
                    Icon(Icons.Filled.Close, null, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("Spotify", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Log in with your Spotify account. Premium needed.", fontSize = 12.sp, color = Pal.muted)
                }
                IconButton(onClick = { openOutside(ctx, holder[0]?.url ?: curUrl) }) {
                    Icon(Icons.Filled.OpenInBrowser, null, tint = Color.White)
                }
            }
            if (progress in 1..99) {
                Box(Modifier.fillMaxWidth(progress / 100f).height(3.dp).background(Pal.accent))
            } else {
                Spacer(Modifier.height(3.dp))
            }
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { c ->
                    val web = WebView(c)
                    val st = web.settings
                    st.javaScriptEnabled = true
                    st.domStorageEnabled = true
                    st.mediaPlaybackRequiresUserGesture = false
                    st.userAgentString = DESKTOP_UA
                    st.useWideViewPort = true
                    st.loadWithOverviewMode = true
                    st.setSupportZoom(true)
                    st.builtInZoomControls = true
                    st.displayZoomControls = false
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(web, true)
                    web.webViewClient = WebViewClient()
                    web.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }

                        // lets the page play protected (DRM) music
                        override fun onPermissionRequest(request: PermissionRequest) {
                            val ok = request.resources
                                .filter { it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID }
                                .toTypedArray()
                            if (ok.isNotEmpty()) request.grant(ok) else request.deny()
                        }
                    }
                    holder[0] = web
                    web.loadUrl(url)
                    web
                }
            )
        }
    }
}
