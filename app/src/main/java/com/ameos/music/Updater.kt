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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
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

    fun download(ctx: Context, info: Info) {
        val app = ctx.applicationContext
        // Android asks once for permission to install apps from VAME
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
                openInBrowser(app, info.apkUrl)
            }
            return
        }
        try {
            val dm = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(info.apkUrl))
                .setTitle("VAME " + info.version)
                .setDescription("Downloading update")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                    app, Environment.DIRECTORY_DOWNLOADS, "VAME-" + info.version + ".apk"
                )
            val id = dm.enqueue(req)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) {
                    if (i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != id) return
                    try {
                        c.unregisterReceiver(this)
                    } catch (e: Exception) {
                    }
                    install(c, dm, id, info)
                }
            }
            ContextCompat.registerReceiver(
                app, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
            Toast.makeText(app, "Downloading VAME " + info.version + "...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            openInBrowser(app, info.apkUrl)
        }
    }

    private fun install(c: Context, dm: DownloadManager, id: Long, info: Info) {
        try {
            val uri = dm.getUriForDownloadedFile(id)
                ?: throw IllegalStateException("download missing")
            c.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: Exception) {
            Toast.makeText(c, "Open the download notification to install", Toast.LENGTH_LONG).show()
        }
    }

    private fun openInBrowser(c: Context, url: String) {
        try {
            c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(c, "Could not open the download link", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun UpdateDialog(vm: MainViewModel) {
    val info = vm.updateInfo ?: return
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = { vm.showUpdate = false },
        containerColor = Color(0xFF2A1215),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFD9C0C2),
        title = { Text("Update available: " + info.version) },
        text = {
            Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "You have " + Updater.currentVersion(ctx) + ".",
                    fontSize = 13.sp
                )
                if (info.notes.isNotBlank()) {
                    Text(info.notes.trim().take(800), fontSize = 13.sp, modifier = Modifier)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.showUpdate = false
                Updater.download(ctx, info)
            }) { Text("Update now", color = Color(0xFFF6C6D0)) }
        },
        dismissButton = {
            TextButton(onClick = { vm.showUpdate = false }) { Text("Later", color = Color(0xFFD9C0C2)) }
        }
    )
}
