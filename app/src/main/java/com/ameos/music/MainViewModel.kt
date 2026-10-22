package com.ameos.music

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val player = PlayerHolder.get(app)

    var query by mutableStateOf("")
    var results by mutableStateOf<List<Song>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var current by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(e: PlaybackException) {
                error = "Play nahi hua: " + (e.message ?: e.errorCodeName)
            }
        })
        isPlaying = player.isPlaying
    }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                results = MusicRepo.search(q)
                if (results.isEmpty()) error = "Kuch nahi mila"
            } catch (e: Exception) {
                error = "Search fail: " + (e.message ?: e.javaClass.simpleName)
            }
            loading = false
        }
    }

    fun play(song: Song) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val streamUrl = MusicRepo.audioUrl(song.url)
                val meta = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .apply {
                        if (song.cover != null) setArtworkUri(Uri.parse(song.cover))
                    }
                    .build()
                val item = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaMetadata(meta)
                    .build()
                player.setMediaItem(item)
                player.prepare()
                player.play()
                current = song
                val ctx = getApplication<Application>()
                ctx.startService(Intent(ctx, PlaybackService::class.java))
            } catch (e: Exception) {
                error = "Play fail: " + (e.message ?: e.javaClass.simpleName)
            }
            loading = false
        }
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }
}
