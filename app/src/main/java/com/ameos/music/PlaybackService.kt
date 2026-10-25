package com.ameos.music

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

object PlaybackBridge {
    var onNext: (() -> Unit)? = null
    var onPrev: (() -> Unit)? = null
    var onToggle: (() -> Unit)? = null
}

private class QueuePlayer(p: Player) : ForwardingPlayer(p) {
    private val extra = intArrayOf(
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
    )

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands().buildUpon().addAll(*extra).build()

    override fun isCommandAvailable(command: Int): Boolean =
        extra.contains(command) || super.isCommandAvailable(command)

    override fun seekToNext() {
        PlaybackBridge.onNext?.invoke()
    }

    override fun seekToNextMediaItem() {
        PlaybackBridge.onNext?.invoke()
    }

    override fun seekToPrevious() {
        PlaybackBridge.onPrev?.invoke()
    }

    override fun seekToPreviousMediaItem() {
        PlaybackBridge.onPrev?.invoke()
    }
}

class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val open = Intent(this, MainActivity::class.java)
        open.putExtra("open_player", true)
        open.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pi = PendingIntent.getActivity(
            this, 0, open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val s = MediaSession.Builder(this, QueuePlayer(PlayerHolder.get(this)))
            .setSessionActivity(pi)
            .build()
        session = s
        addSession(s)
    }

    // swiped away from recents: stop the music and close the whole app (if the setting is on)
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!Prefs.stopOnSwipe(this)) {
            super.onTaskRemoved(rootIntent)
            return
        }
        val p = PlayerHolder.get(this)
        p.pause()
        p.stop()
        p.clearMediaItems()
        stopSelf()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    // Keep the player notification (Xiaomi island / lock screen) alive while paused.
    // Normally Android drops it after a while of pause; staying "foreground" prevents that.
    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired || Prefs.keepAlive(this))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
