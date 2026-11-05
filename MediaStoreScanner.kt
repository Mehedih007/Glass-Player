package com.neo.glassplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

/** Reads audio from MediaStore. Runs off the main thread; caller supplies the dispatcher. */
class MediaStoreScanner(private val context: Context) {

    fun scan(): List<SongEntity> {
        val out = ArrayList<SongEntity>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        context.contentResolver.query(collection, projection, selection, null, null)?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val data = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val added = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val songId = c.getLong(id)
                val path = c.getString(data).orEmpty()
                val rawArtist = c.getString(artist).orEmpty()
                out += SongEntity(
                    id = songId,
                    title = c.getString(title)?.takeIf { it.isNotBlank() } ?: File(path).nameWithoutExtension,
                    artist = if (rawArtist == "<unknown>" || rawArtist.isBlank()) "Unknown artist" else rawArtist,
                    album = c.getString(album)?.takeIf { it.isNotBlank() } ?: "Unknown album",
                    albumId = c.getLong(albumId),
                    durationMs = c.getLong(dur),
                    uri = ContentUris.withAppendedId(collection, songId).toString(),
                    folder = File(path).parent.orEmpty(),
                    dateAdded = c.getLong(added),
                )
            }
        }
        return out
    }

    companion object {
        fun artworkUri(albumId: Long): Uri =
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    }
}
