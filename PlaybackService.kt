package com.neo.glassplayer.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.neo.glassplayer.GlassApp
import kotlinx.coroutines.*

class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var store: QueueStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var saveJob: Job? = null
    private var consecutiveErrors = 0

    override fun onCreate() {
        super.onCreate()
        val repo = (application as GlassApp).repository
        store = QueueStore(this, repo)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)   // pause on headphone unplug / Bluetooth drop
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                consecutiveErrors = 0
                if (item != null && reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    item.mediaId.toLongOrNull()?.let { id ->
                        scope.launch(Dispatchers.IO) { repo.songs.recordPlay(id, System.currentTimeMillis()) }
                    }
                }
                scheduleSave()
            }

            override fun onPlayerError(error: PlaybackException) {
                // Missing or corrupt file: skip to the next track. Stop after a full pass of failures.
                consecutiveErrors++
                if (consecutiveErrors >= player.mediaItemCount.coerceAtLeast(1)) {
                    player.pause()
                    return
                }
                if (player.hasNextMediaItem()) player.seekToNextMediaItem() else player.seekTo(0, 0)
                player.prepare()
                player.play()
            }

            override fun onShuffleModeEnabledChanged(enabled: Boolean) = scheduleSave()
            override fun onRepeatModeChanged(repeatMode: Int) = scheduleSave()
            override fun onTimelineChanged(t: androidx.media3.common.Timeline, r: Int) = scheduleSave()
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (!isPlaying) scheduleSave() }
        })

        session = MediaSession.Builder(this, player).build()
        restoreQueue(repo)
    }

    private fun restoreQueue(repo: com.neo.glassplayer.data.MusicRepository) {
        scope.launch {
            val saved = withContext(Dispatchers.IO) { store.load() } ?: return@launch
            val byId = withContext(Dispatchers.IO) { repo.songs.getByIds(saved.ids) }.associateBy { it.id }
            val items = saved.ids.mapNotNull { byId[it]?.takeIf { s -> !s.missing }?.toMediaItem() }
            if (items.isEmpty() || player.mediaItemCount > 0) return@launch
            player.shuffleModeEnabled = saved.shuffle
            player.repeatMode = saved.repeat
            player.setMediaItems(items, saved.index.coerceIn(0, items.lastIndex), saved.positionMs)
            player.prepare()          // stays paused until the user presses play
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(500)               // debounce rapid queue edits
            val ids = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
            val index = player.currentMediaItemIndex
            val pos = player.currentPosition
            val shuffle = player.shuffleModeEnabled
            val repeat = player.repeatMode
            withContext(Dispatchers.IO) { store.save(ids, index, pos, shuffle, repeat) }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        scheduleSaveNow()
        session?.run { player.release(); release() }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    private fun scheduleSaveNow() {
        val ids = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        val index = player.currentMediaItemIndex
        val pos = player.currentPosition
        val shuffle = player.shuffleModeEnabled
        val repeat = player.repeatMode
        // Blocking write is acceptable here: tiny payload, service is shutting down.
        runBlocking(Dispatchers.IO) { store.save(ids, index, pos, shuffle, repeat) }
    }
}
