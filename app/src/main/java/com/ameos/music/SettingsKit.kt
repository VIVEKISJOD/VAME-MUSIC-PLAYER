package com.ameos.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Small building blocks shared by Settings, the Spotify page and the Move-to-another-app page,
// so every page looks the same: a glass card per group, a round icon + title above it.

@Composable
fun KitCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .glassFill()
            .border(1.dp, Color(0x22FFFFFF), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
fun KitHeader(icon: ImageVector, title: String, sub: String? = null) {
    Row(
        Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Pal.accent.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Pal.accent, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Pal.muted)
        }
    }
}

@Composable
fun KitDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(1.dp)
            .background(Color(0x1FFFFFFF))
    )
}

// a small title (and optional grey help text) above chips or buttons inside a card
@Composable
fun KitLabel(title: String, sub: String? = null) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(title, fontSize = 16.sp, color = Color.White)
        if (sub != null) Text(sub, fontSize = 12.sp, color = Pal.muted)
    }
}

@Composable
fun KitSwitch(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (UiOpts.tv) Modifier.clip(RoundedCornerShape(14.dp)).clickable { onChange(!checked) }
                else Modifier
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp, color = Color.White)
            Text(sub, fontSize = 12.sp, color = Pal.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = if (UiOpts.tv) null else onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2FBF71),
                checkedBorderColor = Color(0xFF2FBF71),
                uncheckedThumbColor = Color(0xFF8A8A92),
                uncheckedTrackColor = Color(0xFF2C2C32),
                uncheckedBorderColor = Color(0xFF5A5A62)
            )
        )
    }
}

// a row that does something when tapped (title + grey line under it)
@Composable
fun KitAction(title: String, sub: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(title, fontSize = 16.sp, color = Color.White)
        Text(sub, fontSize = 12.sp, color = Pal.muted)
    }
}

@Composable
fun KitChips(selected: Int, names: List<String>, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        names.forEachIndexed { i, n ->
            val on = selected == i
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Pal.accent else Color(0x33FFFFFF))
                    .clickable { onPick(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    n, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (on) Pal.bgTop else Color.White
                )
            }
        }
    }
}

// full width pill button. primary = filled with the accent colour.
@Composable
fun KitButton(text: String, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(50))
            .then(
                if (primary) Modifier.background(Pal.accent)
                else Modifier.glassFill().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 13.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = if (primary) Pal.bgTop else Color.White,
            textAlign = TextAlign.Center
        )
    }
    Spacer(Modifier.height(8.dp))
}

// small pill button for rows inside a list
@Composable
fun KitSmallBtn(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(50))
            .background(Color(0x33FFFFFF))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun KitNote(text: String) {
    Text(
        text, fontSize = 12.sp, color = Pal.muted,
        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
    )
}
