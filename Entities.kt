package com.neo.glassplayer.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "songs", indices = [Index("albumId"), Index("artist"), Index("folder")])
data class SongEntity(
    @PrimaryKey val id: Long,          // MediaStore audio id
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val folder: String,
    val dateAdded: Long,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val favorite: Boolean = false,
    val missing: Boolean = false,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "position"],
    indices = [Index("songId")],
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val position: Int,
    val songId: Long,
)

@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey val position: Int,
    val songId: Long,
)
