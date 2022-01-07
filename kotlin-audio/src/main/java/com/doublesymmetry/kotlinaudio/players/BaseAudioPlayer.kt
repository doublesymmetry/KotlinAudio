package com.doublesymmetry.kotlinaudio.players

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_LOSS
import android.net.Uri
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
import com.doublesymmetry.kotlinaudio.utils.isJUnitTest
import com.doublesymmetry.kotlinaudio.utils.isUriLocal
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultLoadControl.*
import com.google.android.exoplayer2.Player.Listener
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
import timber.log.Timber
import java.util.concurrent.TimeUnit
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor

import com.google.android.exoplayer2.database.ExoDatabaseProvider

import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.flac.VorbisComment
import com.google.android.exoplayer2.metadata.icy.IcyHeaders
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.metadata.id3.UrlLinkFrame
import com.google.android.exoplayer2.metadata.mp4.MdtaMetadataEntry
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import java.io.File


abstract class BaseAudioPlayer internal constructor(private val context: Context, bufferConfig: BufferConfig? = null, private val cacheConfig: CacheConfig? = null) : AudioManager.OnAudioFocusChangeListener {
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

    private var focus: AudioFocusRequestCompat? = null
    private var hasAudioFocus = false
    private var wasDucking = false

    init {
        if (cacheConfig != null) {
            val cacheDir = File(context.cacheDir, "TrackPlayer")
            val db: DatabaseProvider = ExoDatabaseProvider(context)
            cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(cacheConfig.maxCacheSize ?: 0), db)
        }

        exoPlayer = SimpleExoPlayer.Builder(context).apply {
            if (bufferConfig != null) setLoadControl(setupBuffer(bufferConfig))
        }.build()

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
        abandonAudioFocus()
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

    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        Timber.d("Requesting audio focus...")

        val manager = ContextCompat.getSystemService(context, AudioManager::class.java)

        focus = AudioFocusRequestCompat.Builder(AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(this)
            .setAudioAttributes(AudioAttributesCompat.Builder()
                .setUsage(USAGE_MEDIA)
                .setContentType(CONTENT_TYPE_MUSIC)
                .build())
            .setWillPauseWhenDucked(playerOptions.alwaysPauseOnInterruption)
            .build()

        val result: Int = if (manager != null && focus != null) {
            AudioManagerCompat.requestAudioFocus(manager, focus!!)
        } else {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED
        }

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    private fun abandonAudioFocus() {
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
                abandonAudioFocus()
                isPaused = true
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
            /**
             * ID3 Metadata (MP3)
             *
             * https://en.wikipedia.org/wiki/ID3
             */
            fun id3(metadata: Metadata): Boolean {
                var handled = false;
                var title: String? = null
                var url: String? = null
                var artist: String? = null
                var album: String? = null
                var date: String? = null
                var genre: String? = null
                for (i in 0 until metadata.length()) {
                    when (val entry = metadata[i]) {
                        is TextInformationFrame -> {
                            when (entry.id.uppercase()) {
                                "TIT2",
                                "TT2" -> {
                                    handled = true;
                                    title = entry.value
                                }
                                "TALB",
                                "TOAL",
                                "TAL" -> {
                                    handled = true;
                                    album = entry.value
                                }
                                "TOPE",
                                "TPE1",
                                "TP1" -> {
                                    handled = true;
                                    artist = entry.value
                                }
                                "TDRC",
                                "TOR" -> {
                                    handled = true;
                                    date = entry.value
                                }
                                "TCON",
                                "TCO" -> {
                                    handled = true;
                                    genre = entry.value
                                }

                            }
                        }
                        is UrlLinkFrame -> {
                            when (entry.id.uppercase()) {
                                "WOAS",
                                "WOAF",
                                "WOAR",
                                "WAR" -> {
                                    handled = true;
                                    url = entry.url;
                                }
                            }
                        }
                    }
                }
                if (handled) {
                    val playbackMetadata = PlaybackMetadata("id3");
                    playbackMetadata.artist = artist;
                    playbackMetadata.album = album;
                    playbackMetadata.date = date;
                    playbackMetadata.genre = genre;
                    playbackMetadata.title = title;
                    playbackMetadata.url = url;
                    playerEventHolder.updateOnPlaybackMetadata(playbackMetadata);
                }
                return handled;
            }

            /**
             * Shoutcast / Icecast metadata (ICY protocol)
             *
             * https://cast.readme.io/docs/icy
             */
            fun icy(metadata: Metadata): Boolean {
                var handled = false;
                for (i in 0 until metadata.length()) {
                    when (val entry = metadata[i]) {
                        is IcyHeaders -> {
                            handled = true;
                            val playbackMetadata = PlaybackMetadata("icy-headers");
                            playbackMetadata.title = entry.name;
                            playbackMetadata.url = entry.url;
                            playbackMetadata.genre = entry.genre;
                            playerEventHolder.updateOnPlaybackMetadata(playbackMetadata);
                        }
                        is IcyInfo -> {
                            handled = true;
                            var artist: String?
                            var title: String?
                            val index =
                                if (entry.title == null) -1 else entry.title!!.indexOf(" - ")
                            if (index != -1) {
                                artist = entry.title!!.substring(0, index)
                                title = entry.title!!.substring(index + 3)
                            } else {
                                artist = null
                                title = entry.title
                            }
                            val playbackMetadata = PlaybackMetadata("icy");
                            playbackMetadata.title = title;
                            playbackMetadata.url = entry.url;
                            playbackMetadata.artist = artist;
                            playerEventHolder.updateOnPlaybackMetadata(playbackMetadata);
                        }
                    }
                }
                return handled;
            }

            /**
             * Vorbis Comments (Vorbis, FLAC, Opus, Speex, Theora)
             *
             * https://xiph.org/vorbis/doc/v-comment.html
             */
            fun vorbisComment(metadata: Metadata): Boolean {
                var handled = false;
                var title: String? = null
                var url: String? = null
                var artist: String? = null
                var album: String? = null
                var date: String? = null
                var genre: String? = null
                for (i in 0 until metadata.length()) {
                    val entry = metadata[i]
                    if (entry is VorbisComment) {
                        when (entry.key) {
                            "TITLE" -> {
                                handled = true;
                                title = entry.value;
                            }
                            "ARTIST" -> {
                                handled = true;
                                artist = entry.value;
                            }
                            "ALBUM" -> {
                                handled = true;
                                album = entry.value;
                            }
                            "DATE" -> {
                                handled = true;
                                date = entry.value
                            }
                            "GENRE" -> {
                                handled = true;
                                genre = entry.value
                            }
                            "URL" -> {
                                handled = true;
                                url = entry.value
                            }
                        }
                    }
                }
                if (handled) {
                    val playbackMetadata = PlaybackMetadata("vorbis-comment");
                    playbackMetadata.artist = artist;
                    playbackMetadata.album = album;
                    playbackMetadata.date = date;
                    playbackMetadata.genre = genre;
                    playbackMetadata.title = title;
                    playbackMetadata.url = url;
                    playerEventHolder.updateOnPlaybackMetadata(playbackMetadata);
                }
                return handled;
            }

            /**
             * QuickTime MDTA metadata (mov, qt)
             *
             * https://developer.apple.com/library/archive/documentation/QuickTime/QTFF/Metadata/Metadata.html
             */
            fun quicktime(metadata: Metadata): Boolean {
                var handled = false;
                var title: String? = null
                var artist: String? = null
                var album: String? = null
                var date: String? = null
                var genre: String? = null
                for (i in 0 until metadata.length()) {
                    val entry = metadata[i];
                    if (entry is MdtaMetadataEntry) {
                        when (entry.key) {
                            "com.apple.quicktime.title" -> {
                                handled = true;
                                title = entry.value.toString();
                            }
                            "com.apple.quicktime.artist" -> {
                                handled = true;
                                artist = entry.value.toString();
                            }
                            "com.apple.quicktime.album" -> {
                                handled = true;
                                album = entry.value.toString();
                            }
                            "com.apple.quicktime.creationdate" -> {
                                handled = true;
                                date = entry.value.toString();
                            }
                            "com.apple.quicktime.genre" -> {
                                handled = true;
                                genre = entry.value.toString();
                            }
                        }
                    }
                }

                if (handled) {
                    val playbackMetadata = PlaybackMetadata("quicktime");
                    playbackMetadata.artist = artist;
                    playbackMetadata.album = album;
                    playbackMetadata.date = date;
                    playbackMetadata.genre = genre;
                    playbackMetadata.title = title;
                    playerEventHolder.updateOnPlaybackMetadata(playbackMetadata);
                }
                return handled;
            }

            id3(metadata) || icy(metadata) || vorbisComment(metadata) || quicktime(metadata);
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
                requestAudioFocus()
                playerEventHolder.updateAudioPlayerState(AudioPlayerState.PLAYING)
            } else {
                abandonAudioFocus()
                playerEventHolder.updateAudioPlayerState(AudioPlayerState.PAUSED)
            }
        }
    }
}