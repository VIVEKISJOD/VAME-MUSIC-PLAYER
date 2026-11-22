package com.ameos.music

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// step73: App style = layout, shapes and spacing. It is separate from the colour Look (Pal.look).
// Pal.style: 0 Material, 1 Apple, 2 Spotify. Screens read these tokens, so one switch changes everything.
object St {
    val names = listOf("Material", "Apple", "Spotify")

    val isMaterial: Boolean get() = Pal.style == 0
    val isApple: Boolean get() = Pal.style == 1
    val isSpotify: Boolean get() = Pal.style == 2

    // mini player corner
    val miniR: Dp get() = when (Pal.style) {
        0 -> 28.dp
        1 -> 22.dp
        else -> 10.dp
    }

    // dock (bottom bar) corner
    val barR: Dp get() = when (Pal.style) {
        0 -> 32.dp
        1 -> 26.dp
        else -> 18.dp
    }

    // cards, tiles
    val cardR: Dp get() = when (Pal.style) {
        0 -> 26.dp
        1 -> 16.dp
        else -> 8.dp
    }

    // covers inside rows and shelves
    val coverR: Dp get() = when (Pal.style) {
        0 -> 16.dp
        1 -> 10.dp
        else -> 5.dp
    }

    // big cover on the player
    val playerCoverR: Dp get() = when (Pal.style) {
        0 -> 32.dp
        1 -> 14.dp
        else -> 8.dp
    }

    // big page title size in sp
    val titleSp: Int get() = when (Pal.style) {
        0 -> 32
        1 -> 36
        else -> 28
    }
}
