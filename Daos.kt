package com.neo.glassplayer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE missing = 0 ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE missing = 0 AND favorite = 1 ORDER BY title COLLATE NOCASE")
    fun observeFavorites(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE missing = 0 AND lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int = 20): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE missing = 0 ORDER BY dateAdded DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 20): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE missing = 0 AND playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int = 20): Flow<List<SongEntity>>

    @Query(
        "SELECT * FROM songs WHERE missing = 0 AND " +
            "(title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' OR album LIKE '%' || :q || '%') " +
            "ORDER BY title COLLATE NOCASE LIMIT 200"
    )
    fun search(q: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT id FROM songs")
    suspend fun allIds(): List<Long>

    // Insert new rows; keep user data (playCount, favorite) on existing rows.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(songs: List<SongEntity>)

    @Query(
        "UPDATE songs SET title = :title, artist = :artist, album = :album, albumId = :albumId, " +
            "durationMs = :durationMs, uri = :uri, folder = :folder, missing = 0 WHERE id = :id"
    )
    suspend fun updateMeta(
        id: Long, title: String, artist: String, album: String, albumId: Long,
        durationMs: Long, uri: String, folder: String,
    )

    @Query("UPDATE songs SET missing = 1 WHERE id IN (:ids)")
    suspend fun markMissing(ids: List<Long>)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :now WHERE id = :id")
    suspend fun recordPlay(id: Long, now: Long)

    @Query("UPDATE songs SET favorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Transaction
    suspend fun sync(scanned: List<SongEntity>) {
        val existing = allIds().toHashSet()
        val scannedIds = scanned.map { it.id }.toHashSet()
        insertIgnore(scanned.filter { it.id !in existing })
        scanned.filter { it.id in existing }.forEach {
            updateMeta(it.id, it.title, it.artist, it.album, it.albumId, it.durationMs, it.uri, it.folder)
        }
        val gone = existing.filter { it !in scannedIds }
        gone.chunked(500).forEach { markMissing(it) }
    }
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query(
        "SELECT s.* FROM playlist_items i JOIN songs s ON s.id = i.songId " +
            "WHERE i.playlistId = :playlistId AND s.missing = 0 ORDER BY i.position"
    )
    fun observeSongs(playlistId: Long): Flow<List<SongEntity>>

    @Insert suspend fun insert(p: PlaylistEntity): Long
    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)
    @Query("DELETE FROM playlist_items WHERE playlistId = :id")
    suspend fun clearItems(id: Long)

    @Query("SELECT songId FROM playlist_items WHERE playlistId = :id ORDER BY position")
    suspend fun songIds(id: Long): List<Long>

    @Insert suspend fun insertItems(items: List<PlaylistItemEntity>)

    /** Rewrites the whole ordered list. Keeps positions contiguous and consistent. */
    @Transaction
    suspend fun setSongs(playlistId: Long, ids: List<Long>) {
        clearItems(playlistId)
        insertItems(ids.mapIndexed { i, s -> PlaylistItemEntity(playlistId, i, s) })
    }

    @Transaction
    suspend fun delete(id: Long) {
        clearItems(id)
        deletePlaylist(id)
    }
}

@Dao
interface QueueDao {
    @Query("SELECT songId FROM queue ORDER BY position")
    suspend fun ids(): List<Long>
    @Query("DELETE FROM queue")
    suspend fun clear()
    @Insert suspend fun insertAll(items: List<QueueEntity>)

    @Transaction
    suspend fun replace(ids: List<Long>) {
        clear()
        insertAll(ids.mapIndexed { i, s -> QueueEntity(i, s) })
    }
}
