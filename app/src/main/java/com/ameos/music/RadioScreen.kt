package com.ameos.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// step64: Radio tab (the old "Your type" tab). Station cards with the artist picture,
// popular stations, and your genre mixes. Tap = play an endless radio.
private val RadioPastels = listOf(
    Color(0xFFF8D98A), Color(0xFFF2A6AC), Color(0xFFD7A8F8), Color(0xFFA8D8F0),
    Color(0xFFB5E3B0), Color(0xFFF6B98F)
)

private val PopularStations = listOf(
    "The Weeknd", "Bad Bunny", "Arijit Singh", "Taylor Swift", "Drake", "Imagine Dragons",
    "Ed Sheeran", "Dua Lipa", "Diljit Dosanjh", "Bruno Mars"
)

private object RadioCovers {
    val map = HashMap<String, String?>()
}

@Composable
private fun StationCard(name: String, cover: String?, idx: Int, vm: MainViewModel) {
    var pic by remember(name) { mutableStateOf(cover ?: RadioCovers.map[name]) }
    LaunchedEffect(name) {
        if (pic == null && !RadioCovers.map.containsKey(name)) {
            val c = try { MusicRepo.searchArtists(name).firstOrNull()?.cover } catch (e: Exception) { null }
            RadioCovers.map[name] = c
            pic = c
        }
    }
    val tick = rememberTick()
    Column(Modifier.width(150.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pressScale(0.96f)
                .clip(RoundedCornerShape(18.dp))
                .background(RadioPastels[idx % RadioPastels.size])
                .clickable {
                    tick()
                    vm.playRadioFor(name)
                }
        ) {
            Text(
                "RADIO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF16161A),
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            )
            Cover(pic, Modifier.align(Alignment.Center).size(78.dp), CircleShape)
            Text(
                name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16161A),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
            )
        }
        Text(
            "Radio: " + name, fontSize = 13.sp, color = Pal.textDim, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun MixCard(shelf: GenreShelf, idx: Int, vm: MainViewModel) {
    val tick = rememberTick()
    Column(Modifier.width(150.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pressScale(0.96f)
                .clip(RoundedCornerShape(18.dp))
                .background(RadioPastels[(idx + 2) % RadioPastels.size])
                .clickable {
                    tick()
                    vm.startMix(shelf.songs)
                }
        ) {
            Cover(shelf.songs.firstOrNull()?.cover, Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
            Box(Modifier.fillMaxSize().background(Color(0x55000000)))
            Text(
                shelf.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
            )
        }
        Text(
            "Mix " + shelf.name, fontSize = 13.sp, color = Pal.textDim, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun RadioScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) {
        if (vm.genreShelves.isEmpty()) vm.loadGenreShelves()
    }
    // recommended = artists you liked, then artists of the songs you played
    val rec = remember(vm.likedArtists.size, vm.played.size) {
        val out = ArrayList<ArtistItem>()
        for (a in vm.likedArtists) out.add(a)
        for (s in vm.played) {
            val n = artistName(s.artist)
            if (out.none { artistKey(it.name) == artistKey(n) }) out.add(ArtistItem(n, null))
            if (out.size >= 10) break
        }
        out.take(10)
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 200.dp)) {
        item { ScreenTitle("Radio") }
        if (rec.isNotEmpty()) {
            item { ShelfTitle("Recommended stations") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(rec.size) { i -> StationCard(rec[i].name, rec[i].cover, i, vm) }
                }
            }
        }
        item { ShelfTitle("Most popular radio stations") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(PopularStations.size) { i -> StationCard(PopularStations[i], null, i + 1, vm) }
            }
        }
        if (vm.genreShelves.isNotEmpty()) {
            item { ShelfTitle("Your mixes by genre") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vm.genreShelves.size) { i -> MixCard(vm.genreShelves[i], i, vm) }
                }
            }
        }
    }
}
