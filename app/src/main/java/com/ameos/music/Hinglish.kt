package com.ameos.music

import java.text.Normalizer

// Turns Hindi (Devanagari) text into Hinglish (English letters). Other text is left alone.
object Hinglish {

    private val cons = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "n",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "n",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'ळ' to "l", 'व' to "v",
        'श' to "sh", 'ष' to "sh", 'स' to "s", 'ह' to "h"
    )

    private val vowels = mapOf(
        'अ' to "a", 'आ' to "a", 'इ' to "i", 'ई' to "i", 'उ' to "u", 'ऊ' to "u",
        'ऋ' to "ri", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au",
        'ऑ' to "o", 'ऍ' to "e"
    )

    private val matras = mapOf(
        'ा' to "a", 'ि' to "i", 'ी' to "i", 'ु' to "u", 'ू' to "u", 'ृ' to "ri",
        'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au", 'ॉ' to "o", 'ॅ' to "e"
    )

    private val nukta = mapOf(
        "k" to "q", "kh" to "kh", "g" to "g", "j" to "z",
        "d" to "r", "dh" to "rh", "ph" to "f"
    )

    // common words where the rule based result is not how people write them
    private val words = mapOf(
        "हम" to "hum", "में" to "mein", "नहीं" to "nahi", "यह" to "yeh", "वह" to "woh",
        "ये" to "ye", "वो" to "woh", "तू" to "tu", "क्या" to "kya", "कि" to "ki",
        "जो" to "jo", "ना" to "na", "न" to "na", "ऐ" to "ae"
    )

    private class Syl(var c: String, var v: String?) {
        var tail: String = ""
    }

    private fun isDev(ch: Char): Boolean =
        ch in '\u0900'..'\u0963' || ch in '\u0966'..'\u097F'

    fun convert(text: String): String {
        if (text.none { isDev(it) }) return text
        val s = Normalizer.normalize(text, Normalizer.Form.NFC)
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (isDev(ch)) {
                var j = i
                while (j < s.length && isDev(s[j])) j++
                out.append(word(s.substring(i, j)))
                i = j
            } else {
                if (ch != '\u0964' && ch != '\u0965') out.append(ch)
                i++
            }
        }
        return out.toString().replace("  ", " ").trim()
    }

    private fun word(raw: String): String {
        val dict = words[raw]
        if (dict != null) return dict
        val w = raw.replace("ज्ञ", "ग्य")
        val list = ArrayList<Syl>()
        var k = 0
        while (k < w.length) {
            val ch = w[k]
            val base = cons[ch]
            if (base != null) {
                var c: String = base
                k++
                if (k < w.length && w[k] == '\u093C') {
                    c = nukta[c] ?: c
                    k++
                }
                val syl = Syl(c, null)
                if (k < w.length) {
                    val m = matras[w[k]]
                    if (m != null) {
                        syl.v = m
                        k++
                    } else if (w[k] == '\u094D') {
                        syl.v = ""
                        k++
                    }
                }
                list.add(syl)
            } else if (vowels.containsKey(ch)) {
                list.add(Syl("", vowels[ch]))
                k++
            } else if (ch == '\u0902' || ch == '\u0901') {
                if (list.isNotEmpty()) list[list.size - 1].tail += "n"
                k++
            } else if (ch == '\u0903') {
                if (list.isNotEmpty()) list[list.size - 1].tail += "h"
                k++
            } else if (ch in '\u0966'..'\u096F') {
                list.add(Syl((ch - '\u0966').toString(), ""))
                k++
            } else {
                k++
            }
        }

        val n = list.size
        val keep = BooleanArray(n)
        for (i in 0 until n) {
            val s = list[i]
            if (s.v != null) continue
            if (n == 1) {
                keep[i] = true
                continue
            }
            if (i == n - 1) {
                keep[i] = false
                continue
            }
            val prev = if (i > 0) list[i - 1] else null
            val prevHasVowel = prev != null && prev.tail.isEmpty() &&
                ((prev.v == null && keep[i - 1]) || (prev.v != null && prev.v != ""))
            val nx = list[i + 1].v
            val nextExplicit = nx != null && nx != ""
            keep[i] = !(prevHasVowel && nextExplicit)
        }

        val sb = StringBuilder()
        for (i in 0 until n) {
            val s = list[i]
            sb.append(s.c)
            val v = s.v
            if (v == null) {
                if (keep[i]) sb.append("a")
            } else {
                sb.append(v)
            }
            sb.append(s.tail)
        }
        return sb.toString()
    }
}
