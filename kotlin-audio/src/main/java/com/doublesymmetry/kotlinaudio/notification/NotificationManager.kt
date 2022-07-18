package com.doublesymmetry.kotlinaudio.notification

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import com.doublesymmetry.kotlinaudio.R
import com.doublesymmetry.kotlinaudio.event.NotificationEventHolder
import com.doublesymmetry.kotlinaudio.models.*
import com.doublesymmetry.kotlinaudio.utils.isJUnitTest
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.util.Log


class NotificationManager internal constructor(private val context: Context, private val exoPlayer: ExoPlayer, private val event: NotificationEventHolder) :
    PlayerNotificationManager.NotificationListener {
    private lateinit var descriptionAdapter: DescriptionAdapter
    private var internalManager: PlayerNotificationManager? = null

    private val mediaSession: MediaSessionCompat = MediaSessionCompat(context, "AudioPlayerSession")
    private val mediaSessionConnector: MediaSessionConnector = MediaSessionConnector(mediaSession)

    private val scope = CoroutineScope(Dispatchers.Main)

    private val buttons = mutableSetOf<NotificationButton?>()

    var notificationMetadata: NotificationMetadata? = null
        set(value) {
            field = value
            reload()
        }

    var ratingType: Int = RatingCompat.RATING_NONE
        set(value) {
            field = value

            scope.launch {
                mediaSession.setRatingType(ratingType)
                mediaSessionConnector.setRatingCallback(object : MediaSessionConnector.RatingCallback {
                    override fun onCommand(player: Player, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean {
                        return true
                    }

                    override fun onSetRating(player: Player, rating: RatingCompat) {
                        event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.RATING(rating, null))
                    }

                    override fun onSetRating(player: Player, rating: RatingCompat, extras: Bundle?) {
                        event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.RATING(rating, extras))
                    }

                })
            }
        }

    private var _showPlayPauseButton = false
    var showPlayPauseButton: Boolean
        get() = _showPlayPauseButton
        set(value) {
            scope.launch {
                _showPlayPauseButton = value
                internalManager?.setUsePlayPauseActions(value)
            }
        }

    private var _showStopButton = false
    var showStopButton: Boolean
        get() = _showStopButton
        set(value) {
            scope.launch {
                _showStopButton = value
                internalManager?.setUseStopAction(value)
            }
        }

    private var _showForwardButton = false
    var showForwardButton: Boolean
        get() = _showForwardButton
        set(value) {
            scope.launch {
                _showForwardButton = value
                internalManager?.setUseFastForwardAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    private var _showForwardButtonCompact = false
    var showForwardButtonCompact: Boolean
        get() = _showForwardButtonCompact
        set(value) {
            scope.launch {
                _showForwardButtonCompact = value
                internalManager?.setUseFastForwardActionInCompactView(value)
            }
        }

    private var _showBackwardButton = false
    var showBackwardButton: Boolean
        get() = _showBackwardButton
        set(value) {
            scope.launch {
                _showBackwardButton = value
                internalManager?.setUseRewindAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    private var _showBackwardButtonCompact = false
    var showBackwardButtonCompact: Boolean
        get() = _showBackwardButtonCompact
        set(value) {
            scope.launch {
                _showBackwardButtonCompact = value
                internalManager?.setUseRewindActionInCompactView(value)
            }
        }

    private var _showNextButton = false
    var showNextButton: Boolean
        get() = _showNextButton
        set(value) {
            scope.launch {
                _showNextButton = value
                internalManager?.setUseNextAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    private var _showNextButtonCompact = false
    var showNextButtonCompact: Boolean
        get() = _showNextButtonCompact
        set(value) {
            scope.launch {
                _showNextButtonCompact = value
                internalManager?.setUseNextActionInCompactView(value)
            }
        }

    private var _showPreviousButton = false
    var showPreviousButton: Boolean
        get() = _showPreviousButton
        set(value) {
            scope.launch {
                _showPreviousButton = value
                internalManager?.setUsePreviousAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    private var _showPreviousButtonCompact = false
    var showPreviousButtonCompact: Boolean
        get() = _showPreviousButtonCompact
        set(value) {
            scope.launch {
                _showPreviousButtonCompact = value
                internalManager?.setUsePreviousActionInCompactView(value)
            }
        }

    init {
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.PLAY(null))
            }

            override fun onPause() {
                super.onPause()
                event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.PAUSE(null))
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.NEXT(null))
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.PREVIOUS(null))
            }

            override fun onFastForward() {
                super.onFastForward()
                event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.FORWARD(null))
            }

            override fun onRewind() {
                super.onRewind()
                event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.REWIND(null))
            }

            override fun onStop() {
                super.onStop()
                event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.STOP(null))
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                event.updateOnMediaSessionCallbackTriggered(MediaSessionCallback.SEEK(position = pos, null))
            }
        })

        scope.launch {
            if (!isJUnitTest()) {
                mediaSessionConnector.setPlayer(exoPlayer)
            }
        }
    }

    /**
     * Create a media player notification that automatically updates.
     *
     * **NOTE:** You should only call this once. Subsequent calls will result in an error.
     */
    fun createNotification(config: NotificationConfig) = scope.launch {
        buttons.apply {
            clear()
            addAll(config.buttons)
        }

        descriptionAdapter = DescriptionAdapter(object : NotificationMetadataProvider {
            override fun getTitle(): String? {
                return notificationMetadata?.title
            }

            override fun getArtist(): String? {
                return notificationMetadata?.artist
            }

            override fun getArtworkUrl(): String? {
                return notificationMetadata?.artworkUrl
            }
        }, context, config.pendingIntent)

        internalManager = PlayerNotificationManager.Builder(context, NOTIFICATION_ID, CHANNEL_ID).apply {
            setChannelNameResourceId(R.string.playback_channel_name)
            setMediaDescriptionAdapter(descriptionAdapter)
            setNotificationListener(this@NotificationManager)

            if (buttons.isNotEmpty()) {
                config.buttons.forEach { button ->
                    when (button) {
                        is NotificationButton.PLAY -> button.icon?.let { setPlayActionIconResourceId(it) }
                        is NotificationButton.PAUSE -> button.icon?.let { setPauseActionIconResourceId(it) }
                        is NotificationButton.STOP -> button.icon?.let { setStopActionIconResourceId(it) }
                        is NotificationButton.FORWARD -> button.icon?.let { setFastForwardActionIconResourceId(it) }
                        is NotificationButton.BACKWARD -> button.icon?.let { setRewindActionIconResourceId(it) }
                        is NotificationButton.NEXT -> button.icon?.let { setNextActionIconResourceId(it) }
                        is NotificationButton.PREVIOUS -> button.icon?.let { setPreviousActionIconResourceId(it) }
                    }
                }
            }
        }.build()

        Log.d("RNTP", ">>>>>>>> packageName: " + context.packageName)
        var intentFilter = IntentFilter("com.doublesymmetry.kotlinaudio.event")
//        intentFilter.addAction(PlayerNotificationManager.ACTION_PLAY)
//        intentFilter.addAction(PlayerNotificationManager.ACTION_PAUSE)
//        intentFilter.addAction(PlayerNotificationManager.ACTION_PREVIOUS)
//        intentFilter.addAction(PlayerNotificationManager.ACTION_NEXT)
//        intentFilter.addAction(PlayerNotificationManager.ACTION_FAST_FORWARD)
//        intentFilter.addAction(PlayerNotificationManager.ACTION_REWIND)
//        intentFilter.addAction(PlayerNotificationManager.ACTION_STOP)
        var manager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
        manager.registerReceiver(object : BroadcastReceiver () {
            override fun onReceive(p0: Context?, p1: Intent?) {
                Log.d("RNTP", ">>>>>>>> onReceive")
            }
        }, intentFilter);

        if (!isJUnitTest()) {
            internalManager?.apply {
                setColor(config.accentColor ?: Color.TRANSPARENT)
                config.smallIcon?.let { setSmallIcon(it) }

                config.buttons.forEach { button ->
                    when (button) {
                        is NotificationButton.PLAY, is NotificationButton.PAUSE -> showPlayPauseButton = true
                        is NotificationButton.STOP -> {
                            showStopButton = true
                        }
                        is NotificationButton.FORWARD -> {
                            showForwardButton = true
                            showForwardButtonCompact = button.isCompact
                        }
                        is NotificationButton.BACKWARD -> {
                            showBackwardButton = true
                            showBackwardButtonCompact = button.isCompact
                        }
                        is NotificationButton.NEXT -> {
                            showNextButton = true
                            showNextButtonCompact = button.isCompact
                        }
                        is NotificationButton.PREVIOUS -> {
                            showPreviousButton = true
                            showPreviousButtonCompact = button.isCompact
                        }
                    }
                }

                setMediaSessionToken(mediaSession.sessionToken)
                setPlayer(exoPlayer)
            }
        }
    }

    // FIXME: This functions seems wrong. It does not do what the name suggests...
    fun clearNotification() = scope.launch {
        mediaSession.isActive = false
        internalManager?.setPlayer(null)
    }

    override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
        scope.launch {
            event.updateNotificationState(NotificationState.POSTED(notificationId, notification, ongoing))
        }
    }

    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        scope.launch {
            event.updateNotificationState(NotificationState.CANCELLED(notificationId))
        }
    }

    internal fun onPlay() = scope.launch {
        mediaSession.isActive = true
        reload()
    }

    internal fun onPause() = scope.launch {
        reload()
    }

    fun onUnbind() = scope.launch {
        reload()
    }

    internal fun destroy() = scope.launch {
        mediaSession.isActive = false
        descriptionAdapter.release()
        internalManager?.setPlayer(null)
    }

    private fun reload() = scope.launch {
        internalManager?.invalidate()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "kotlin_audio_player"
    }
}
