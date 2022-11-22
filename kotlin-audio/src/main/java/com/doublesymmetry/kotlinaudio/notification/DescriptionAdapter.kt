package com.doublesymmetry.kotlinaudio.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.doublesymmetry.kotlinaudio.models.AudioItem
import com.doublesymmetry.kotlinaudio.models.AudioItemHolder
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager

interface NotificationMetadataProvider {
    fun getTitle(): String?
    fun getArtist(): String?
    fun getArtworkUrl(): String?
}

/**
 * Provides content assets of the media currently playing. If certain data is missing from [AudioItem], data from the media file's metadata is used instead.
 * @param context Some Android [Context].
 * @param pendingIntent The [PendingIntent] that should be fired when the notification is tapped.
 */
class DescriptionAdapter(private val metadataProvider: NotificationMetadataProvider, private val context: Context, private val pendingIntent: PendingIntent?): PlayerNotificationManager.MediaDescriptionAdapter {
    private var disposable: Disposable? = null

    override fun getCurrentContentTitle(player: Player): CharSequence {
        return metadataProvider.getTitle() ?: player.mediaMetadata.displayTitle ?: ""
    }

    override fun createCurrentContentIntent(player: Player): PendingIntent? {
        return pendingIntent
    }

    override fun getCurrentContentText(player: Player): CharSequence? {
        return metadataProvider.getArtist() ?: player.mediaMetadata.artist ?: player.mediaMetadata.albumArtist
    }

    override fun getCurrentSubText(player: Player): CharSequence? {
        return metadataProvider.getTitle() ?: player.mediaMetadata.displayTitle ?: ""
    }

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback,
    ): Bitmap? {
        val itemHolder = player.currentMediaItem?.localConfiguration?.tag as AudioItemHolder?
            ?: return null
        val data = player.mediaMetadata.artworkData
        val source = metadataProvider.getArtworkUrl() ?: player.mediaMetadata.artworkUri

        if (data != null) {
            return BitmapFactory.decodeByteArray(data, 0, data.size)
        }

        if (source == null) {
            return null
        }
        if (itemHolder.artworkBitmap != null) {
            return itemHolder.artworkBitmap
        }
        var hadBitmapOnTime = false
        disposable = context.imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(source)
                .target {
                    itemHolder.artworkBitmap = (it as BitmapDrawable).bitmap
                    // If getCurrentLargeIcon returned the placeholder before we could pass back
                    // the artwork bitmap use the onBitmap callback to set the notification
                    // after the fact:
                    if (!hadBitmapOnTime) {
                        callback.onBitmap(it.bitmap)
                    }
                }
                .build()
        )
        hadBitmapOnTime = itemHolder.artworkBitmap != null
        return itemHolder.artworkBitmap ?: getLargeIconPlaceholder()
    }

    private fun getLargeIconPlaceholder(): Bitmap {
        val placeholderImage = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        placeholderImage.eraseColor(Color.DKGRAY)
        return placeholderImage
    }

    fun release() {
        disposable?.dispose()
        disposable = null
    }
}