package com.ameos.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Genres = listOf(
    "Pop", "Rap", "Hip-Hop", "R&B",
    "Rock", "Alternative/Indie", "Bollywood", "Punjabi",
    "Lo-fi", "EDM", "Jazz", "K-pop",
    "Classical", "Devotional", "Country", "Metal"
)

// First-launch page: "what type of songs do you like?" (also reachable from Settings).
@Composable
fun OnboardingScreen(vm: MainViewModel) {
    var sel by remember { mutableStateOf(vm.genres) }
    BackHandler { vm.finishOnboarding(sel) }
    val tv = UiOpts.tv
    val firstFocus = remember { FocusRequester() }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Pal.bgTop, Pal.bgMid, Pal.bgBot)
                )
            )
            .blockTaps()
            .tvPageFocus(firstFocus)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (tv) 64.dp else 20.dp)
        ) {
            Text(
                "What type of songs do you like?",
                fontSize = if (tv) 36.sp else 32.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                modifier = Modifier.padding(top = if (tv) 24.dp else 40.dp)
            )
            Text(
                "Pick a few. They shape your Home, Search and Your Type. You can change this later in Settings.",
                fontSize = 14.sp, color = Pal.textDim,
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp)
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Genres.chunked(if (tv) 4 else 2).forEachIndexed { r, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEachIndexed { c, g ->
                            val on = g in sel
                            Box(
                                Modifier
                                    .weight(1f)
                                    .then(if (r == 0 && c == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                    .padClick(RoundedCornerShape(20.dp), if (on) Pal.accent else Pal.surface) {
                                        sel = if (on) sel - g else sel + g
                                    }
                                    .padding(vertical = if (tv) 26.dp else 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    g, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                    color = if (on) Color(0xFF3A1014) else Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .padClick(RoundedCornerShape(50), Color(0xFFD9381E), 1.02f) { vm.finishOnboarding(sel) }
                    .padding(vertical = 17.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (sel.isEmpty()) "Skip" else "Continue",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }
        }
    }
}
