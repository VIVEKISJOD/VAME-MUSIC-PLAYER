package com.ameos.music

// "Stay in the same style": ranks the related songs against what you really listen to, so one
// off-genre song (a Punjabi track after 21 Savage) is not mixed in and does not become the seed
// for more of them. Anchor = the song playing + your last songs + your liked songs.
object StyleRank {

    // 0 Latin, 1 Devanagari, 2 Gurmukhi, 3 other
    private fun script(text: String): Int {
        var dev = 0
        var gur = 0
        var lat = 0
        var oth = 0
        for (ch in text) {
            val c = ch.code
            if (c in 0x0900..0x097F) dev++
            else if (c in 0x0A00..0x0A7F) gur++
            else if (c in 0x0041..0x007A) lat++
            else if (ch.isLetter()) oth++
        }
        val best = maxOf(maxOf(dev, gur), maxOf(lat, oth))
        if (best == 0) return 0
        return if (best == lat) 0 else if (best == dev) 1 else if (best == gur) 2 else 3
    }

    private val tags = listOf(
        "punjabi" to listOf("punjabi", "bhangra", "desi", "sidhu", "aujla", "diljit", "ap dhillon", "dhillon"),
        "hindi" to listOf("hindi", "bollywood", "t-series", "tseries", "arijit", "lofi hindi"),
        "tamil" to listOf("tamil", "kollywood", "anirudh"),
        "telugu" to listOf("telugu", "tollywood"),
        "korean" to listOf("k-pop", "kpop", "korean"),
        "spanish" to listOf("reggaeton", "latino", "spanish", "bad bunny")
    )

    private fun tagsOf(s: Song): Set<String> {
        val t = (s.title + " " + s.artist).lowercase()
        val out = HashSet<String>()
        for ((name, words) in tags) if (words.any { t.contains(it) }) out.add(name)
        return out
    }

    private fun names(artist: String): Set<String> =
        artistName(artist).lowercase()
            .split(",", "&", " x ", " feat. ", " feat ", " ft. ", " ft ", " and ")
            .map { it.filter { c -> c.isLetterOrDigit() } }
            .filter { it.length >= 3 }
            .toSet()

    private fun featNames(title: String): Set<String> {
        val t = title.lowercase()
        val i = listOf("feat.", "feat ", "ft.", "ft ", "with ").map { t.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: return emptySet()
        return names(t.substring(i).replace(Regex("^(feat\\.|feat|ft\\.|ft|with)\\s*"), "").trimEnd(')', ']'))
    }

    fun rank(current: Song, recent: List<Song>, liked: List<Song>, candidates: List<Song>): List<Song> {
        if (candidates.size < 3) return candidates
        val anchor = ArrayList<Song>()
        anchor.add(current)
        anchor.addAll(recent.take(8))
        val likedAnchor = liked.take(30)
        val aNames = HashSet<String>()
        val aScripts = IntArray(4)
        val aTags = HashSet<String>()
        for (s in anchor) {
            aNames.addAll(names(s.artist))
            aNames.addAll(featNames(s.title))
            aScripts[script(s.title + " " + s.artist)]++
            aTags.addAll(tagsOf(s))
        }
        val lNames = HashSet<String>()
        for (s in likedAnchor) lNames.addAll(names(s.artist))
        val domScript = aScripts.indices.maxByOrNull { aScripts[it] } ?: 0
        val curNames = names(current.artist) + featNames(current.title)

        val scored = candidates.map { c ->
            var sc = 0
            val cn = names(c.artist) + featNames(c.title)
            if (cn.any { it in curNames }) sc += 4
            sc += 2 * cn.count { it in aNames && it !in curNames }.coerceAtMost(2)
            if (cn.any { it in lNames }) sc += 2
            val cs = script(c.title + " " + c.artist)
            if (cs == domScript) sc += 1 else if (aScripts[cs] == 0) sc -= 4
            val ct = tagsOf(c)
            if (ct.isNotEmpty()) {
                if (ct.any { it in aTags }) sc += 2 else sc -= 3
            }
            Pair(c, sc)
        }
        val kept = scored.filter { it.second >= -2 }
        // keep the original order for equal scores (sortedByDescending is stable)
        val pick = if (kept.size >= 4) kept else scored
        return pick.sortedByDescending { it.second }.map { it.first }
    }
}
