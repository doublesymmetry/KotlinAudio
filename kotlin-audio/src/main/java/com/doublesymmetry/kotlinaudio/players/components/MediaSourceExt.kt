package com.doublesymmetry.kotlinaudio.players.components

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import com.google.android.exoplayer2.source.MediaSource

fun MediaSource.getMediaMetadataCompat(): MediaMetadataCompat {
    val audioItem = mediaItem.localConfiguration?.tag as com.doublesymmetry.kotlinaudio.models.AudioItem?
    val metadata = mediaItem.mediaMetadata
    val title = metadata.title ?: audioItem?.title
    val artist = metadata.artist ?: audioItem?.artist
    val albumTitle = metadata.albumTitle ?: audioItem?.albumTitle
    val genre = metadata.genre
    val duration = audioItem?.duration ?: -1
    val artwork = metadata.artworkUri ?: audioItem?.artwork
    val rating = RatingCompat.fromRating(metadata.userRating)

    return MediaMetadataCompat.Builder().apply {
        putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist.toString())
        putString(MediaMetadataCompat.METADATA_KEY_TITLE, title.toString())
        putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle.toString())
        putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre.toString())
        putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

        if (artwork != null) {
            putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artwork.toString())
        }

        if (rating != null) {
            putRating(MediaMetadataCompat.METADATA_KEY_RATING, rating)
        }
    }.build()
}