package com.ameos.music

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class OutDevice(val id: Int, val name: String, val kind: Int, val info: AudioDeviceInfo)

object AudioOutput {
    const val PHONE = 0
    const val BLUETOOTH = 1
    const val WIRED = 2

    fun list(ctx: Context): List<OutDevice> {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val out = ArrayList<OutDevice>()
        var phoneAdded = false
        for (d in am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            val t = d.type
            val raw = (d.productName ?: "").toString().trim()
            val isBle = Build.VERSION.SDK_INT >= 31 &&
                (t == AudioDeviceInfo.TYPE_BLE_HEADSET || t == AudioDeviceInfo.TYPE_BLE_SPEAKER)
            if (t == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                if (!phoneAdded) {
                    phoneAdded = true
                    out.add(OutDevice(d.id, "This phone", PHONE, d))
                }
            } else if (t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || isBle) {
                out.add(OutDevice(d.id, if (raw.isEmpty()) "Bluetooth" else raw, BLUETOOTH, d))
            } else if (t == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                t == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                t == AudioDeviceInfo.TYPE_USB_HEADSET
            ) {
                out.add(OutDevice(d.id, if (raw.isEmpty()) "Headphones" else raw, WIRED, d))
            }
        }
        return out.sortedBy { if (it.kind == PHONE) 0 else 1 }
    }

    fun watch(ctx: Context, onChange: () -> Unit) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                onChange()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                onChange()
            }
        }, Handler(Looper.getMainLooper()))
    }
}

private val OBg: Color get() = Pal.surface
private val ORow: Color get() = Pal.surface2
private val OSub: Color get() = Pal.textDim
private val OAccent: Color get() = Pal.accent

// "Play audio on" picker: this phone / Bluetooth / headphones
@Composable
fun OutputDialog(vm: MainViewModel) {
    val ctx = LocalContext.current
    val close = { vm.showOutputDialog = false }
    val selected = vm.currentOutput?.id
    val hasExternal = vm.outputs.any { it.kind != AudioOutput.PHONE }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(32.dp))
                .glassPanel(RoundedCornerShape(32.dp))
                .padding(20.dp)
        ) {
            Text("Play audio on", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                vm.outputs.forEach { d ->
                    val on = d.id == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ORow)
                            .clickable {
                                vm.chooseOutput(d)
                                close()
                            }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (d.kind) {
                            AudioOutput.BLUETOOTH -> Icons.Filled.Bluetooth
                            AudioOutput.WIRED -> Icons.Filled.Headphones
                            else -> Icons.Filled.PhoneAndroid
                        }
                        Icon(icon, null, tint = if (on) OAccent else OSub, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            d.name, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = if (on) OAccent else Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (on) {
                            Icon(Icons.Filled.Check, null, tint = OAccent)
                        }
                    }
                }
                if (!hasExternal) {
                    Text(
                        "No Bluetooth device connected.",
                        fontSize = 14.sp, color = OSub,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp)
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ORow)
                        .clickable {
                            close()
                            try {
                                val i = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ctx.startActivity(i)
                            } catch (e: Exception) {
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Settings, null, tint = OSub, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Bluetooth settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
