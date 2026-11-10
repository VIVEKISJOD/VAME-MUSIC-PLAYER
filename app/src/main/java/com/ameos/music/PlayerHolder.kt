package com.ameos.music

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

object PlayerHolder {
    private var player: ExoPlayer? = null
    private var audioCache: SimpleCache? = null

    // songs you played are kept on the phone (up to 300 MB, oldest are dropped), so they replay at once
    private fun audioCache(context: Context): SimpleCache? {
        val have = audioCache
        if (have != null) return have
        return try {
            val c = SimpleCache(
                File(context.cacheDir, "audio"),
                LeastRecentlyUsedCacheEvictor(300L * 1024L * 1024L),
                StandaloneDatabaseProvider(context)
            )
            audioCache = c
            c
        } catch (e: Exception) {
            null
        }
    }

    fun clearAudioCache() {
        val c = audioCache ?: return
        try {
            for (k in c.keys.toList()) c.removeResource(k)
        } catch (e: Exception) {
        }
    }

    @Synchronized
    fun get(context: Context): ExoPlayer {
        val existing = player
        if (existing != null) return existing
        val created = build(context.applicationContext)
        player = created
        return created
    }

    private fun build(context: Context): ExoPlayer {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(DownloaderImpl.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)

        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // start after 0.8 s of audio instead of the default 2.5 s; keep a long buffer for steady play
        val load = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 60_000, 800, 2_000)
            .build()

        val store = audioCache(context)
        val upstream: androidx.media3.datasource.DataSource.Factory = if (store != null) {
            // only internet songs go through the cache; downloaded files are read directly
            val cached = CacheDataSource.Factory()
                .setCache(store)
                .setUpstreamDataSourceFactory(http)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            DefaultDataSource.Factory(context, cached)
        } else {
            DefaultDataSource.Factory(context, http)
        }
        val built = ExoPlayer.Builder(context)
            .setLoadControl(load)
            .setMediaSourceFactory(DefaultMediaSourceFactory(upstream))
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        SoundFx.init(context, built)
        return built
    }
}
