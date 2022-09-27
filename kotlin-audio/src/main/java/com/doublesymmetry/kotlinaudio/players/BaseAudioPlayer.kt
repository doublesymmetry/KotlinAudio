package com.doublesymmetry.kotlinaudio.players

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_LOSS
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC
import androidx.media.AudioAttributesCompat.USAGE_MEDIA
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media.AudioManagerCompat.AUDIOFOCUS_GAIN
import com.doublesymmetry.kotlinaudio.event.EventHolder
import com.doublesymmetry.kotlinaudio.event.NotificationEventHolder
import com.doublesymmetry.kotlinaudio.event.PlayerEventHolder
import com.doublesymmetry.kotlinaudio.models.*
import com.doublesymmetry.kotlinaudio.notification.NotificationManager
import com.doublesymmetry.kotlinaudio.players.components.PlayerCache
import com.doublesymmetry.kotlinaudio.players.components.getMediaMetadataCompat
import com.doublesymmetry.kotlinaudio.utils.isUriLocal
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultLoadControl.*
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

abstract class BaseAudioPlayer internal constructor(private val context: Context, playerConfig: PlayerConfig, private val bufferConfig: BufferConfig?, private val cacheConfig: CacheConfig?) : AudioManager.OnAudioFocusChangeListener {
    protected val exoPlayer: ExoPlayer
    private val forwardingPlayer: ForwardingPlayer

    private var cache: SimpleCache? = null
    private val scope = MainScope()

    val notificationManager: NotificationManager

    open val playerOptions: PlayerOptions = DefaultPlayerOptions()

    open val currentItem: AudioItem?
        get() = exoPlayer.currentMediaItem?.localConfiguration?.tag as AudioItem?

    var playerState: AudioPlayerState = AudioPlayerState.IDLE
        private set(value) {
            if (value != field) {
                field = value
                playerEventHolder.updateAudioPlayerState(value)
            }
        }

    val duration: Long
        get() {
            return if (exoPlayer.duration == C.TIME_UNSET) 0
            else exoPlayer.duration
        }

    private var oldPosition = 0L

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

    var ratingType: Int = RatingCompat.RATING_NONE
        set(value) {
            field = value

            mediaSession.setRatingType(ratingType)
            mediaSessionConnector.setRatingCallback(object : MediaSessionConnector.RatingCallback {
                override fun onCommand(player: Player, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean {
                    return true
                }

                override fun onSetRating(player: Player, rating: RatingCompat) {
                    playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.RATING(rating, null))
                }

                override fun onSetRating(player: Player, rating: RatingCompat, extras: Bundle?) {
                    playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.RATING(rating, extras))
                }
            })
        }

    val event = EventHolder(notificationEventHolder, playerEventHolder)

    private var focus: AudioFocusRequestCompat? = null
    private var hasAudioFocus = false
    private var wasDucking = false

    protected val mediaSession = MediaSessionCompat(context, "KotlinAudioPlayer")
    protected val mediaSessionConnector = MediaSessionConnector(mediaSession)

    init {
        if (cacheConfig != null) {
            cache = PlayerCache.getInstance(context, cacheConfig)
        }

        exoPlayer = ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(playerConfig.handleAudioBecomingNoisy)
            .apply {
                if (bufferConfig != null) setLoadControl(setupBuffer(bufferConfig))
            }
            .build()
        forwardingPlayer = createForwardingPlayer()

        mediaSession.isActive = true

        val playerToUse = if (playerConfig.interceptPlayerActionsTriggeredExternally) forwardingPlayer else exoPlayer

        notificationManager = NotificationManager(context, playerToUse, mediaSession.sessionToken, notificationEventHolder)

        exoPlayer.addListener(PlayerListener())

        scope.launch {
            mediaSessionConnector.setPlayer(playerToUse)
        }
    }

    private fun createForwardingPlayer(): ForwardingPlayer {
        return object : ForwardingPlayer(exoPlayer) {
            override fun play() {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.PLAY)
            }

            override fun pause() {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.PAUSE)
            }

            override fun seekToNext() {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.NEXT)
            }

            override fun seekToPrevious() {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.PREVIOUS)
            }

            override fun seekForward() {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.FORWARD)
            }

            override fun seekBack() {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.REWIND)
            }

            override fun stop() {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.STOP)
            }

            override fun seekTo(positionMs: Long) {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.SEEK(positionMs))
            }
        }
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
    }

    fun pause() {
        exoPlayer.pause()
    }

    /**
     * Stops playback, resetting the player and the queue. To use the player again, simply add a new [AudioItem].
     */
    open fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    /**
     * Stops and destroys the player. Only call this when you are finished using the player, otherwise use [pause].
     */
    @CallSuper
    open fun destroy() {
        abandonAudioFocusIfHeld()
        stop()
        notificationManager.destroy()
        exoPlayer.release()
        cache?.release()
        cache = null
        mediaSession.isActive = false
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

    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        Timber.d("Requesting audio focus...")

        val manager = ContextCompat.getSystemService(context, AudioManager::class.java)

        focus = AudioFocusRequestCompat.Builder(AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(this)
            .setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setUsage(USAGE_MEDIA)
                    .setContentType(CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setWillPauseWhenDucked(playerOptions.alwaysPauseOnInterruption)
            .build()

        val result: Int = if (manager != null && focus != null) {
            AudioManagerCompat.requestAudioFocus(manager, focus!!)
        } else {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED
        }

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    private fun abandonAudioFocusIfHeld() {
        if (!hasAudioFocus) return
        Timber.d("Abandoning audio focus...")

        val manager = ContextCompat.getSystemService(context, AudioManager::class.java)

        val result: Int = if (manager != null && focus != null) {
            AudioManagerCompat.abandonAudioFocusRequest(manager, focus!!)
        } else {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED
        }

        hasAudioFocus = (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Timber.d("Audio focus changed")

        var isPermanent = false
        var isPaused = false
        var isDucking = false

        when (focusChange) {
            AUDIOFOCUS_LOSS -> {
                isPermanent = true
                isPaused = true
                abandonAudioFocusIfHeld()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> isPaused = true
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (playerOptions.alwaysPauseOnInterruption) isPaused = true else isDucking = true
        }

        if (isDucking) {
            volumeMultiplier = 0.5f
            wasDucking = true
        } else if (wasDucking) {
            volumeMultiplier = 1f
            wasDucking = false
        }

        playerEventHolder.updateOnAudioFocusChanged(isPaused, isPermanent)
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
            playerState = when (playbackState) {
                Player.STATE_BUFFERING -> if (exoPlayer.playWhenReady) AudioPlayerState.BUFFERING else AudioPlayerState.LOADING
                Player.STATE_READY -> {
                    requestAudioFocus()
                    AudioPlayerState.READY
                }
                Player.STATE_IDLE -> {
                    abandonAudioFocusIfHeld()
                    AudioPlayerState.IDLE
                }
                Player.STATE_ENDED -> AudioPlayerState.ENDED
                else -> {
                    Timber.e("Unknown playback state: $playbackState")
                    AudioPlayerState.IDLE
                }
            }
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            this@BaseAudioPlayer.oldPosition = oldPosition.positionMs

            when (reason) {
                Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> playerEventHolder.updatePositionChangedReason(PositionChangedReason.AUTO(oldPosition.positionMs, newPosition.positionMs))
                Player.DISCONTINUITY_REASON_SEEK -> playerEventHolder.updatePositionChangedReason(PositionChangedReason.SEEK(oldPosition.positionMs, newPosition.positionMs))
                Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> playerEventHolder.updatePositionChangedReason(PositionChangedReason.SEEK_FAILED(oldPosition.positionMs, newPosition.positionMs))
                Player.DISCONTINUITY_REASON_REMOVE -> playerEventHolder.updatePositionChangedReason(PositionChangedReason.QUEUE_CHANGED(oldPosition.positionMs, newPosition.positionMs))
                Player.DISCONTINUITY_REASON_SKIP -> playerEventHolder.updatePositionChangedReason(PositionChangedReason.SKIPPED_PERIOD(oldPosition.positionMs, newPosition.positionMs))
                Player.DISCONTINUITY_REASON_INTERNAL -> playerEventHolder.updatePositionChangedReason(PositionChangedReason.UNKNOWN(oldPosition.positionMs, newPosition.positionMs))
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            when (reason) {
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> playerEventHolder.updateAudioItemTransition(AudioItemTransitionReason.AUTO(oldPosition))
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> playerEventHolder.updateAudioItemTransition(AudioItemTransitionReason.QUEUE_CHANGED(oldPosition))
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> playerEventHolder.updateAudioItemTransition(AudioItemTransitionReason.REPEAT(oldPosition))
                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> playerEventHolder.updateAudioItemTransition(AudioItemTransitionReason.SEEK_TO_ANOTHER_AUDIO_ITEM(oldPosition))
            }

            if (automaticallyUpdateNotificationMetadata) {
                notificationManager.notificationMetadata = NotificationMetadata(currentItem?.title, currentItem?.artist, currentItem?.artwork)
            }

            mediaSessionConnector.setMediaMetadataProvider {
                val mediaSource = currentItem?.let { item -> getMediaSourceFromAudioItem(item) }
                mediaSource?.getMediaMetadataCompat() ?: MediaMetadataCompat.Builder().build()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // Unless ExoPlayer is in STATE_READY then it is either idle, buffering or the media has ended
            // Without this check these other state will be immediately overridden by PLAYING/PAUSED
            if (exoPlayer.playbackState != Player.STATE_READY) {
                return
            }

            playerState = if (isPlaying) AudioPlayerState.PLAYING else AudioPlayerState.PAUSED
            playerEventHolder.updateAudioPlayerState(
                if (isPlaying) AudioPlayerState.PLAYING else AudioPlayerState.PAUSED
            )
        }
    }
}
