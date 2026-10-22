package com.ameos.music

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

// Offline songs: audio files live in the app's private storage (filesDir/downloads).
object Downloads {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    private fun dir(c: Context): File {
        val d = File(c.filesDir, "downloads")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun nameOf(song: Song): String {
        val md = MessageDigest.getInstance("MD5").digest(song.url.toByteArray())
        return md.joinToString("") { b -> String.format("%02x", b) } + ".audio"
    }

    // the finished file for a song, or null if it is not downloaded
    fun file(c: Context, song: Song): File? {
        val f = File(dir(c), nameOf(song))
        return if (f.exists() && f.length() > 0L) f else null
    }

    fun delete(c: Context, song: Song) {
        File(dir(c), nameOf(song)).delete()
        File(dir(c), nameOf(song) + ".part").delete()
    }

    fun totalBytes(c: Context): Long =
        dir(c).listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    // downloads in 1 MB pieces (YouTube slows down one big request)
    suspend fun fetch(
        c: Context,
        song: Song,
        audioUrl: String,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val target = File(dir(c), nameOf(song))
        val part = File(dir(c), nameOf(song) + ".part")
        try {
            part.delete()
            val chunk = 1000000L
            var start = 0L
            var total = -1L
            part.outputStream().use { out ->
                while (total < 0L || start < total) {
                    val req = Request.Builder()
                        .url(audioUrl)
                        .header("User-Agent", DownloaderImpl.USER_AGENT)
                        .header("Range", "bytes=" + start + "-" + (start + chunk - 1))
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) throw IllegalStateException("HTTP " + resp.code)
                        val body = resp.body ?: throw IllegalStateException("empty body")
                        val bytes = body.bytes()
                        if (bytes.isEmpty()) throw IllegalStateException("empty chunk")
                        out.write(bytes)
                        start += bytes.size
                        val cr = resp.header("Content-Range")
                        if (cr != null) {
                            val t = cr.substringAfter('/', "").toLongOrNull()
                            if (t != null) total = t
                        }
                        if (resp.code == 200 || bytes.size < chunk) {
                            // server sent everything / this was the last piece
                            if (total < 0L || resp.code == 200) total = start
                        }
                    }
                    if (total > 0L) onProgress((start.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                }
            }
            if (target.exists()) target.delete()
            part.renameTo(target)
        } catch (e: CancellationException) {
            part.delete()
            throw e
        } catch (e: Exception) {
            part.delete()
            false
        }
    }
}
