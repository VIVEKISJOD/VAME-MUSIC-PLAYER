package com.ameos.music

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// App icon variants. Each one is an <activity-alias> in the manifest (IconDefault, IconCrimson ...),
// only one is enabled at a time. The launcher shows the enabled one.
data class IconVariant(val id: String, val name: String, val a: Color, val b: Color)

object AppIcons {
    // the icon that is on right now (the tablet rail logo follows it)
    var live by mutableStateOf("Default")

    val all = listOf(
        IconVariant("Default", "Classic", Color(0xFF4F46E5), Color(0xFF9333EA)),
        IconVariant("Crimson", "Crimson", Color(0xFFFF5C78), Color(0xFF6E0828)),
        IconVariant("Sunset", "Sunset", Color(0xFFFFC450), Color(0xFFD62846)),
        IconVariant("Emerald", "Emerald", Color(0xFF34D399), Color(0xFF064E3B)),
        IconVariant("Ocean", "Ocean", Color(0xFF38BDF8), Color(0xFF1E2978)),
        IconVariant("Violet", "Violet", Color(0xFFC496FF), Color(0xFF3C1482)),
        IconVariant("Midnight", "Midnight", Color(0xFF2E2E38), Color(0xFF000000)),
        IconVariant("Mono", "Mono", Color(0xFF78808E), Color(0xFF1C202A))
    )

    private fun comp(c: Context, id: String) = ComponentName(c.packageName, "com.ameos.music.Icon" + id)

    // the variant that is switched on right now
    fun current(c: Context): String {
        val pm = c.packageManager
        for (v in all) {
            val st = pm.getComponentEnabledSetting(comp(c, v.id))
            if (st == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return v.id
        }
        return "Default"
    }

    fun set(c: Context, id: String) {
        val pm = c.packageManager
        // switch the new one on first, then the others off, so there is always a launcher icon
        pm.setComponentEnabledSetting(
            comp(c, id), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
        for (v in all) {
            if (v.id != id) {
                pm.setComponentEnabledSetting(
                    comp(c, v.id), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}

// Settings > App icon: tap a logo to make it the home screen icon
@Composable
fun AppIconSection(label: Boolean = true) {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(AppIcons.current(ctx)) }
    if (label) SectionLabel("App icon")
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppIcons.all.forEach { v ->
            val on = selected == v.id
            Column(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        if (selected != v.id) {
                            AppIcons.set(ctx, v.id)
                            AppIcons.live = v.id
                            selected = v.id
                            Toast.makeText(
                                ctx, v.name + " icon on. Your launcher may take a few seconds.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(v.a, v.b)))
                        .border(
                            if (on) 3.dp else 1.dp,
                            if (on) Color.White else Color(0x44FFFFFF),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painterResource(R.drawable.ic_launcher_fg), null,
                        Modifier.fillMaxSize()
                    )
                }
                Text(
                    v.name, fontSize = 11.sp,
                    color = if (on) Color.White else Color(0xFF8C7C7E),
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
    Text(
        "Changes the icon on your home screen. Some launchers close the app once when you switch.",
        fontSize = 12.sp, color = Color(0xFF8C7C7E), modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
    )
}
