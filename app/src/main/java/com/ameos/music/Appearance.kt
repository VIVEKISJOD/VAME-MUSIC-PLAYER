package com.ameos.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private data class Preset(val name: String, val hue: Float, val sat: Float)

private val Presets = listOf(
    Preset("Crimson", 355f, 1f),
    Preset("Sunset", 18f, 1f),
    Preset("Amber", 38f, 1f),
    Preset("Emerald", 150f, 1f),
    Preset("Ocean", 210f, 1f),
    Preset("Violet", 265f, 1f),
    Preset("Rose", 325f, 1f),
    Preset("Mono", 0f, 0f)
)

// Settings > Appearance: look (colour glass / AMOLED / AMOLED glass), palettes, custom colour
@Composable
fun AppearanceSection() {
    val ctx = LocalContext.current
    SectionLabel("Appearance")

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("Colour glass", "AMOLED", "AMOLED glass").forEachIndexed { i, name ->
            val on = Pal.mode == i
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .glassFill()
                    .border(
                        if (on) 2.dp else 1.dp,
                        if (on) Pal.accent else Color(0x33FFFFFF),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable {
                        Pal.mode = i
                        Pal.save(ctx)
                    }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (i == 0) Brush.verticalGradient(listOf(Pal.bgTop, Color(0xFF0A0808)))
                            else Brush.verticalGradient(listOf(Color.Black, Color.Black))
                        )
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier
                            .padding(bottom = 7.dp)
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                when (i) {
                                    1 -> Color(0xFF181818)
                                    2 -> Color(0x44FFFFFF)
                                    else -> Color(0x33FFFFFF)
                                }
                            )
                            .border(
                                1.dp,
                                if (i == 1) Color(0x22FFFFFF) else Color(0x66FFFFFF),
                                RoundedCornerShape(50)
                            )
                    )
                }
                Text(
                    name, fontSize = 12.sp, color = Color.White,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
    Text(
        "AMOLED is pure black to save battery. AMOLED glass keeps the black but adds the glass look.",
        fontSize = 12.sp, color = Color(0xFF8C7C7E), modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
    )

    Text("Palette", fontSize = 16.sp, color = Color.White)
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Presets.forEach { p ->
            val on = abs(Pal.hue - p.hue) < 1f && abs(Pal.sat - p.sat) < 0.05f
            Column(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        Pal.hue = p.hue
                        Pal.sat = p.sat
                        Pal.save(ctx)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (p.sat == 0f) Color(0xFFCFCFCF) else Color.hsv(p.hue, 0.5f, 1f)
                        )
                        .border(
                            if (on) 3.dp else 1.dp,
                            if (on) Color.White else Color(0x44FFFFFF),
                            CircleShape
                        )
                )
                Text(
                    p.name, fontSize = 11.sp,
                    color = if (on) Color.White else Color(0xFF8C7C7E),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    Text("Custom colour", fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(top = 14.dp))
    Slider(
        value = Pal.hue,
        onValueChange = {
            Pal.hue = it
            if (Pal.sat < 0.05f) Pal.sat = 1f
        },
        onValueChangeFinished = { Pal.save(ctx) },
        valueRange = 0f..359f,
        colors = SliderDefaults.colors(
            thumbColor = Pal.accent,
            activeTrackColor = Pal.accent,
            inactiveTrackColor = Color(0x33FFFFFF)
        )
    )
    Text("Colour strength", fontSize = 16.sp, color = Color.White)
    Slider(
        value = Pal.sat,
        onValueChange = { Pal.sat = it },
        onValueChangeFinished = { Pal.save(ctx) },
        valueRange = 0f..1f,
        colors = SliderDefaults.colors(
            thumbColor = Pal.accent,
            activeTrackColor = Pal.accent,
            inactiveTrackColor = Color(0x33FFFFFF)
        )
    )
    Spacer(Modifier.height(4.dp))
}
