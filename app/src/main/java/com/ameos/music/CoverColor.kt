package com.ameos.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Finds a nice dark main colour of a cover so Now Playing and Lyrics can match the song.
object CoverColor {
    private val cache = HashMap<String, Int>()
    val DEFAULT = Color(0xFF7A1F22)

    suspend fun get(ctx: Context, url: String): Int? {
        val hit = cache[url]
        if (hit != null) return hit
        val req = ImageRequest.Builder(ctx).data(url).allowHardware(false).size(96).build()
        val res = ctx.imageLoader.execute(req)
        val bmp = ((res as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return null
        val c = withContext(Dispatchers.Default) { dominant(bmp) }
        cache[url] = c
        return c
    }

    private fun dominant(src: Bitmap): Int {
        val n = 24
        val b = Bitmap.createScaledBitmap(src, n, n, true)
        var r = 0.0
        var g = 0.0
        var bl = 0.0
        var sum = 0.0
        val hsv = FloatArray(3)
        for (y in 0 until n) {
            for (x in 0 until n) {
                val p = b.getPixel(x, y)
                AColor.colorToHSV(p, hsv)
                val w = (hsv[1] * hsv[2]).toDouble() + 0.02
                r += AColor.red(p) * w
                g += AColor.green(p) * w
                bl += AColor.blue(p) * w
                sum += w
            }
        }
        val avg = AColor.rgb((r / sum).toInt(), (g / sum).toInt(), (bl / sum).toInt())
        AColor.colorToHSV(avg, hsv)
        hsv[1] = (hsv[1] * 1.1f).coerceAtMost(0.85f)
        hsv[2] = hsv[2].coerceIn(0.28f, 0.50f)
        return AColor.HSVToColor(hsv)
    }
}

@Composable
fun rememberCoverTint(url: String?): Color {
    val ctx = LocalContext.current
    var target by remember { mutableStateOf(CoverColor.DEFAULT) }
    LaunchedEffect(url) {
        if (url != null) {
            val v = try {
                CoverColor.get(ctx, url)
            } catch (e: Exception) {
                null
            }
            if (v != null) target = Color(v)
        }
    }
    val animated by animateColorAsState(target, tween(700), label = "tint")
    return animated
}
