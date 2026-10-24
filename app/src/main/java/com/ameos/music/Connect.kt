package com.ameos.music

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Another VAME app found on the same Wi-Fi (phone, tablet, TV)
class Peer(val id: String, val name: String, val host: String, val port: Int, val service: String)

// What the other device is playing right now (read from its /state every second)
class Remote(
    val title: String,
    val artist: String,
    val cover: String?,
    val url: String,
    val playing: Boolean,
    val posMs: Long,
    val durMs: Long,
    val vol: Int,
    val at: Long = SystemClock.elapsedRealtime()
)

// "Play on another device", like Spotify Connect, same Wi-Fi only, no account, no server.
// Every VAME app runs a tiny web server (port 8765+) and announces itself with NSD (_vame._tcp).
// The controller sends the queue to the chosen device (/play), sends buttons (/cmd) and reads /state.
object Connect {
    private const val TYPE = "_vame._tcp."

    val peers = mutableStateListOf<Peer>()
    var target by mutableStateOf<Peer?>(null)      // null = this device plays
    var remote by mutableStateOf<Remote?>(null)
    var allow by mutableStateOf(true)              // other devices may play here
    var deviceName = ""
    var myId = ""

    @Volatile var stateJson = "{\"has\":false}"
    @Volatile var vm: MainViewModel? = null
    @Volatile private var lastSend = 0L

    private var started = false
    private var server: NanoHTTPD? = null
    private var nsd: NsdManager? = null
    private var pollJob: Job? = null
    private var resolving = false
    private val resolveQueue = ArrayList<NsdServiceInfo>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    @Synchronized
    fun start(app: Application, model: MainViewModel) {
        vm = model
        if (started) return
        started = true
        val sp = app.getSharedPreferences("vame", Context.MODE_PRIVATE)
        allow = sp.getBoolean("connectAllow", true)
        val saved = sp.getString("connectId", null)
        val id = saved ?: java.util.UUID.randomUUID().toString().take(8)
        if (saved == null) sp.edit().putString("connectId", id).apply()
        myId = id
        deviceName = nameOf(app)
        scope.launch {
            var port = 8765
            var srv: NanoHTTPD? = null
            while (port < 8790 && srv == null) {
                val s = Srv(port)
                try {
                    s.start(5000, false)
                    srv = s
                } catch (e: Exception) {
                    port++
                }
            }
            if (srv != null) {
                server = srv
                register(app, port)
                discover()
            }
        }
    }

    fun setAllow(app: Context, on: Boolean) {
        allow = on
        app.getSharedPreferences("vame", Context.MODE_PRIVATE).edit().putBoolean("connectAllow", on).apply()
    }

    private fun nameOf(app: Application): String {
        val n = try {
            android.provider.Settings.Global.getString(app.contentResolver, "device_name")
        } catch (e: Exception) {
            null
        }
        return if (!n.isNullOrBlank()) n else Build.MODEL
    }

    // ---------------- the small web server on this device ----------------

    private class Srv(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val path = session.uri
            if (path == "/state") {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", stateJson)
            }
            if (!allow) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "off")
            }
            if (path == "/play" && session.method == Method.POST) {
                val files = HashMap<String, String>()
                try {
                    session.parseBody(files)
                } catch (e: Exception) {
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "bad")
                }
                val body = files["postData"]
                if (body != null) handlePlay(body)
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain", "ok")
            }
            if (path == "/cmd") {
                val what = session.parameters["do"]?.firstOrNull() ?: ""
                val v = session.parameters["v"]?.firstOrNull()?.toLongOrNull() ?: 0L
                main.post { vm?.connectCmd(what, v) }
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain", "ok")
            }
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "?")
        }
    }

    private fun handlePlay(body: String) {
        try {
            val o = JSONObject(body)
            val arr = o.getJSONArray("q")
            val list = ArrayList<Song>()
            for (k in 0 until arr.length()) list.add(songOf(arr.getJSONObject(k)))
            val i = o.optInt("i", 0)
            val pos = o.optLong("pos", 0L)
            main.post { vm?.connectPlayHere(list, i, pos) }
        } catch (e: Exception) {
        }
    }

    private fun songOf(o: JSONObject): Song {
        val cover = if (o.has("c")) o.getString("c") else null
        return Song(o.optString("t"), o.optString("a"), o.optString("u"), cover, o.optLong("d", 0L))
    }

    private fun songJson(s: Song): JSONObject {
        val o = JSONObject()
        o.put("t", s.title)
        o.put("a", s.artist)
        o.put("u", s.url)
        val c = s.cover
        if (c != null) o.put("c", c)
        o.put("d", s.durationSec)
        return o
    }

    // ---------------- finding the other devices (NSD) ----------------

    private fun register(app: Application, port: Int) {
        try {
            val m = app.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsd = m
            val info = NsdServiceInfo()
            info.serviceName = "VAME-" + myId
            info.serviceType = TYPE
            info.port = port
            info.setAttribute("i", myId)
            info.setAttribute("n", deviceName)
            m.registerService(info, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(s: NsdServiceInfo, e: Int) {}
                override fun onUnregistrationFailed(s: NsdServiceInfo, e: Int) {}
                override fun onServiceRegistered(s: NsdServiceInfo) {}
                override fun onServiceUnregistered(s: NsdServiceInfo) {}
            })
        } catch (e: Exception) {
        }
    }

    private fun discover() {
        val m = nsd ?: return
        try {
            m.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(t: String, e: Int) {}
                override fun onStopDiscoveryFailed(t: String, e: Int) {}
                override fun onDiscoveryStarted(t: String) {}
                override fun onDiscoveryStopped(t: String) {}
                override fun onServiceFound(s: NsdServiceInfo) {
                    if (s.serviceName == "VAME-" + myId) return
                    main.post {
                        resolveQueue.add(s)
                        pump()
                    }
                }

                override fun onServiceLost(s: NsdServiceInfo) {
                    main.post { peers.removeAll { it.service == s.serviceName } }
                }
            })
        } catch (e: Exception) {
        }
    }

    // NSD can resolve only one service at a time
    private fun pump() {
        if (resolving || resolveQueue.isEmpty()) return
        val s = resolveQueue.removeAt(0)
        resolving = true
        try {
            nsd?.resolveService(s, object : NsdManager.ResolveListener {
                override fun onResolveFailed(i: NsdServiceInfo, e: Int) {
                    main.post {
                        resolving = false
                        pump()
                    }
                }

                override fun onServiceResolved(i: NsdServiceInfo) {
                    val raw = i.host?.hostAddress
                    val id = String(i.attributes["i"] ?: ByteArray(0))
                    val nm = String(i.attributes["n"] ?: i.serviceName.toByteArray())
                    val port = i.port
                    val svc = i.serviceName
                    main.post {
                        if (raw != null && id != myId) {
                            val host = if (raw.contains(":")) "[" + raw + "]" else raw
                            val key = if (id.isBlank()) svc else id
                            peers.removeAll { it.id == key || it.service == svc }
                            peers.add(Peer(key, if (nm.isBlank()) svc else nm, host, port, svc))
                        }
                        resolving = false
                        pump()
                    }
                }
            })
        } catch (e: Exception) {
            resolving = false
        }
    }

    // ---------------- talking to the chosen device ----------------

    fun sendPlay(p: Peer, list: List<Song>, index: Int, posMs: Long) {
        lastSend = SystemClock.elapsedRealtime()
        val from = (index - 5).coerceAtLeast(0)
        val arr = JSONArray()
        list.drop(from).take(200).forEach { arr.put(songJson(it)) }
        val o = JSONObject()
        o.put("q", arr)
        o.put("i", index - from)
        o.put("pos", posMs)
        scope.launch {
            try {
                val req = Request.Builder()
                    .url("http://" + p.host + ":" + p.port + "/play")
                    .post(o.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(req).execute().close()
            } catch (e: Exception) {
            }
        }
    }

    fun cmd(p: Peer, what: String, v: Long = 0L) {
        scope.launch {
            try {
                val req = Request.Builder()
                    .url("http://" + p.host + ":" + p.port + "/cmd?do=" + what + "&v=" + v)
                    .build()
                http.newCall(req).execute().close()
            } catch (e: Exception) {
            }
        }
    }

    // while a device is chosen: read its state every second and follow it
    fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            var fails = 0
            while (true) {
                val p = target ?: break
                try {
                    val req = Request.Builder()
                        .url("http://" + p.host + ":" + p.port + "/state")
                        .build()
                    val txt = http.newCall(req).execute().use { it.body?.string() ?: "" }
                    val o = JSONObject(txt)
                    fails = 0
                    if (o.optBoolean("has", false) && SystemClock.elapsedRealtime() - lastSend > 2500L) {
                        val r = Remote(
                            o.optString("t"), o.optString("a"),
                            if (o.has("c")) o.getString("c") else null,
                            o.optString("u"), o.optBoolean("play", false),
                            o.optLong("pos", 0L), o.optLong("dur", 0L), o.optInt("vol", 50)
                        )
                        remote = r
                        main.post { vm?.applyRemote(r) }
                    }
                } catch (e: Exception) {
                    fails++
                    if (fails >= 4) {
                        main.post { vm?.connectLost(p) }
                        break
                    }
                }
                delay(1000)
            }
        }
    }
}
