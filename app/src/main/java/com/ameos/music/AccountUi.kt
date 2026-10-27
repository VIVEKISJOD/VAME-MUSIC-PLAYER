package com.ameos.music

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

// TV: shows the on-screen keyboard for the sign-in web page (the page itself does not ask for it
// when it is opened with a remote).
private fun showKeyboard(web: WebView) {
    try {
        web.isFocusable = true
        web.isFocusableInTouchMode = true
        web.requestFocus()
        val imm = web.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
        // NOT forced: a forced keyboard cannot be closed with the remote's Back key
        imm.showSoftInput(web, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    } catch (e: Exception) {
    }
}

private fun hideKeyboard(web: WebView) {
    try {
        val imm = web.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(web.windowToken, 0)
    } catch (e: Exception) {
    }
}

// presses the page's Next / Sign in button (or Enter on the field) and closes the keyboard
private const val NEXT_JS = "(function(){" +
    "var b=document.querySelector('#identifierNext button,#passwordNext button,#totpNext button')" +
    "||document.querySelector('button[type=submit]');" +
    "if(b){b.click();return;}" +
    "var a=document.activeElement;if(a){" +
    "a.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',keyCode:13,which:13,bubbles:true}));" +
    "a.dispatchEvent(new KeyboardEvent('keyup',{key:'Enter',keyCode:13,which:13,bubbles:true}));" +
    "var f=a.form;if(f&&f.requestSubmit){f.requestSubmit();}}" +
    "})();"

// the page calls VameKb.show() whenever a text field gets the cursor
private class KbBridge(private val web: WebView) {
    @android.webkit.JavascriptInterface
    fun show() {
        web.post { showKeyboard(web) }
    }
}

private const val KB_JS = "(function(){" +
    "if(window.__vameKb){return;}window.__vameKb=true;" +
    "document.addEventListener('focusin',function(e){var t=e.target;" +
    "if(t&&(t.tagName==='INPUT'||t.tagName==='TEXTAREA')){VameKb.show();}},true);" +
    "var el=document.querySelector('input[type=email],input[type=text],input[type=password],input[type=tel]');" +
    "if(el){el.focus();}" +
    "})();"

// Full screen Google sign-in inside the app (a normal web page, like Metrolist / InnerTune).
// When music.youtube.com opens and the login cookie exists, we keep it and close this screen.
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(vm: MainViewModel) {
    BackHandler { vm.showLogin = false }
    var progress by remember { mutableIntStateOf(0) }
    val holder = remember { arrayOfNulls<WebView>(1) }
    val firstFocus = remember { FocusRequester() }
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
            .blockTaps()
            .tvPageFocus(firstFocus)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackBtn(Modifier.focusRequester(firstFocus), Icons.Filled.Close) { vm.showLogin = false }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        "Sign in with Google", fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, color = Color.White
                    )
                    Text(
                        "Optional. Only used to show your YouTube Music library.",
                        fontSize = 12.sp, color = Pal.textDim,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (UiOpts.tv) {
                    // TV: a button that opens the on-screen keyboard (it also opens by itself on a text field)
                    Row(
                        Modifier
                            .padClick(RoundedCornerShape(50), Color(0x33FFFFFF), 1.05f) {
                                holder[0]?.let { showKeyboard(it) }
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Keyboard, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Keyboard", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(
                        Modifier
                            .padClick(RoundedCornerShape(50), Color(0x33FFFFFF), 1.05f) {
                                holder[0]?.let { hideKeyboard(it) }
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hide keyboard", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(
                        Modifier
                            .padClick(RoundedCornerShape(50), Pal.accent, 1.05f) {
                                holder[0]?.let {
                                    it.evaluateJavascript(NEXT_JS, null)
                                    hideKeyboard(it)
                                }
                            }
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Next", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Pal.bgTop)
                    }
                }
            }
            if (progress in 1..99) {
                Box(
                    Modifier
                        .fillMaxWidth(progress / 100f)
                        .height(3.dp)
                        .background(Pal.accent)
                )
            } else {
                Spacer(Modifier.height(3.dp))
            }
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { c ->
                    val web = WebView(c)
                    web.isFocusable = true
                    web.isFocusableInTouchMode = true
                    web.addJavascriptInterface(KbBridge(web), "VameKb")
                    web.settings.javaScriptEnabled = true
                    web.settings.domStorageEnabled = true
                    web.settings.userAgentString = Account.UA
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(web, true)
                    var finished = false
                    web.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (UiOpts.tv && view != null) {
                                // TV: the cursor goes to the first text field and the keyboard opens by itself
                                view.requestFocus()
                                view.evaluateJavascript(KB_JS, null)
                            }
                            if (finished || url == null) return
                            if (url.startsWith("https://music.youtube.com")) {
                                val ck = CookieManager.getInstance()
                                    .getCookie("https://music.youtube.com") ?: ""
                                if (ck.contains("SAPISID")) {
                                    finished = true
                                    CookieManager.getInstance().flush()
                                    vm.onLoginDone(ck)
                                }
                            }
                        }
                    }
                    web.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    holder[0] = web
                    web.loadUrl(Account.LOGIN_URL)
                    web
                }
            )
        }
    }
}

@Composable
private fun AccButton(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .glassFill()
            .padRing(RoundedCornerShape(50)) { onClick() }
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
    Spacer(Modifier.height(8.dp))
}

// Settings > Account (optional, never part of onboarding)
@Composable
fun AccountSection(vm: MainViewModel, label: Boolean = true) {
    if (label) SectionLabel("Account (optional)")
    if (!vm.ytSignedIn) {
        Text(
            "Sign in with Google to see your YouTube Music playlists and liked songs inside " +
                "VAME. You can skip this: VAME works fully without an account.",
            fontSize = 12.sp, color = Pal.muted, modifier = Modifier.padding(bottom = 10.dp)
        )
        AccButton("Sign in with Google") { vm.showLogin = true }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cover(vm.ytPhoto, Modifier.size(46.dp), CircleShape)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (vm.ytName.isBlank()) "Google account" else vm.ytName,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (vm.ytStatus.isBlank()) "Signed in" else vm.ytStatus,
                    fontSize = 12.sp, color = Pal.muted
                )
            }
        }
        AccButton("Refresh YouTube library") { vm.refreshYt() }
        AccButton("Copy YouTube likes to my Liked songs") { vm.importYtLikes() }
        AccButton("Save new playlists to YouTube Music: " + (if (vm.ytSync) "ON" else "OFF")) { vm.toggleYtSync() }
        AccButton("Upload my VAME playlists to YouTube Music") { vm.ytPushAll() }
        AccButton("Sign out") { vm.signOutYt() }
    }
    Text(
        "Unofficial: VAME talks to YouTube Music the same way the website does, so it " +
            "can stop working when YouTube changes something. It reads your library and, only " +
            "if the switch above is ON, saves the playlists you make in VAME as private " +
            "playlists on your account (create, add songs, rename, delete those only). It never " +
            "touches your other playlists or likes. Your login stays on this phone. " +
            "Songs still play without ads through NewPipe.",
        fontSize = 12.sp, color = Pal.muted, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
    )
}
