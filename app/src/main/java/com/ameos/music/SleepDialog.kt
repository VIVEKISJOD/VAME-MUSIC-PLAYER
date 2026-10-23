package com.ameos.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val DlgBg: Color get() = Pal.surface
private val CardBg: Color get() = Pal.surface2
private val Accent: Color get() = Pal.accent
private val Dim: Color get() = Pal.textDim

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Accent else CardBg)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = if (selected) Color(0xFF3A1014) else Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        Modifier.padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun NumberField(
    value: String,
    hint: String,
    onValue: (String) -> Unit,
    onSet: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = value,
            onValueChange = onValue,
            placeholder = { Text(hint, color = Dim) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Accent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Accent)
                .clickable { onSet() }
                .padding(horizontal = 26.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Set", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A1014))
        }
    }
}

@Composable
fun SleepDialog(vm: MainViewModel) {
    var customMin by remember { mutableStateOf("") }
    var customTimes by remember { mutableStateOf("") }
    val close = { vm.showSleepDialog = false }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(32.dp))
                .glassPanel(RoundedCornerShape(32.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bedtime, null, tint = Accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sleep and repeat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // what is running now
            val running = vm.sleepLeftSec > 0 || vm.sleepAfterSong || vm.timesLeft > 0
            if (running) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Pal.accent.copy(alpha = 0.2f))
                        .padding(start = 18.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        if (vm.sleepLeftSec > 0) {
                            val m = vm.sleepLeftSec / 60
                            val s = (vm.sleepLeftSec % 60).toString().padStart(2, '0')
                            Text("Stops in $m:$s", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (vm.sleepAfterSong) {
                            Text("Stops when this song ends", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (vm.timesLeft > 0) {
                            Text("Plays left: ${vm.timesLeft}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Text(
                        "Cancel", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                vm.setSleep(0)
                                vm.timesLeft = 0
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            SectionTitle(Icons.Filled.Bedtime, "Stop playing after")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("15 min", false, Modifier.weight(1f)) { vm.setSleep(15); close() }
                Chip("30 min", false, Modifier.weight(1f)) { vm.setSleep(30); close() }
                Chip("45 min", false, Modifier.weight(1f)) { vm.setSleep(45); close() }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("60 min", false, Modifier.weight(1f)) { vm.setSleep(60); close() }
                Chip("End of song", vm.sleepAfterSong, Modifier.weight(1.4f)) { vm.setSleep(0, true); close() }
            }
            NumberField(
                value = customMin,
                hint = "Custom minutes",
                onValue = { customMin = it.filter { c -> c.isDigit() }.take(3) },
                onSet = {
                    val n = customMin.toIntOrNull()
                    if (n != null && n > 0) {
                        vm.setSleep(n.coerceAtMost(600))
                        close()
                    }
                }
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardBg)
                    .padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Let the song finish", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "When time is up, the current song plays to its end first.",
                        fontSize = 13.sp, color = Dim
                    )
                }
                Switch(checked = vm.finishSong, onCheckedChange = { vm.setFinishSongOn(it) })
            }

            SectionTitle(Icons.Filled.Repeat, "Play this song, then stop")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("1x", false, Modifier.weight(1f)) { vm.playTimes(1); close() }
                Chip("2x", false, Modifier.weight(1f)) { vm.playTimes(2); close() }
                Chip("3x", false, Modifier.weight(1f)) { vm.playTimes(3); close() }
                Chip("5x", false, Modifier.weight(1f)) { vm.playTimes(5); close() }
            }
            NumberField(
                value = customTimes,
                hint = "Custom times",
                onValue = { customTimes = it.filter { c -> c.isDigit() }.take(3) },
                onSet = {
                    val n = customTimes.toIntOrNull()
                    if (n != null && n > 0) {
                        vm.playTimes(n.coerceAtMost(500))
                        close()
                    }
                }
            )
            Text(
                "The song restarts from the beginning, plays that many times, then stops.",
                fontSize = 13.sp, color = Dim,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CardBg)
                    .clickable { close() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
