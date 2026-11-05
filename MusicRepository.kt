package com.neo.glassplayer.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val scanner = MediaStoreScanner(context)

    val songs get() = db.songs()
    val playlists get() = db.playlists()
    val queue get() = db.queue()

    suspend fun rescan(): Int = withContext(Dispatchers.IO) {
        val scanned = try { scanner.scan() } catch (e: SecurityException) { return@withContext -1 }
        db.songs().sync(scanned)
        scanned.size
    }
}
