package com.doublesymmetry.kotlinaudio.notification

import android.app.Notification
import android.content.Context
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

class NotificationManager internal constructor(private val context: Context, private val exoPlayer: ExoPlayer, private val event: NotificationEventHolder) : PlayerNotificationManager.NotificationListener {
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

    var showPlayPauseButton = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUsePlayPauseActions(value)
            }
        }

    var showStopButton = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUseStopAction(value)
            }
        }

    var showForwardButton = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUseFastForwardAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    var showForwardButtonCompact = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUseFastForwardActionInCompactView(value)
            }
        }

    var showRewindButton = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUseRewindAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    var showRewindButtonCompact = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUseRewindActionInCompactView(value)
            }
        }

    var showNextButton = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUseNextAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    var showNextButtonCompact = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUseNextActionInCompactView(value)
            }
        }

    var showPreviousButton = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUsePreviousAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    var showPreviousButtonCompact = true
        set(value) {
            scope.launch {
                field = value
                internalManager?.setUsePreviousActionInCompactView(value)
            }
        }

    init {
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
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
//                setPrimaryActionReceiver(this@NotificationManager)

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

        if (!isJUnitTest()) {
            internalManager?.apply {
                setColor(config.accentColor ?: Color.TRANSPARENT)
                config.smallIcon?.let { setSmallIcon(it) }

                config.buttons.forEach { button ->
                    when (button) {
                        is NotificationButton.PLAY, is NotificationButton.PAUSE -> {
                            showPlayPauseButton = true
                        }
                        is NotificationButton.STOP -> {
                            showStopButton = true
                        }
                        is NotificationButton.FORWARD -> {
                            showForwardButton = true
                            showForwardButtonCompact = button.isCompact
                        }
                        is NotificationButton.BACKWARD -> {
                            showRewindButton = true
                            showRewindButtonCompact = button.isCompact
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

//    override fun onAction(player: Player, action: String, intent: Intent) {
//        scope.launch {
//            event.updateOnNotificationButtonTapped(NotificationButton.valueOf(action))
//        }
//    }

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
