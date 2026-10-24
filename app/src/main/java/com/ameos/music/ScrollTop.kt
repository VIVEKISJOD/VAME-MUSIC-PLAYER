package com.ameos.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// small round "back to top" button, shows after scrolling down a few songs
@Composable
fun ScrollTopButton(state: LazyListState, modifier: Modifier = Modifier) {
    val show by remember { derivedStateOf { state.firstVisibleItemIndex > 4 } }
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = show,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xCC000000))
                .clickable { scope.launch { state.animateScrollToItem(0) } },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, null, tint = Color.White)
        }
    }
}
