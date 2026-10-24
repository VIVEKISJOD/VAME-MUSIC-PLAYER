package com.ameos.music

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Looks at the newest GitHub Release of this repo. If it is newer than the installed app,
// the app offers to download it and install it over the old one.
object Updater {

    private const val RELEASE_API =
        "https://api.github.com/repos/VIVEKISJOD/VAME-MUSIC-PLAYER/releases/latest"

    data class Info(val version: String, val notes: String, val apkUrl: String)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun latest(): Info? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "VAME-music-app")
            .build()
        client.newCall(req).execute().use { r ->
            if (r.code == 404) return@withContext null // no release published yet
            if (!r.isSuccessful) throw IllegalStateException("GitHub answered " + r.code)
            val body = r.body?.string() ?: return@withContext null
            val o = JSONObject(body)
            val tag = o.optString("tag_name", "").removePrefix("v").trim()
            val assets = o.optJSONArray("assets") ?: return@withContext null
            var url = ""
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) {
                    url = a.optString("browser_download_url")
                    break
                }
            }
            if (tag.isEmpty() || url.isEmpty()) null else Info(tag, o.optString("body", ""), url)
        }
    }

    fun currentVersion(ctx: Context): String =
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"
        } catch (e: Exception) {
            "0"
        }

    // "0.10.0" is newer than "0.9.2"
    fun isNewer(latest: String, current: String): Boolean {
        val a = latest.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val b = current.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    // Android asks once for permission to install apps from VAME.
    // Returns true if we still have to wait for the person to allow it.
    fun needsInstallPermission(app: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && !app.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(
                app, "Allow VAME to install updates, then tap Update again", Toast.LENGTH_LONG
            ).show()
            try {
                app.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + app.packageName)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                Toast.makeText(app, "Open Settings > Apps > VAME > Install unknown apps", Toast.LENGTH_LONG).show()
            }
            return true
        }
        return false
    }

    // Downloads the APK inside the app with progress 0..100. If the connection stalls or drops,
    // it waits a moment and continues from where it stopped (up to 12 tries).
    suspend fun downloadApk(
        app: Context, info: Info, onProgress: (Int) -> Unit, onDetail: (String) -> Unit
    ): File =
        withContext(Dispatchers.IO) {
            val dir = File(app.cacheDir, "update")
            dir.mkdirs()
            // keep a half-finished file of this version so "Try again" continues where it stopped
            dir.listFiles()?.forEach { if (it.name != "VAME-" + info.version + ".apk") it.delete() }
            val file = File(dir, "VAME-" + info.version + ".apk")
            val dl = client.newBuilder()
                .protocols(listOf(Protocol.HTTP_1_1))
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            var finished = false
            var lastError: Exception? = null
            var tries = 0
            var last = -1
            while (!finished && tries < 12) {
                tries++
                try {
                    val have = if (file.exists()) file.length() else 0L
                    val rb = Request.Builder()
                        .url(info.apkUrl)
                        .header("User-Agent", "VAME-music-app")
                    if (have > 0) rb.header("Range", "bytes=" + have + "-")
                    dl.newCall(rb.build()).execute().use { r ->
                        if (r.code == 416 && have > 0) {
                            finished = true
                            return@use
                        }
                        if (r.code >= 500) throw java.io.IOException("GitHub answered " + r.code)
                        if (!r.isSuccessful) throw IllegalStateException("GitHub answered " + r.code)
                        val body = r.body ?: throw IllegalStateException("Empty download")
                        val resumed = r.code == 206 && have > 0
                        if (!resumed && have > 0) file.delete()
                        val start = if (resumed) have else 0L
                        val len = body.contentLength()
                        val cr = r.header("Content-Range")
                        var total = if (len > 0) start + len else -1L
                        if (cr != null && cr.contains("/")) {
                            total = cr.substringAfter("/").toLongOrNull() ?: total
                        }
                        body.byteStream().use { input ->
                            java.io.FileOutputStream(file, resumed).use { out ->
                                val buf = ByteArray(64 * 1024)
                                var done = start
                                var tickAt = System.currentTimeMillis()
                                var tickBytes = done
                                onDetail("Connected, starting...")
                                while (true) {
                                    ensureActive()
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    out.write(buf, 0, n)
                                    done += n
                                    val now = System.currentTimeMillis()
                                    if (now - tickAt >= 700) {
                                        val speed = (done - tickBytes) * 1000 / (now - tickAt) / 1024
                                        onDetail(mb(done) + (if (total > 0) " / " + mb(total) else "") + " MB  -  " + speed + " KB/s")
                                        tickAt = now
                                        tickBytes = done
                                    }
                                    if (total > 0) {
                                        val pct = (done * 100 / total).toInt()
                                        if (pct != last) {
                                            last = pct
                                            onProgress(pct)
                                        }
                                    }
                                }
                            }
                        }
                        if (total > 0 && file.length() < total) {
                            throw java.io.IOException("Connection dropped")
                        }
                        finished = true
                    }
                } catch (e: java.io.IOException) {
                    lastError = e
                    onDetail("Connection problem, trying again (" + tries + "/12)...")
                    delay(1500)
                }
            }
            if (!finished) throw lastError ?: IllegalStateException("Download failed")
            if (file.length() < 100_000L) throw IllegalStateException("Download incomplete")
            file
        }

    private fun mb(b: Long): String = String.format(java.util.Locale.US, "%.1f", b / 1048576.0)

    fun install(app: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(app, app.packageName + ".fileprovider", file)
            app.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: Exception) {
            Toast.makeText(app, "Could not open the installer", Toast.LENGTH_LONG).show()
        }
    }

    fun openInBrowser(c: Context, url: String) {
        try {
            c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(c, "Could not open the download link", Toast.LENGTH_SHORT).show()
        }
    }
}

// Play Protect warns about every app that is not from the Play Store. The code cannot hide that.
@Composable
fun PlayProtectHelp() {
    Text(
        "Play Protect says \"App scan recommended\" because very few phones have installed " +
            "this exact file yet (every new version is a new file). Tap Scan app, wait a few " +
            "seconds, then install. To stop it for good: Play Store > your profile picture > " +
            "Play Protect > gear icon > turn off Scan apps with Play Protect.",
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
fun UpdateDialog(vm: MainViewModel) {
    val info = vm.updateInfo ?: return
    val ctx = LocalContext.current
    val busy = vm.updateProgress >= 0
    AlertDialog(
        onDismissRequest = { if (!busy) vm.showUpdate = false },
        containerColor = Pal.surface,
        titleContentColor = Color.White,
        textContentColor = Pal.textDim,
        title = { Text("Update available: " + info.version) },
        text = {
            Column {
                Text("You have " + Updater.currentVersion(ctx) + ".", fontSize = 13.sp)
                // progress is always on top, so it is never hidden under the notes
                if (busy) {
                    Text(
                        if (vm.updateProgress > 0) "Downloading... " + vm.updateProgress + "%" else "Downloading...",
                        fontSize = 15.sp, color = Color.White,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    if (vm.updateProgress > 0) {
                        LinearProgressIndicator(
                            progress = { vm.updateProgress / 100f },
                            color = Pal.accentSoft,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        )
                    } else {
                        LinearProgressIndicator(
                            color = Pal.accentSoft,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        )
                    }
                    if (vm.updateDetail.isNotBlank()) {
                        Text(vm.updateDetail, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Text(
                        "Keep VAME open on screen until it finishes.",
                        fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (vm.updateError != null) {
                    Text(
                        vm.updateError ?: "",
                        fontSize = 13.sp, color = Color(0xFFFFB4AB),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                TextButton(
                    onClick = { Updater.openInBrowser(ctx, info.apkUrl) },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Download in browser instead", color = Pal.accentSoft, fontSize = 13.sp)
                }
                Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                    if (info.notes.isNotBlank()) {
                        Text(info.notes.trim().take(800), fontSize = 13.sp)
                    }
                    PlayProtectHelp()
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = { vm.startUpdate(ctx.applicationContext, info) }
            ) {
                Text(
                    if (busy) "Downloading..." else if (vm.updateError != null) "Try again" else "Update now",
                    color = Pal.accentSoft
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { if (busy) vm.cancelUpdate() else vm.showUpdate = false }) {
                Text(if (busy) "Cancel" else "Later", color = Pal.textDim)
            }
        }
    )
}
