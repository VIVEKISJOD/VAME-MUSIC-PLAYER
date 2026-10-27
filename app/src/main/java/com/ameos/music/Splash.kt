package com.ameos.music

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// step55: short logo animation (about 1.3 seconds) when the app opens.
// It plays once per app start (turning the screen does not play it again).
object SplashState {
    var done by mutableStateOf(false)
}

@Composable
fun LogoSplash() {
    if (SplashState.done) return
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val veil = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            // 0.0 - 0.6 s: logo pops in with a small bounce, glow and name fade in
            launch { logoAlpha.animateTo(1f, tween(280)) }
            launch {
                logoScale.animateTo(
                    1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)
                )
            }
            launch { glow.animateTo(1f, tween(600)) }
            launch {
                delay(200)
                textAlpha.animateTo(1f, tween(300))
            }
        }
        // short hold, then the whole splash fades away and the logo grows a little
        delay(350)
        coroutineScope {
            launch { veil.animateTo(0f, tween(320)) }
            launch { logoScale.animateTo(1.15f, tween(320)) }
        }
        SplashState.done = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = veil.value }
            .background(Color(0xFF130709))
            // the splash catches touches while it is on screen
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        e.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(300.dp)
                .graphicsLayer {
                    alpha = glow.value * 0.9f
                    scaleX = 0.6f + 0.4f * glow.value
                    scaleY = 0.6f + 0.4f * glow.value
                }
                .background(
                    Brush.radialGradient(listOf(Color(0x558B3DFF), Color(0x00000000))),
                    CircleShape
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painterResource(R.drawable.logo_app), null,
                Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    }
                    .clip(RoundedCornerShape(26.dp))
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "VAME", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White,
                letterSpacing = 6.sp,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            )
        }
    }
}
