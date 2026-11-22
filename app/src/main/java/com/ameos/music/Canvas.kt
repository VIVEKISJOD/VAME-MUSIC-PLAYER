package com.ameos.music

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// step73: animated player canvas.
// Level 1 (default): the cover moves slowly (zoom, drift, light sweep) or colour waves drift behind it.
// Level 3 (optional): the bass makes the canvas pulse a little (Android Visualizer, needs the microphone permission).
// Level 2 (short video loop) is NOT built yet.
object CanvasPrefs {
    var on by mutableStateOf(true)
    var mode by mutableIntStateOf(0)        // 0 cover motion, 1 colour waves
    var videoWifi by mutableIntStateOf(0)   // kept for the video level later (0 Wi-Fi only, 1 always)
    var pulse by mutableStateOf(false)

    fun load(c: Context) {
        val sp = c.getSharedPreferences("vame", Context.MODE_PRIVATE)
        on = sp.getBoolean("canvasOn", true)
        mode = sp.getInt("canvasMode", 0).coerceIn(0, 1)
        videoWifi = sp.getInt("canvasVideoWifi", 0).coerceIn(0, 1)
        // the pulse needs the microphone permission: never on without it
        pulse = sp.getBoolean("canvasPulse", false) &&
            c.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun save(c: Context) {
        c.getSharedPreferences("vame", Context.MODE_PRIVATE).edit()
            .putBoolean("canvasOn", on).putInt("canvasMode", mode)
            .putInt("canvasVideoWifi", videoWifi).putBoolean("canvasPulse", pulse)
            .apply()
    }
}

// low-frequency energy of the song, 0..1 (Level 3)
object CanvasPulse {
    var level by mutableFloatStateOf(0f)
    private var viz: Visualizer? = null

    fun start(session: Int) {
        stop()
        try {
            val v = Visualizer(session)
            v.captureSize = Visualizer.getCaptureSizeRange()[0]
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                }

                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft == null || fft.size < 10) return
                    var e = 0f
                    var i = 2
                    while (i < 10) {
                        val re = fft[i].toInt()
                        val im = fft[i + 1].toInt()
                        e += sqrt((re * re + im * im).toFloat())
                        i += 2
                    }
                    val target = (e / 4f / 110f).coerceIn(0f, 1f)
                    level = level * 0.55f + target * 0.45f
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true)
            v.enabled = true
            viz = v
        } catch (t: Throwable) {
            viz = null
        }
    }

    fun stop() {
        try {
            viz?.enabled = false
            viz?.release()
        } catch (t: Throwable) {
        }
        viz = null
        level = 0f
    }
}

// four colours that fit the cover colour
fun canvasColors(tint: Color): List<Color> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(tint.toArgb(), hsv)
    val s = hsv[1].coerceIn(0.45f, 0.9f)
    fun c(dh: Float, v: Float): Color = Color.hsv(((hsv[0] + dh) % 360f + 360f) % 360f, s, v)
    return listOf(c(0f, 0.90f), c(28f, 0.85f), c(-32f, 0.80f), c(70f, 0.75f))
}

// one slow clock for the canvas; it only runs while a song plays and the canvas is on
@Composable
private fun rememberCanvasPhase(playing: Boolean): Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val phase = remember { Animatable(0f) }
    val live = CanvasPrefs.on && !UiOpts.reduceMotion && playing
    LaunchedEffect(live) {
        if (live) {
            while (true) {
                phase.animateTo(phase.value + 1f, tween(24000, easing = LinearEasing))
            }
        }
    }
    return phase
}

// the artwork: slow zoom + drift + a light sweep (frozen while paused)
@Composable
fun PlayerCanvas(cover: String?, playing: Boolean, modifier: Modifier = Modifier) {
    val phase = rememberCanvasPhase(playing)
    val animated = CanvasPrefs.on && !UiOpts.reduceMotion
    Box(modifier) {
        Cover(
            cover,
            Modifier.fillMaxSize().graphicsLayer {
                if (animated) {
                    val p = phase.value * 6.2831855f
                    val k = 1.10f + 0.05f * sin(p) + CanvasPulse.level * 0.03f
                    scaleX = k
                    scaleY = k
                    translationX = cos(p) * size.width * 0.02f
                    translationY = sin(p * 0.7f) * size.height * 0.02f
                }
            },
            RoundedCornerShape(0.dp),
            hiRes = true
        )
        if (animated) {
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val t = (phase.value * 2f) % 1f
                    val x = (t * 2.4f - 0.7f) * size.width
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color(0x26FFFFFF), Color.Transparent),
                            start = Offset(x, 0f),
                            end = Offset(x + size.width * 0.5f, size.height * 0.4f)
                        )
                    )
                }
            )
        }
    }
}

// colour waves: four soft colour blobs drift on a dark base
@Composable
fun CanvasBackdrop(tint: Color, playing: Boolean, modifier: Modifier = Modifier) {
    val phase = rememberCanvasPhase(playing)
    val cols = canvasColors(tint)
    Box(
        modifier.drawBehind {
            drawRect(Color(0xFF09090B))
            val p = phase.value * 6.2831855f
            cols.forEachIndexed { i, c ->
                val a = p * (0.6f + 0.25f * i) + i * 1.7f
                val cx = size.width * (0.5f + 0.38f * cos(a))
                val cy = size.height * (0.5f + 0.38f * sin(a * 1.1f))
                val r = size.maxDimension * (0.55f + CanvasPulse.level * 0.1f)
                drawCircle(
                    Brush.radialGradient(
                        listOf(c.copy(alpha = 0.70f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }
    )
}

// Settings > Player > Canvas
@Composable
fun CanvasSection() {
    val ctx = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        CanvasPrefs.pulse = ok
        CanvasPrefs.save(ctx)
        if (!ok) Toast.makeText(ctx, "Audio pulse needs the microphone permission", Toast.LENGTH_LONG).show()
    }
    KitSwitch("Canvas", "Moving artwork on the player: the cover zooms and drifts slowly while a song plays.", CanvasPrefs.on) {
        CanvasPrefs.on = it
        CanvasPrefs.save(ctx)
    }
    KitDivider()
    KitLabel("Canvas style")
    KitChips(CanvasPrefs.mode, listOf("Cover motion", "Colour waves")) {
        CanvasPrefs.mode = it
        CanvasPrefs.save(ctx)
    }
    KitDivider()
    KitSwitch(
        "Audio pulse",
        "The canvas beats a little with the bass. Android asks for the microphone permission for this; VAME never records anything.",
        CanvasPrefs.pulse
    ) { v ->
        if (!v) {
            CanvasPrefs.pulse = false
            CanvasPrefs.save(ctx)
        } else if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            CanvasPrefs.pulse = true
            CanvasPrefs.save(ctx)
        } else {
            ask.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    KitNote("YouTube has no official canvas videos or animated covers, so VAME makes the motion itself from the cover. It stops while paused and with Calmer animations on.")
}
