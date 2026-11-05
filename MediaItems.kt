package com.neo.glassplayer.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.neo.glassplayer.data.MediaStoreScanner
import com.neo.glassplayer.data.SongEntity

fun SongEntity.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri.toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(MediaStoreScanner.artworkUri(albumId))
                .build()
        )
        .build()
