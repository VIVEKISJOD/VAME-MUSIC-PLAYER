package com.ameos.music

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// step60: lyric styles (a look for the lyrics that fits the song) and lines that move left / right with the speed of the song.

class LyricSpec(
    val name: String,
    val fontKey: String,        // part of the font file name in assets/fonts ("" = normal font)
    val prefer: List<String>,   // words that make a file win when a font has many files (bold, italic)
    val caps: Boolean,          // CAPITAL letters
    val track: Float,           // letter spacing in sp
    val scale: Float,           // text size factor (fonts look bigger or smaller)
    val glow: Float,            // glow strength of the line that is sung
    val fixed: Color?,          // fixed colour of the sung line (null = from the cover)
    val tintMix: Float,         // how much of the cover colour goes into the sung line
    val slide: Int,             // dp the other lines sit away from the middle
    val animMs: Int             // speed of the slide
)

object LyricStyle {
    // 0 = Auto (the app guesses from the song), 1 = Classic (the old look), 2.. = the styles
    val names = listOf("Auto", "Classic", "Soft", "Dark", "Rap", "Romantic", "Party", "Retro")

    private val specs = listOf(
        LyricSpec("Classic", "", emptyList(), false, 0f, 1f, 0.55f, null, 0f, 10, 450),
        LyricSpec("Soft", "baloo", listOf("bold"), false, 0f, 1f, 0.6f, null, 0.55f, 8, 800),
        LyricSpec("Dark", "metalmania", emptyList(), true, 1f, 1.1f, 0.9f, Color(0xFFFF3B30), 0f, 14, 260),
        LyricSpec("Rap", "teko", listOf("bold"), true, 1f, 1.3f, 0.35f, null, 0f, 22, 200),
        LyricSpec("Romantic", "playfair", listOf("bold", "italic"), false, 0f, 0.95f, 0.5f, Color(0xFFFFD6E0), 0f, 8, 950),
        LyricSpec("Party", "bungee", emptyList(), true, 0.5f, 0.78f, 0.8f, null, 1f, 18, 280),
        LyricSpec("Retro", "rozha", emptyList(), false, 0f, 0.95f, 0.55f, Color(0xFFFFD27A), 0f, 12, 600)
    )

    var choice by mutableIntStateOf(0)
    var moving by mutableStateOf(true)

    fun load(c: Context) {
        val p = c.getSharedPreferences("vame_lyrics", Context.MODE_PRIVATE)
        choice = p.getInt("choice", 0).coerceIn(0, names.size - 1)
        moving = p.getBoolean("moving", true)
    }

    fun save(c: Context) {
        c.getSharedPreferences("vame_lyrics", Context.MODE_PRIVATE).edit()
            .putInt("choice", choice).putBoolean("moving", moving).apply()
    }

    // detected = 0 (nothing clear, use Classic) or the index in `specs` + 1
    fun specFor(choice: Int, detected: Int): LyricSpec {
        val idx = if (choice == 0) (if (detected <= 0) 1 else detected) else choice
        return specs[(idx - 1).coerceIn(0, specs.size - 1)]
    }

    private val darkWords = listOf("metal", "hardcore", "drill", "phonk", "demon", "devil", "dark", "death", "hell", "kill", "scream", "horror", "gothic", "shaitan", "maut", "qatl")
    private val rapWords = listOf("rap", "hip hop", "hiphop", "freestyle", "cypher", "diss", "mc ", "seedhe maut", "raftaar", "emiway", "divine", "krsna", "krsna rap", "badshah", "honey singh", "ikka", "talha anjum", "trap", "bars")
    private val softWords = listOf("lofi", "lo-fi", "acoustic", "chill", "soothing", "sleep", "calm", "piano", "slowed", "reverb", "unplugged", "mellow", "relax")
    private val romanticWords = listOf("love", "pyar", "ishq", "mohabbat", "dil", "sad", "broken", "tanha", "judai", "dard", "heart", "romantic", "ballad", "arijit", "atif", "tum ", "kasam", "bewafa", "yaad")
    private val partyWords = listOf("dj", "remix", "party", "dance", "club", "edm", "bhangra", "punjabi", "garba", "mashup", "nacho", "beat", "sharaab", "daru")
    private val retroWords = listOf("kishore", "rafi", "lata", "asha bhosle", "mukesh", "90s", "70s", "80s", "retro", "classic", "old is gold", "burman", "geeta dutt", "manna dey")

    private fun hits(text: String, words: List<String>): Int = words.count { text.contains(it) }

    // words sung per second on every line (the speed of the song as we can see it from the lyrics)
    fun wps(lines: List<LyricLine>): FloatArray {
        val out = FloatArray(lines.size)
        for (i in lines.indices) {
            val words = lines[i].text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            val next = lines.getOrNull(i + 1)?.timeMs ?: (lines[i].timeMs + 4000L)
            val span = ((next - lines[i].timeMs) / 1000f).coerceIn(1f, 7f)
            out[i] = if (words == 0) 0f else words / span
        }
        return out
    }

    // guess a style from the title, the artist and how fast the lyrics go. 0 = no clear guess (Classic).
    fun detect(title: String, artist: String, lines: List<LyricLine>): Int {
        val text = (title + " " + artist).lowercase()
        val score = IntArray(specs.size + 1) // index = style index (1 Classic .. 7 Retro)
        score[3] += hits(text, darkWords) * 2
        score[4] += hits(text, rapWords) * 2
        score[2] += hits(text, softWords) * 2
        score[5] += hits(text, romanticWords)
        score[6] += hits(text, partyWords) * 2
        score[7] += hits(text, retroWords) * 2
        val sung = wps(lines).filter { it > 0f }.sorted()
        if (sung.size >= 6) {
            val mid = sung[sung.size / 2]
            if (mid > 3.0f) score[4] += 3 else if (mid > 2.4f) score[4] += 1
            if (mid < 1.5f) {
                score[2] += 1
                score[5] += 1
            }
        }
        var best = 0
        var bestScore = 1
        for (i in 2..7) {
            if (score[i] > bestScore) {
                bestScore = score[i]
                best = i
            }
        }
        return best
    }
}

// left / right / middle for every line: slow songs stay in the middle, fast songs jump from side to side
object LyricMotion {
    fun sides(lines: List<LyricLine>): IntArray {
        val w = LyricStyle.wps(lines)
        val out = IntArray(lines.size)
        var side = -1
        var count = 0
        for (i in lines.indices) {
            var sum = 0f
            var n = 0
            for (k in (i - 1)..(i + 1)) {
                if (k in lines.indices && w[k] > 0f) {
                    sum += w[k]
                    n++
                }
            }
            val s = if (n == 0) 0f else sum / n
            if (s < 1.4f) {
                out[i] = 0
                continue
            }
            val period = if (s < 2.8f) 2 else 1
            if (count % period == 0) side = -side
            count++
            out[i] = side
        }
        return out
    }
}

// finds the font files in assets/fonts by the name inside the file name (see assets/fonts/README.txt)
object LyricFonts {
    private val cache = HashMap<String, FontFamily?>()

    fun family(ctx: Context, key: String, prefer: List<String>): FontFamily? {
        if (key.isBlank()) return null
        if (cache.containsKey(key)) return cache[key]
        var found: FontFamily? = null
        try {
            val files = (ctx.assets.list("fonts") ?: emptyArray())
                .filter { it.endsWith(".ttf", true) || it.endsWith(".otf", true) }
            var bestName: String? = null
            var bestScore = -1
            for (f in files) {
                val norm = f.lowercase().filter { it.isLetter() }
                if (!norm.contains(key)) continue
                val sc = prefer.count { norm.contains(it) }
                if (sc > bestScore) {
                    bestScore = sc
                    bestName = f
                }
            }
            if (bestName != null) {
                found = FontFamily(Typeface.createFromAsset(ctx.assets, "fonts/" + bestName))
            }
        } catch (e: Exception) {
            found = null
        }
        cache[key] = found
        return found
    }
}
