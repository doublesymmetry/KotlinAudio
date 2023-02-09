package com.doublesymmetry.kotlinaudio.players.components

import android.media.MediaMetadata.METADATA_KEY_ALBUM
import android.media.MediaMetadata.METADATA_KEY_ARTIST
import android.media.MediaMetadata.METADATA_KEY_DURATION
import android.media.MediaMetadata.METADATA_KEY_GENRE
import android.media.MediaMetadata.METADATA_KEY_TITLE
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaMetadata
//import androidx.media3.common.MediaMetadata
import androidx.media3.common.Rating
import androidx.media3.exoplayer.source.MediaSource
import com.doublesymmetry.kotlinaudio.models.AudioItemHolder

fun MediaSource.getMediaMetadata(): MediaMetadata {
    val audioItem = (mediaItem.localConfiguration?.tag as AudioItemHolder?)?.audioItem
    val metadata = mediaItem.mediaMetadata
    val title = metadata.title ?: audioItem?.title
    val artist = metadata.artist ?: audioItem?.artist
    val albumTitle = metadata.albumTitle ?: audioItem?.albumTitle
    val genre = metadata.genre
    val duration = audioItem?.duration ?: -1
    val artwork = metadata.artworkUri ?: audioItem?.artwork
    val rating = metadata.userRating

    return MediaMetadata.Builder().apply {
        setArtist(artist.toString())
        setTitle(title.toString())
        setAlbumTitle(albumTitle.toString())
        setGenre(genre.toString())

        if (artwork != null) {
            setArtworkUri(artwork as Uri?)
        }

        if (rating != null) {
            setUserRating(rating)
        }
    }.build()
}