package com.ameos.music

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

// One pinned tile on Home.
// kind: "playlist" (id = VAME playlist id), "album" (id = album link or "yt:" + YouTube id),
// "artist" (id = artist name), "song" (id = song link)
data class Dial(val kind: String, val id: String, val title: String, val sub: String, val cover: String?)

object SpeedDial {
    private fun sp(c: Context) = c.getSharedPreferences("vame", Context.MODE_PRIVATE)

    fun load(c: Context): List<Dial> {
        val raw = sp(c).getString("speedDial", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Dial(
                    o.optString("k"), o.optString("i"), o.optString("t"), o.optString("s"),
                    if (o.isNull("c")) null else o.optString("c")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(c: Context, list: List<Dial>) {
        val arr = JSONArray()
        for (d in list) {
            val o = JSONObject()
            o.put("k", d.kind)
            o.put("i", d.id)
            o.put("t", d.title)
            o.put("s", d.sub)
            o.put("c", d.cover ?: JSONObject.NULL)
            arr.put(o)
        }
        sp(c).edit().putString("speedDial", arr.toString()).apply()
    }
}

// Pin / unpin with a short message
fun pinToggle(vm: MainViewModel, ctx: Context, d: Dial) {
    val on = vm.toggleDial(d)
    Toast.makeText(
        ctx, if (on) "Pinned to speed dial" else "Removed from speed dial", Toast.LENGTH_SHORT
    ).show()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedDialShelf(vm: MainViewModel) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val tick = rememberTick()
    val list = vm.dials.toList()
    val cols = if (LocalConfiguration.current.screenWidthDp >= 600) 6 else 3
    val shape = RoundedCornerShape(18.dp)
    ShelfTitle("Speed dial", "Hold a tile to remove it")
    Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in list.chunked(cols)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (d in row) {
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(Color(0x22FFFFFF))
                            .combinedClickable(
                                onClick = {
                                    tick()
                                    vm.openDial(d)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pinToggle(vm, ctx, d)
                                }
                            )
                    ) {
                        Cover(d.cover, Modifier.fillMaxSize(), shape)
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.45f to Color.Transparent,
                                        1f to Color(0xCC000000)
                                    )
                                )
                        )
                        Text(
                            d.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 8.dp, end = 8.dp, bottom = 7.dp)
                        )
                    }
                }
                for (k in 0 until cols - row.size) Spacer(Modifier.weight(1f))
            }
        }
    }
}
