package com.doublesymmetry.kotlinaudio.players

import android.content.Context
import android.net.Uri
import androidx.annotation.CallSuper
import com.doublesymmetry.kotlinaudio.event.EventHolder
import com.doublesymmetry.kotlinaudio.event.NotificationEventHolder
import com.doublesymmetry.kotlinaudio.event.PlayerEventHolder
import com.doublesymmetry.kotlinaudio.models.*
import com.doublesymmetry.kotlinaudio.notification.NotificationManager
import com.doublesymmetry.kotlinaudio.utils.isJUnitTest
import com.doublesymmetry.kotlinaudio.utils.isUriLocal
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultLoadControl.*
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import java.util.concurrent.TimeUnit
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import java.io.File


abstract class BaseAudioPlayer internal constructor(private val context: Context, bufferConfig: BufferConfig? = null, private val cacheConfig: CacheConfig? = null) {
    protected val exoPlayer: SimpleExoPlayer
    private var cache: SimpleCache? = null

    val notificationManager: NotificationManager

    open val playerOptions: PlayerOptions = PlayerOptionsImpl()

    open val currentItem: AudioItem?
        get() = exoPlayer.currentMediaItem?.playbackProperties?.tag as AudioItem?

    val duration: Long
        get() {
            return if (exoPlayer.duration == C.TIME_UNSET) 0
            else exoPlayer.duration
        }

    val position: Long
        get() {
            return if (exoPlayer.currentPosition == C.POSITION_UNSET.toLong()) 0
            else exoPlayer.currentPosition
        }

    val bufferedPosition: Long
        get() {
            return if (exoPlayer.bufferedPosition == C.POSITION_UNSET.toLong()) 0
            else exoPlayer.bufferedPosition
        }

    var volume: Float
        get() = exoPlayer.volume
        set(value) {
            exoPlayer.volume = value * volumeMultiplier
        }

    var playbackSpeed: Float
        get() = exoPlayer.playbackParameters.speed
        set(value) {
            exoPlayer.setPlaybackSpeed(value)
        }

    var automaticallyUpdateNotificationMetadata: Boolean = true

    private var volumeMultiplier = 1f
        private set(value) {
            field = value
            volume = volume
        }

    val isPlaying
        get() = exoPlayer.isPlaying

    private val notificationEventHolder = NotificationEventHolder()
    private val playerEventHolder = PlayerEventHolder()

    val event = EventHolder(notificationEventHolder, playerEventHolder)

    init {
        if (cacheConfig != null) {
            val cacheDir = File(context.cacheDir, "TrackPlayer")
            val db: DatabaseProvider = ExoDatabaseProvider(context)
            cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(cacheConfig.maxCacheSize ?: 0), db)
        }

        exoPlayer = SimpleExoPlayer.Builder(context).apply {
            if (bufferConfig != null) setLoadControl(setupBuffer(bufferConfig))
        }.build()

        // Have ExoPlayer manage audio focus for us
        // see https://medium.com/google-exoplayer/easy-audio-focus-with-exoplayer-a2dcbbe4640e
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build();
        exoPlayer.setAudioAttributes(audioAttributes,true);

        notificationManager = NotificationManager(context, exoPlayer, notificationEventHolder)

        if (isJUnitTest()) {
            exoPlayer.setThrowsWhenUsingWrongThread(false)
        }

        exoPlayer.addListener(PlayerListener())
    }

    private fun setupBuffer(bufferConfig: BufferConfig): DefaultLoadControl {
        bufferConfig.apply {
            val multiplier = DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / DEFAULT_BUFFER_FOR_PLAYBACK_MS
            val minBuffer = if (minBuffer != null && minBuffer != 0) minBuffer else DEFAULT_MIN_BUFFER_MS
            val maxBuffer = if (maxBuffer != null && maxBuffer != 0) maxBuffer else DEFAULT_MAX_BUFFER_MS
            val playBuffer = if (playBuffer != null && playBuffer != 0) playBuffer else DEFAULT_BUFFER_FOR_PLAYBACK_MS
            val backBuffer = if (backBuffer != null && backBuffer != 0) backBuffer else DEFAULT_BACK_BUFFER_DURATION_MS

            return Builder()
                .setBufferDurationsMs(minBuffer, maxBuffer, playBuffer, playBuffer * multiplier)
                .setBackBuffer(backBuffer, false)
                .build()
        }
    }

    /**
     * Will replace the current item with a new one and load it into the player.
     * @param item The [AudioItem] to replace the current one.
     * @param playWhenReady If this is `true` it will automatically start playback. Default is `true`.
     */
    open fun load(item: AudioItem, playWhenReady: Boolean = true) {
        val mediaSource = getMediaSourceFromAudioItem(item)

        exoPlayer.addMediaSource(mediaSource)
        exoPlayer.playWhenReady = playWhenReady
        exoPlayer.prepare()
    }

    fun togglePlaying() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun play() {
        exoPlayer.play()
        notificationManager.onPlay()
    }

    fun pause() {
        exoPlayer.pause()
    }

    /**
     * Stops playback, resetting the player and queue.
     */
    open fun stop() {
        exoPlayer.stop(true)
        exoPlayer.pause()
    }

    /**
     * Stops and destroys the player. Only call this when you are finished using the player, otherwise use [pause].
     */
    @CallSuper
    open fun destroy() {
        exoPlayer.release()
        notificationManager.destroy()
        cache?.release()
    }

    fun seek(duration: Long, unit: TimeUnit) {
        val millis = TimeUnit.MILLISECONDS.convert(duration, unit)

        exoPlayer.seekTo(millis)
    }

    private fun getMediaItemFromAudioItem(audioItem: AudioItem): MediaItem {
        return MediaItem.Builder().setUri(audioItem.audioUrl).setTag(audioItem).build()
    }

    protected fun getMediaSourceFromAudioItem(audioItem: AudioItem): MediaSource {
        val factory: DataSource.Factory
        val uri = Uri.parse(audioItem.audioUrl)
        val mediaItem = getMediaItemFromAudioItem(audioItem)

        val userAgent = if (audioItem.options == null || audioItem.options!!.userAgent.isNullOrBlank()) {
            Util.getUserAgent(context, APPLICATION_NAME)
        } else {
            audioItem.options!!.userAgent
        }

        factory = when {
            audioItem.options?.resourceId != null -> {
                val raw = RawResourceDataSource(context)
                raw.open(DataSpec(uri))
                DataSource.Factory { raw }
            }
            isUriLocal(uri) -> {
                DefaultDataSourceFactory(context, userAgent)
            }
            else -> {
                val tempFactory = DefaultHttpDataSource.Factory().apply {
                    setUserAgent(userAgent)
                    setAllowCrossProtocolRedirects(true)

                    audioItem.options?.headers?.let {
                        setDefaultRequestProperties(it.toMap())
                    }
                }

                enableCaching(tempFactory)
            }
        }

        return when (audioItem.type) {
            MediaType.DASH -> createDashSource(mediaItem, factory)
            MediaType.HLS -> createHlsSource(mediaItem, factory)
            MediaType.SMOOTH_STREAMING -> createSsSource(mediaItem, factory)
            else -> createProgressiveSource(mediaItem, factory)
        }
    }

    private fun createDashSource(mediaItem: MediaItem, factory: DataSource.Factory?): MediaSource {
        return DashMediaSource.Factory(DefaultDashChunkSource.Factory(factory!!), factory)
            .createMediaSource(mediaItem)
    }

    private fun createHlsSource(mediaItem: MediaItem, factory: DataSource.Factory?): MediaSource {
        return HlsMediaSource.Factory(factory!!)
            .createMediaSource(mediaItem)
    }

    private fun createSsSource(mediaItem: MediaItem, factory: DataSource.Factory?): MediaSource {
        return SsMediaSource.Factory(DefaultSsChunkSource.Factory(factory!!), factory)
            .createMediaSource(mediaItem)
    }

    private fun createProgressiveSource(mediaItem: MediaItem, factory: DataSource.Factory): ProgressiveMediaSource {
        return ProgressiveMediaSource.Factory(
            factory, DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)
        )
            .createMediaSource(mediaItem)
    }

    private fun enableCaching(factory: DataSource.Factory): DataSource.Factory {
        return if (cache == null || cacheConfig == null || (cacheConfig.maxCacheSize ?: 0) <= 0) {
            factory
        } else {
            CacheDataSource.Factory().apply {
                setCache(this@BaseAudioPlayer.cache!!)
                setUpstreamDataSourceFactory(factory)
                setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            }
        }
    }

    companion object {
        const val APPLICATION_NAME = "react-native-track-player"
    }

    inner class PlayerListener : Listener {
        override fun onMetadata(metadata: Metadata) {
            PlaybackMetadata.fromId3Metadata(metadata)?.let { playerEventHolder.updateOnPlaybackMetadata(it) }
            PlaybackMetadata.fromIcy(metadata)?.let { playerEventHolder.updateOnPlaybackMetadata(it) }
            PlaybackMetadata.fromVorbisComment(metadata)?.let { playerEventHolder.updateOnPlaybackMetadata(it) }
            PlaybackMetadata.fromQuickTime(metadata)?.let { playerEventHolder.updateOnPlaybackMetadata(it) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> playerEventHolder.updateAudioPlayerState(if (exoPlayer.playWhenReady) AudioPlayerState.BUFFERING else AudioPlayerState.LOADING)
                Player.STATE_READY -> playerEventHolder.updateAudioPlayerState(AudioPlayerState.READY)
                Player.STATE_IDLE -> playerEventHolder.updateAudioPlayerState(AudioPlayerState.IDLE)
                Player.STATE_ENDED -> playerEventHolder.updateAudioPlayerState(AudioPlayerState.ENDED)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            when (reason) {
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> playerEventHolder.updateAudioItemTransition(AudioItemTransitionReason.AUTO)
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> playerEventHolder.updateAudioItemTransition(AudioItemTransitionReason.QUEUE_CHANGED)
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> playerEventHolder.updateAudioItemTransition(AudioItemTransitionReason.REPEAT)
                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> playerEventHolder.updateAudioItemTransition(AudioItemTransitionReason.SEEK_TO_ANOTHER_AUDIO_ITEM)
            }

            if (automaticallyUpdateNotificationMetadata)
                notificationManager.notificatioMetadata = NotificationMetadata(currentItem?.title, currentItem?.artist, currentItem?.artwork)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                playerEventHolder.updateAudioPlayerState(AudioPlayerState.PLAYING)
            } else {
                playerEventHolder.updateAudioPlayerState(AudioPlayerState.PAUSED)
            }
        }
    }
}