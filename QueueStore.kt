package com.neo.glassplayer.playback

import android.content.Context
import com.neo.glassplayer.data.MusicRepository

/** Persists the queue (Room) and the position (SharedPreferences). */
class QueueStore(context: Context, private val repo: MusicRepository) {
    private val prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE)

    suspend fun save(ids: List<Long>, index: Int, positionMs: Long, shuffle: Boolean, repeat: Int) {
        repo.queue.replace(ids)
        prefs.edit()
            .putInt("index", index)
            .putLong("pos", positionMs)
            .putBoolean("shuffle", shuffle)
            .putInt("repeat", repeat)
            .apply()
    }

    data class Restored(val ids: List<Long>, val index: Int, val positionMs: Long, val shuffle: Boolean, val repeat: Int)

    suspend fun load(): Restored? {
        val ids = repo.queue.ids()
        if (ids.isEmpty()) return null
        return Restored(
            ids,
            prefs.getInt("index", 0),
            prefs.getLong("pos", 0L),
            prefs.getBoolean("shuffle", false),
            prefs.getInt("repeat", 0),
        )
    }
}
