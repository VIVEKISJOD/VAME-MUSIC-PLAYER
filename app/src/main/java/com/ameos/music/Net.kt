package com.ameos.music

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Watches the internet. When it is gone, `offline` turns true and the app switches itself to
// offline mode (Home / Search / playlists show only downloaded songs). It switches back by
// itself when the internet returns. Settings > Playback > Auto offline mode can turn it off.
object Net {
    var offline by mutableStateOf(false)
    var auto by mutableStateOf(true)
    private var started = false
    private var cm: ConnectivityManager? = null

    private fun check(): Boolean {
        val c = cm ?: return true
        return try {
            val n = c.activeNetwork ?: return false
            val caps = c.getNetworkCapabilities(n) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    // true while the app should behave as offline
    val on: Boolean get() = auto && offline

    fun start(ctx: Context) {
        if (started) return
        started = true
        auto = ctx.getSharedPreferences("vame", Context.MODE_PRIVATE).getBoolean("autoOffline", true)
        val c = ctx.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        cm = c
        if (c == null) return
        offline = !check()
        try {
            c.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    offline = false
                }

                override fun onLost(network: Network) {
                    offline = !check()
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    offline = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            })
        } catch (e: Exception) {
            // some phones refuse; then the app simply never switches to offline mode
        }
    }

    fun chooseAuto(ctx: Context, v: Boolean) {
        auto = v
        ctx.getSharedPreferences("vame", Context.MODE_PRIVATE).edit().putBoolean("autoOffline", v).apply()
    }
}
