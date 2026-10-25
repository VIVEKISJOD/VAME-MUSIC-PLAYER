package com.ameos.music

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.widget.RemoteViews
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// 4x1 home screen widget: cover, song, previous / play / next
class WideWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.refresh(context)
    }
}

// 2x2 home screen widget: big cover with the song name on it
class SquareWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.refresh(context)
    }
}

// buttons of the widgets
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WidgetUpdater.ACT_TOGGLE -> {
                val t = PlaybackBridge.onToggle
                if (t != null) {
                    t.invoke()
                } else {
                    val p = PlayerHolder.get(context.applicationContext)
                    if (p.mediaItemCount > 0) {
                        if (p.isPlaying) p.pause() else p.play()
                    }
                }
            }
            WidgetUpdater.ACT_NEXT -> PlaybackBridge.onNext?.invoke()
            WidgetUpdater.ACT_PREV -> PlaybackBridge.onPrev?.invoke()
        }
    }
}

object WidgetUpdater {
    const val ACT_TOGGLE = "com.ameos.music.WIDGET_TOGGLE"
    const val ACT_NEXT = "com.ameos.music.WIDGET_NEXT"
    const val ACT_PREV = "com.ameos.music.WIDGET_PREV"

    private fun artFile(ctx: Context) = File(ctx.cacheDir, "widget_cover.png")

    // save the cover of the current song (small) so the widgets can draw it
    suspend fun saveArt(ctx: Context, url: String?) {
        withContext(Dispatchers.IO) {
            val f = artFile(ctx)
            if (url.isNullOrBlank()) {
                f.delete()
                return@withContext
            }
            try {
                val req = ImageRequest.Builder(ctx).data(url).size(360).allowHardware(false).build()
                val d = ctx.imageLoader.execute(req).drawable
                val bmp = (d as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun clearArt(ctx: Context) {
        artFile(ctx).delete()
    }

    // square, rounded corners, optional dark fade at the bottom (so the text can be read)
    private fun bake(src: Bitmap?, size: Int, radiusFrac: Float, fade: Boolean): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = size * radiusFrac
        val box = RectF(0f, 0f, size.toFloat(), size.toFloat())
        c.drawRoundRect(box, r, r, p)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        if (src != null) {
            val s = minOf(src.width, src.height)
            val sx = (src.width - s) / 2
            val sy = (src.height - s) / 2
            c.drawBitmap(src, Rect(sx, sy, sx + s, sy + s), Rect(0, 0, size, size), p)
        } else {
            p.color = 0xFF3A2A33.toInt()
            c.drawRect(box, p)
        }
        if (fade) {
            val g = Paint(Paint.ANTI_ALIAS_FLAG)
            g.shader = LinearGradient(
                0f, size * 0.40f, 0f, size.toFloat(),
                0x00000000, 0xD9000000.toInt(), Shader.TileMode.CLAMP
            )
            g.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            c.drawRect(box, g)
        }
        return out
    }

    private fun act(ctx: Context, action: String, code: Int): PendingIntent {
        val i = Intent(ctx, WidgetActionReceiver::class.java)
        i.action = action
        return PendingIntent.getBroadcast(
            ctx, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun openApp(ctx: Context): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
        i.putExtra("open_player", true)
        i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            ctx, 90, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // redraw every widget that is on the home screen
    fun refresh(ctx: Context) {
        try {
            val app = ctx.applicationContext
            val mgr = AppWidgetManager.getInstance(app)
            val sp = app.getSharedPreferences("vame_widget", Context.MODE_PRIVATE)
            val title = sp.getString("t", null)
            val artist = sp.getString("a", "") ?: ""
            val playing = sp.getBoolean("p", false)
            val f = artFile(app)
            val art: Bitmap? = if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            val playIcon = if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play

            val wide = mgr.getAppWidgetIds(ComponentName(app, WideWidget::class.java))
            if (wide.isNotEmpty()) {
                val v = RemoteViews(app.packageName, R.layout.widget_wide)
                v.setTextViewText(R.id.w_title, title ?: "Not playing")
                v.setTextViewText(R.id.w_artist, if (title == null) "Tap to open VAME" else artist)
                v.setImageViewBitmap(R.id.w_cover, bake(art, 180, 0.2f, false))
                v.setImageViewResource(R.id.w_play, playIcon)
                v.setOnClickPendingIntent(R.id.w_root, openApp(app))
                v.setOnClickPendingIntent(R.id.w_prev, act(app, ACT_PREV, 11))
                v.setOnClickPendingIntent(R.id.w_play, act(app, ACT_TOGGLE, 12))
                v.setOnClickPendingIntent(R.id.w_next, act(app, ACT_NEXT, 13))
                mgr.updateAppWidget(wide, v)
            }

            val sq = mgr.getAppWidgetIds(ComponentName(app, SquareWidget::class.java))
            if (sq.isNotEmpty()) {
                val v = RemoteViews(app.packageName, R.layout.widget_square)
                v.setTextViewText(R.id.w_title, title ?: "Not playing")
                v.setTextViewText(R.id.w_artist, if (title == null) "Tap to open VAME" else artist)
                v.setImageViewBitmap(R.id.w_cover, bake(art, 320, 0.14f, true))
                v.setImageViewResource(R.id.w_play, playIcon)
                v.setOnClickPendingIntent(R.id.w_root, openApp(app))
                v.setOnClickPendingIntent(R.id.w_play, act(app, ACT_TOGGLE, 14))
                mgr.updateAppWidget(sq, v)
            }
        } catch (e: Exception) {
        }
    }
}
