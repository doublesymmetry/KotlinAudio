package com.doublesymmetry.kotlinaudio.players.components

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import com.doublesymmetry.kotlinaudio.models.AudioItemHolder
import com.doublesymmetry.kotlinaudio.players.components.getMediaMetadataCompat
import com.doublesymmetry.kotlinaudio.players.components.getAudioItemHolder
import com.google.android.exoplayer2.MediaItem
import timber.log.Timber

fun MediaItem.getAudioItemHolder(): AudioItemHolder {
    return localConfiguration!!.tag as AudioItemHolder
}

fun MediaItem.getMediaMetadataCompat(): MediaMetadataCompat {
    val holder = getAudioItemHolder()
    val audioItem = holder?.audioItem
    val metadata = mediaMetadata
    val title = metadata.title ?: audioItem?.title
    val artist = metadata.artist ?: audioItem?.artist
    val albumTitle = metadata.albumTitle ?: audioItem?.albumTitle
    val genre = metadata.genre
    val duration = audioItem?.duration ?: -1
    val artwork = metadata.artworkUri ?: audioItem?.artwork
    val rating = RatingCompat.fromRating(metadata.userRating)
    val bitmap = holder?.artworkBitmap;
    return MediaMetadataCompat.Builder().apply {
        putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist.toString())
        putString(MediaMetadataCompat.METADATA_KEY_TITLE, title.toString())
        putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle.toString())
        putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre.toString())
        putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

        if (artwork != null) {
            putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artwork.toString())
        }

        if (bitmap != null) {
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
            putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap);
        }

        if (rating != null) {
            putRating(MediaMetadataCompat.METADATA_KEY_RATING, rating)
        }
    }.build()
}