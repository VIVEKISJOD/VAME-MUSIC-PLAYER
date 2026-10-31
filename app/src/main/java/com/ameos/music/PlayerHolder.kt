package com.ameos.music

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

object PlayerHolder {
    private var player: ExoPlayer? = null

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

        val built = ExoPlayer.Builder(context)
            .setLoadControl(load)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        SoundFx.init(context, built)
        return built
    }
}
