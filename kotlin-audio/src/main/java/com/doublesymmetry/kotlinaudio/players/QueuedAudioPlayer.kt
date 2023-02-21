package com.doublesymmetry.kotlinaudio.players

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata.METADATA_KEY_ARTIST
import android.media.MediaMetadata.METADATA_KEY_TITLE
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.IllegalSeekPositionException
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.session.MediaSession
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.doublesymmetry.kotlinaudio.models.*

import java.util.*
import kotlin.math.max
import kotlin.math.min

class QueuedAudioPlayer(context: Context, playerConfig: PlayerConfig = PlayerConfig(), bufferConfig: BufferConfig? = null, cacheConfig: CacheConfig? = null) : BaseAudioPlayer(context, playerConfig, bufferConfig, cacheConfig) {
    private val queue = LinkedList<MediaSource>()
    override val playerOptions = DefaultQueuedPlayerOptions(exoPlayer)

    init {
//        mediaSessionConnector.setQueueNavigator(KotlinAudioQueueNavigator(mediaSession))
//        mediaSessionConnector.setMetadataDeduplicationEnabled(true)
    }

    val currentIndex
        get() = exoPlayer.currentMediaItemIndex

    override val currentItem: AudioItem?
        get() = (queue.getOrNull(currentIndex)?.mediaItem?.localConfiguration?.tag as AudioItemHolder?)?.audioItem

    val nextIndex: Int?
        get() {
            return if (exoPlayer.nextMediaItemIndex == C.INDEX_UNSET) null
            else exoPlayer.nextMediaItemIndex
        }

    val previousIndex: Int?
        get() {
            return if (exoPlayer.previousMediaItemIndex == C.INDEX_UNSET) null
            else exoPlayer.previousMediaItemIndex
        }

    val items: List<AudioItem>
        get() = queue.map { (it.mediaItem.localConfiguration?.tag as AudioItemHolder).audioItem }

    val previousItems: List<AudioItem>
        get() {
            return if (queue.isEmpty()) emptyList()
            else queue
                .subList(0, exoPlayer.currentMediaItemIndex)
                .map { (it.mediaItem.localConfiguration?.tag as AudioItemHolder).audioItem }
        }

    val nextItems: List<AudioItem>
        get() {
            return if (queue.isEmpty()) emptyList()
            else queue
                .subList(exoPlayer.currentMediaItemIndex, queue.lastIndex)
                .map { (it.mediaItem.localConfiguration?.tag as AudioItemHolder).audioItem }
        }

    val nextItem: AudioItem?
        get() = items.getOrNull(currentIndex + 1)

    val previousItem: AudioItem?
        get() = items.getOrNull(currentIndex - 1)

    override fun load(item: AudioItem, playWhenReady: Boolean) {
        exoPlayer.playWhenReady = playWhenReady
        load(item)
    }

    override fun load(item: AudioItem) {
        if (queue.isEmpty()) {
            add(item)
        } else {
            val mediaSource = getMediaSourceFromAudioItem(item)
            queue[currentIndex] = mediaSource
            exoPlayer.addMediaSource(currentIndex + 1, mediaSource)
            exoPlayer.removeMediaItem(currentIndex)
            exoPlayer.prepare()
        }
    }

    /**
     * Add a single item to the queue. If the AudioPlayer has no item loaded, it will load the `item`.
     * @param item The [AudioItem] to add.
     */
    fun add(item: AudioItem, playWhenReady: Boolean) {
        exoPlayer.playWhenReady = playWhenReady
        add(item)
    }

    /**
     * Add a single item to the queue. If the AudioPlayer has no item loaded, it will load the `item`.
     * @param item The [AudioItem] to add.
     * @param playWhenReady Whether playback starts automatically.
     */
    fun add(item: AudioItem) {
        val mediaSource = getMediaSourceFromAudioItem(item)
        queue.add(mediaSource)
        exoPlayer.addMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    /**
     * Add multiple items to the queue. If the AudioPlayer has no item loaded, it will load the first item in the list.
     * @param items The [AudioItem]s to add.
     * @param playWhenReady Whether playback starts automatically.
     */
    fun add(items: List<AudioItem>, playWhenReady: Boolean) {
        exoPlayer.playWhenReady = playWhenReady
        add(items)
    }

    /**
     * Add multiple items to the queue. If the AudioPlayer has no item loaded, it will load the first item in the list.
     * @param items The [AudioItem]s to add.
     */
    fun add(items: List<AudioItem>) {
        val mediaSources = items.map { getMediaSourceFromAudioItem(it) }
        queue.addAll(mediaSources)
        exoPlayer.addMediaSources(mediaSources)
        exoPlayer.prepare()
    }


    /**
     * Add multiple items to the queue.
     * @param items The [AudioItem]s to add.
     * @param atIndex  Index to insert items at, if no items loaded this will not automatically start playback.
     */
    fun add(items: List<AudioItem>, atIndex: Int) {
        val mediaSources = items.map { getMediaSourceFromAudioItem(it) }
        queue.addAll(atIndex, mediaSources)
        exoPlayer.addMediaSources(atIndex, mediaSources)
        exoPlayer.prepare()
    }

    /**
     * Remove an item from the queue.
     * @param index The index of the item to remove.
     */
    fun remove(index: Int) {
        queue.removeAt(index)
        exoPlayer.removeMediaItem(index)
    }

    /**
     * Remove items from the queue.
     * @param indexes The indexes of the items to remove.
     */
    fun remove(indexes: List<Int>) {
        var sorted = indexes.toList()
        // Sort the indexes in descending order so we can safely remove them one by one
        // without having the next index possibly newly pointing to another item than intended:
        Collections.sort(sorted, Collections.reverseOrder());
        sorted.forEach {
            remove(it)
        }
    }

    /**
     * Skip to the next item in the queue, which may depend on the current repeat mode.
     * Does nothing if there is no next item to skip to.
     */
    fun next() {
        exoPlayer.seekToNextMediaItem()
        exoPlayer.prepare()
    }

    /**
     * Skip to the previous item in the queue, which may depend on the current repeat mode.
     * Does nothing if there is no previous item to skip to.
     */
    fun previous() {
        exoPlayer.seekToPreviousMediaItem()
        exoPlayer.prepare()
    }

    /**
     * Move an item in the queue from one position to another.
     * @param fromIndex The index of the item ot move.
     * @param toIndex The index to move the item to. If the index is larger than the size of the queue, the item is moved to the end of the queue instead.
     */
    fun move(fromIndex: Int, toIndex: Int) {
        exoPlayer.moveMediaItem(fromIndex, toIndex)
        var item = queue[fromIndex]
        queue.removeAt(fromIndex)
        queue.add(max(0, min(items.size, if (toIndex > fromIndex) toIndex else toIndex - 1)), item)
    }

    /**
     * Jump to an item in the queue.
     * @param index the index to jump to
     * @param playWhenReady Whether playback starts automatically.
     */
    fun jumpToItem(index: Int, playWhenReady: Boolean) {
        exoPlayer.playWhenReady = playWhenReady
        jumpToItem(index)
    }

    /**
     * Jump to an item in the queue.
     * @param index the index to jump to
     */
    fun jumpToItem(index: Int) {
        try {
            exoPlayer.seekTo(index, C.TIME_UNSET)
            exoPlayer.prepare()
        } catch (e: IllegalSeekPositionException) {
            throw Error("This item index $index does not exist. The size of the queue is ${queue.size} items.")
        }
    }

    /**
     * Replaces item at index in queue.
     * If updating current index, we update the notification metadata if [automaticallyUpdateNotificationMetadata] is true.
     */
    fun replaceItem(index: Int, item: AudioItem) {
        val mediaSource = getMediaSourceFromAudioItem(item)
        queue[index] = mediaSource

        if (currentIndex == index && automaticallyUpdateNotificationMetadata)
            notificationManager.notificationMetadata = NotificationMetadata(item.title, item.artist, item.artwork)
    }

    /**
     * Removes all the upcoming items, if any (the ones returned by [next]).
     */
    fun removeUpcomingItems() {
        val lastIndex = queue.lastIndex
        if (lastIndex == -1) return

        exoPlayer.removeMediaItems(currentIndex, lastIndex)
        queue.subList(currentIndex, lastIndex).clear()
    }

    /**
     * Removes all the previous items, if any (the ones returned by [previous]).
     */
    fun removePreviousItems() {
        exoPlayer.removeMediaItems(0, currentIndex)
        queue.subList(0, currentIndex).clear()
    }

    override fun destroy() {
        queue.clear()
        super.destroy()
    }

    override fun clear() {
        queue.clear()
        super.clear()
    }

//    private inner class KotlinAudioQueueNavigator(mediaSession: MediaSession) : TimelineQueueNavigator(mediaSession) {
//        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
//            val isActive = windowIndex == player.currentMediaItemIndex
//            val mediaItem = queue[windowIndex].mediaItem
//            val audioItemHolder = (mediaItem.localConfiguration?.tag as AudioItemHolder)
//            val audioItem = audioItemHolder.audioItem
//            val metadata = mediaItem.mediaMetadata
//            var title = metadata.title ?: audioItem.title
//            var artist = metadata.artist ?: audioItem.artist
//            var artworkUrl = (audioItem.artwork ?: metadata.artworkUri)?.toString()
//            val notificationMetadata = notificationManager.notificationMetadata
//            if (isActive && notificationMetadata != null) {
//                title = notificationMetadata.title ?: title
//                artist = notificationMetadata.artist ?: artist
//                artworkUrl = notificationMetadata.artworkUrl ?: artworkUrl
//            }
//            if (
//                isActive &&
//                artworkUrl != null &&
//                audioItemHolder.artworkBitmap == null
//            ) {
//                context.imageLoader.enqueue(
//                    ImageRequest.Builder(context)
//                        .data(artworkUrl)
//                        .target {
//                            audioItemHolder.artworkBitmap = (it as BitmapDrawable).bitmap
////                            mediaSessionConnector.invalidateMediaSessionQueue()
////                            mediaSessionConnector.invalidateMediaSessionMetadata()
//                        }
//                        .build()
//                )
//            }
//
//            return MediaDescriptionCompat.Builder().apply {
//                setTitle(title)
//                setSubtitle(artist)
//                if (audioItemHolder.artworkBitmap != null) {
//                    setIconBitmap(audioItemHolder.artworkBitmap)
//                }
//                setExtras(Bundle().apply{
//                    putString(METADATA_KEY_TITLE, title as String?)
//                    putString(METADATA_KEY_ARTIST, artist as String?)
//                })
//            }.build()
//        }
//    }
}
