package com.ameos.music

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

// step59b: equalizer, bass boost, loudness, 3D (spatial) sound and fade between songs.
// The effects use Android's own audio effects on the player's audio session, so they cost almost no battery.
object SoundFx {
    val presetNames = listOf("Flat", "Bass", "Vocal", "Rock", "Pop", "Treble", "Custom")
    private val presets = listOf(
        floatArrayOf(0f, 0f, 0f, 0f, 0f),
        floatArrayOf(0.8f, 0.5f, 0f, 0f, 0f),
        floatArrayOf(-0.3f, 0f, 0.4f, 0.5f, 0.2f),
        floatArrayOf(0.5f, 0.3f, -0.2f, 0.3f, 0.6f),
        floatArrayOf(-0.2f, 0.3f, 0.5f, 0.3f, -0.2f),
        floatArrayOf(0f, 0f, 0f, 0.5f, 0.8f)
    )
    val crossfadeSeconds = listOf(0, 3, 6, 9)

    var eqOn by mutableStateOf(false)
    var preset by mutableIntStateOf(0)
    val levels = mutableStateListOf(0f, 0f, 0f, 0f, 0f) // -1 .. 1 for every band
    var bandHz by mutableStateOf(listOf(60, 230, 910, 3600, 14000))
    var bass by mutableIntStateOf(0)       // 0 .. 1000
    var loud by mutableIntStateOf(0)       // 0 .. 1000 (milli-dB gain)
    var spatial by mutableIntStateOf(0)    // 0 .. 1000
    var crossfade by mutableIntStateOf(0)  // seconds, 0 = off
    var note by mutableStateOf("")

    private var ctx: Context? = null
    private var eq: Equalizer? = null
    private var bb: BassBoost? = null
    private var le: LoudnessEnhancer? = null
    private var vi: Virtualizer? = null
    private var fading = false

    private fun prefs() = ctx?.getSharedPreferences("vame_sound", Context.MODE_PRIVATE)

    fun init(c: Context, player: ExoPlayer) {
        ctx = c.applicationContext
        load()
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attach(audioSessionId)
            }
        })
        val id = player.audioSessionId
        if (id != 0) attach(id)
    }

    private fun load() {
        val p = prefs() ?: return
        eqOn = p.getBoolean("eqOn", false)
        preset = p.getInt("preset", 0).coerceIn(0, presetNames.size - 1)
        bass = p.getInt("bass", 0)
        loud = p.getInt("loud", 0)
        spatial = p.getInt("spatial", 0)
        crossfade = p.getInt("crossfade", 0)
        val saved = (p.getString("levels", "") ?: "").split(",").mapNotNull { it.toFloatOrNull() }
        if (saved.isNotEmpty()) {
            levels.clear()
            levels.addAll(saved)
        }
    }

    fun save() {
        val p = prefs() ?: return
        p.edit()
            .putBoolean("eqOn", eqOn)
            .putInt("preset", preset)
            .putInt("bass", bass)
            .putInt("loud", loud)
            .putInt("spatial", spatial)
            .putInt("crossfade", crossfade)
            .putString("levels", levels.joinToString(","))
            .apply()
    }

    private fun release() {
        try { eq?.release() } catch (e: Exception) { }
        try { bb?.release() } catch (e: Exception) { }
        try { le?.release() } catch (e: Exception) { }
        try { vi?.release() } catch (e: Exception) { }
        eq = null
        bb = null
        le = null
        vi = null
    }

    private fun attach(sessionId: Int) {
        if (sessionId == 0) return
        release()
        eq = try { Equalizer(0, sessionId) } catch (e: Throwable) { null }
        bb = try { BassBoost(0, sessionId) } catch (e: Throwable) { null }
        le = try { LoudnessEnhancer(sessionId) } catch (e: Throwable) { null }
        vi = try { Virtualizer(0, sessionId) } catch (e: Throwable) { null }
        val missing = ArrayList<String>()
        if (eq == null) missing.add("equalizer")
        if (bb == null) missing.add("bass boost")
        if (le == null) missing.add("loudness")
        if (vi == null) missing.add("3D sound")
        note = if (missing.isEmpty()) "" else "This phone does not support: " + missing.joinToString(", ") + "."
        try {
            val e = eq
            if (e != null) {
                val n = e.getNumberOfBands().toInt()
                bandHz = List(n) { i -> e.getCenterFreq(i.toShort()) / 1000 }
                while (levels.size < n) levels.add(0f)
                while (levels.size > n) levels.removeAt(levels.size - 1)
            }
        } catch (e: Exception) {
        }
        apply()
    }

    fun apply() {
        try {
            val e = eq
            if (e != null) {
                e.setEnabled(eqOn)
                if (eqOn) {
                    val r = e.getBandLevelRange()
                    val lo = r[0].toInt()
                    val hi = r[1].toInt()
                    for (i in 0 until e.getNumberOfBands().toInt()) {
                        val v = levels.getOrElse(i) { 0f }
                        val mb = (v * (if (v >= 0f) hi else -lo)).toInt().coerceIn(lo, hi)
                        e.setBandLevel(i.toShort(), mb.toShort())
                    }
                }
            }
        } catch (e: Exception) {
        }
        try {
            val b = bb
            if (b != null) {
                b.setStrength(bass.toShort())
                b.setEnabled(bass > 0)
            }
        } catch (e: Exception) {
        }
        try {
            val l = le
            if (l != null) {
                l.setTargetGain(loud)
                l.setEnabled(loud > 0)
            }
        } catch (e: Exception) {
        }
        try {
            val v = vi
            if (v != null) {
                v.setStrength(spatial.toShort())
                v.setEnabled(spatial > 0)
            }
        } catch (e: Exception) {
        }
    }

    fun pickPreset(i: Int) {
        preset = i
        if (i in presets.indices) {
            val p = presets[i]
            for (b in levels.indices) levels[b] = p.getOrElse(b) { 0f }
        }
        eqOn = true
        apply()
        save()
    }

    fun setBand(i: Int, v: Float) {
        if (i !in levels.indices) return
        levels[i] = v
        preset = presetNames.size - 1
        eqOn = true
        apply()
    }

    // fade out at the end of a song and fade in at the start of the next one (called about 5 times a second)
    fun tickFade(p: ExoPlayer) {
        val ms = crossfade * 1000L
        if (ms <= 0L) {
            if (fading) {
                p.volume = 1f
                fading = false
            }
            return
        }
        if (!p.isPlaying) return
        val dur = p.duration
        val pos = p.currentPosition
        var v = 1f
        if (dur != C.TIME_UNSET && dur > ms * 2) {
            val left = dur - pos
            if (left in 0 until ms) v = minOf(v, left / ms.toFloat())
        }
        if (pos in 0 until ms) v = minOf(v, pos / ms.toFloat())
        p.volume = v.coerceIn(0f, 1f)
        fading = true
    }
}

@Composable
private fun FxSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 15.sp, color = Color.White)
            Text(valueText, fontSize = 13.sp, color = Pal.muted)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = { SoundFx.save() },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Pal.accent,
                activeTrackColor = Pal.accent,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

// the whole "Sound" block in Settings
@Composable
fun SoundSection() {
    val fx = SoundFx
    KitSwitch("Equalizer", "Change bass, middle and treble to suit your headphones or speaker.", fx.eqOn) {
        fx.eqOn = it
        fx.apply()
        fx.save()
    }
    if (fx.eqOn) {
        KitChips(fx.preset, fx.presetNames) { fx.pickPreset(it) }
        for (i in fx.levels.indices) {
            val hz = fx.bandHz.getOrElse(i) { 0 }
            val name = if (hz >= 1000) (hz / 1000).toString() + " kHz" else "$hz Hz"
            FxSlider(name, (if (fx.levels[i] >= 0f) "+" else "") + (fx.levels[i] * 12).toInt() + " dB", fx.levels[i], -1f..1f) {
                fx.setBand(i, it)
            }
        }
    }
    KitDivider()
    FxSlider("Bass boost", if (fx.bass == 0) "Off" else (fx.bass / 10).toString() + "%", fx.bass.toFloat(), 0f..1000f) {
        fx.bass = it.toInt()
        fx.apply()
    }
    FxSlider("Loudness", if (fx.loud == 0) "Off" else "+" + (fx.loud / 100) + " dB", fx.loud.toFloat(), 0f..1000f) {
        fx.loud = it.toInt()
        fx.apply()
    }
    FxSlider("3D sound", if (fx.spatial == 0) "Off" else (fx.spatial / 10).toString() + "%", fx.spatial.toFloat(), 0f..1000f) {
        fx.spatial = it.toInt()
        fx.apply()
    }
    KitNote("3D sound is best with headphones. Loudness makes quiet songs louder; keep it low to avoid distortion.")
    KitDivider()
    KitLabel("Fade between songs", "The end of a song fades out and the next one fades in.")
    KitChips(
        fx.crossfadeSeconds.indexOf(fx.crossfade).coerceAtLeast(0),
        listOf("Off", "3 s", "6 s", "9 s")
    ) {
        fx.crossfade = fx.crossfadeSeconds[it]
        fx.save()
    }
    if (fx.note.isNotBlank()) KitNote(fx.note)
}
