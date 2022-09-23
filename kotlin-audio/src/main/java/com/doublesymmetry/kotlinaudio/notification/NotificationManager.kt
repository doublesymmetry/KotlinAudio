package com.doublesymmetry.kotlinaudio.notification

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.support.v4.media.session.MediaSessionCompat
import com.doublesymmetry.kotlinaudio.R
import com.doublesymmetry.kotlinaudio.event.NotificationEventHolder
import com.doublesymmetry.kotlinaudio.models.NotificationButton
import com.doublesymmetry.kotlinaudio.models.NotificationConfig
import com.doublesymmetry.kotlinaudio.models.NotificationMetadata
import com.doublesymmetry.kotlinaudio.models.NotificationState
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class NotificationManager internal constructor(private val context: Context, private val player: Player, private val mediaSessionToken: MediaSessionCompat.Token, private val event: NotificationEventHolder) : PlayerNotificationManager.NotificationListener {
    private lateinit var descriptionAdapter: DescriptionAdapter
    private var internalNotificationManager: PlayerNotificationManager? = null
    private val scope = MainScope()
    private val buttons = mutableSetOf<NotificationButton?>()

    var notificationMetadata: NotificationMetadata? = null
        set(value) {
            field = value
            reload()
        }

    var showPlayPauseButton = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUsePlayPauseActions(value)
            }
        }

    var showStopButton = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUseStopAction(value)
            }
        }

    var showForwardButton = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUseFastForwardAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    var showForwardButtonCompact = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUseFastForwardActionInCompactView(value)
            }
        }

    var showRewindButton = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUseRewindAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    var showRewindButtonCompact = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUseRewindActionInCompactView(value)
            }
        }

    var showNextButton = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUseNextAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    var showNextButtonCompact = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUseNextActionInCompactView(value)
            }
        }

    var showPreviousButton = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUsePreviousAction(value)
            }
        }

    /**
     * Controls whether or not this button should appear when the notification is compact (collapsed).
     */
    var showPreviousButtonCompact = false
        set(value) {
            scope.launch {
                field = value
                internalNotificationManager?.setUsePreviousActionInCompactView(value)
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

        descriptionAdapter = DescriptionAdapter(
            object : NotificationMetadataProvider {
                override fun getTitle(): String? {
                    return notificationMetadata?.title
                }

                override fun getArtist(): String? {
                    return notificationMetadata?.artist
                }

                override fun getArtworkUrl(): String? {
                    return notificationMetadata?.artworkUrl
                }
            },
            context,
            config.pendingIntent
        )

        internalNotificationManager = PlayerNotificationManager.Builder(context, NOTIFICATION_ID, CHANNEL_ID).apply {
            setChannelNameResourceId(R.string.playback_channel_name)
            setMediaDescriptionAdapter(descriptionAdapter)
            setNotificationListener(this@NotificationManager)

            hideAllButtonsByDefault()

            if (buttons.isNotEmpty()) {
                config.buttons.forEach { button ->
                    when (button) {
                        is NotificationButton.PLAY_PAUSE -> {
                            button.playIcon?.let { setPlayActionIconResourceId(it) }
                            button.pauseIcon?.let { setPauseActionIconResourceId(it) }
                        }
                        is NotificationButton.STOP -> button.icon?.let { setStopActionIconResourceId(it) }
                        is NotificationButton.FORWARD -> button.icon?.let { setFastForwardActionIconResourceId(it) }
                        is NotificationButton.BACKWARD -> button.icon?.let { setRewindActionIconResourceId(it) }
                        is NotificationButton.NEXT -> button.icon?.let { setNextActionIconResourceId(it) }
                        is NotificationButton.PREVIOUS -> button.icon?.let { setPreviousActionIconResourceId(it) }
                    }
                }
            }
        }.build()

        internalNotificationManager?.apply {
            setColor(config.accentColor ?: Color.TRANSPARENT)
            config.smallIcon?.let { setSmallIcon(it) }
            config.buttons.forEach { button ->
                when (button) {
                    is NotificationButton.PLAY_PAUSE -> {
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

            setMediaSessionToken(mediaSessionToken)
            setPlayer(player)
        }
    }

    fun hideNotification() = scope.launch {
        internalNotificationManager?.setPlayer(null)
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

    internal fun destroy() = scope.launch {
        descriptionAdapter.release()
        internalNotificationManager?.setPlayer(null)
    }

    private fun reload() = scope.launch {
        internalNotificationManager?.invalidate()
    }

    private fun hideAllButtonsByDefault() {
        internalNotificationManager.apply {
            showPlayPauseButton = false
            showForwardButton = false
            showRewindButton = false
            showNextButton = false
            showPreviousButton = false
            showStopButton = false
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "kotlin_audio_player"
    }
}
