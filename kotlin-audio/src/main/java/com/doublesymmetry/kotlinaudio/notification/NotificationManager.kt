package com.doublesymmetry.kotlinaudio.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import coil.imageLoader
import coil.request.ImageRequest
import com.doublesymmetry.kotlinaudio.R
import com.doublesymmetry.kotlinaudio.event.NotificationEventHolder
import com.doublesymmetry.kotlinaudio.event.PlayerEventHolder
import com.doublesymmetry.kotlinaudio.models.AudioItemHolder
import com.doublesymmetry.kotlinaudio.models.MediaSessionCallback
import com.doublesymmetry.kotlinaudio.models.NotificationButton
import com.doublesymmetry.kotlinaudio.models.NotificationConfig
import com.doublesymmetry.kotlinaudio.models.NotificationMetadata
import com.doublesymmetry.kotlinaudio.models.NotificationState
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.CustomActionReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class NotificationManager internal constructor(
    private val context: Context,
    private val player: Player,
    private val mediaSession: MediaSessionCompat,
    private val mediaSessionConnector: MediaSessionConnector,
    val event: NotificationEventHolder,
    val playerEventHolder: PlayerEventHolder
) : PlayerNotificationManager.NotificationListener {
    private lateinit var descriptionAdapter: PlayerNotificationManager.MediaDescriptionAdapter
    private var internalNotificationManager: PlayerNotificationManager? = null
    private val scope = MainScope()
    private val buttons = mutableSetOf<NotificationButton?>()
    var notificationMetadata: NotificationMetadata? = null
        set(value) {
            // Clear bitmap cache if artwork changes
            if (field?.artworkUrl != value?.artworkUrl) {
                val holder = getCurrentItemHolder()
                if (holder != null) {
                    holder.artworkBitmap = null
                }
            }
            field = value
            invalidate()
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

    var stopIcon: Int? = null
    var forwardIcon: Int? = null
    var rewindIcon: Int? = null

    private fun getCurrentItemHolder(): AudioItemHolder? {
        return player?.currentMediaItem?.localConfiguration?.tag as AudioItemHolder?
    }

    init {
        mediaSessionConnector.setQueueNavigator(
            object : TimelineQueueNavigator(mediaSession) {
                override fun getSupportedQueueNavigatorActions(player: Player): Long {
                    return buttons.fold(0) { acc, button ->
                        acc or when (button) {
                            is NotificationButton.NEXT -> {
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            }
                            is NotificationButton.PREVIOUS -> {
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            }
                            else -> {
                                0
                            }
                        }
                    }
                }

                override fun getMediaDescription(
                    player: Player,
                    windowIndex: Int
                ): MediaDescriptionCompat {
                    val currentNotificationMetadata =
                        if (windowIndex == player.currentMediaItemIndex)
                            notificationMetadata else null
                    val mediaItem = player.getMediaItemAt(windowIndex)
                    val audioItemHolder = (mediaItem.localConfiguration?.tag as AudioItemHolder)
                    var title = currentNotificationMetadata?.title ?: mediaItem.mediaMetadata.title
                    ?: audioItemHolder.audioItem.title
                    var artist =
                        currentNotificationMetadata?.artist ?: mediaItem.mediaMetadata.artist
                        ?: audioItemHolder.audioItem.artist
                    return MediaDescriptionCompat.Builder().apply {
                        setTitle(title)
                        setSubtitle(artist)
                        setExtras(Bundle().apply {
                            putString(MediaMetadataCompat.METADATA_KEY_TITLE, title as String?)
                            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist as String?)
                        })
                    }.build()
                }
            }
        )
        mediaSessionConnector.setMetadataDeduplicationEnabled(true)
    }

    private fun createNotificationAction(
        drawable: Int,
        action: String,
        instanceId: Int
    ): NotificationCompat.Action {
        val intent: Intent = Intent(action).setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            instanceId,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            } else {
                PendingIntent.FLAG_CANCEL_CURRENT
            }
        )
        return NotificationCompat.Action.Builder(drawable, action, pendingIntent).build()
    }

    private fun handlePlayerAction(action: String) {
        when (action) {
            REWIND -> {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.REWIND)
            }
            FORWARD -> {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.FORWARD)
            }
            STOP -> {
                playerEventHolder.updateOnPlayerActionTriggeredExternally(MediaSessionCallback.STOP)
            }

        }
    }

    private val customActionReceiver = object : CustomActionReceiver {
        override fun createCustomActions(
            context: Context,
            instanceId: Int
        ): MutableMap<String, NotificationCompat.Action> {
            if (!needsCustomActionsToAddMissingButtons) return mutableMapOf()
            return mutableMapOf(
                REWIND to createNotificationAction(
                    rewindIcon ?: DEFAULT_REWIND_ICON,
                    REWIND,
                    instanceId
                ),
                FORWARD to createNotificationAction(
                    forwardIcon ?: DEFAULT_FORWARD_ICON,
                    FORWARD,
                    instanceId
                ),
                STOP to createNotificationAction(
                    stopIcon ?: DEFAULT_STOP_ICON,
                    STOP,
                    instanceId
                )
            )
        }

        override fun getCustomActions(player: Player): List<String> {
            if (!needsCustomActionsToAddMissingButtons) return emptyList()
            return buttons.mapNotNull {
                when (it) {
                    is NotificationButton.BACKWARD -> {
                        REWIND
                    }
                    is NotificationButton.FORWARD -> {
                        FORWARD
                    }
                    is NotificationButton.STOP -> {
                        STOP
                    }
                    else -> {
                        null
                    }
                }
            }
        }

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            handlePlayerAction(action)
        }
    }

    fun invalidate() {
        internalNotificationManager?.invalidate()
        mediaSessionConnector.invalidateMediaSessionQueue()
        mediaSessionConnector.invalidateMediaSessionMetadata()
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

        stopIcon = null
        forwardIcon = null
        rewindIcon = null
        mediaSessionConnector.setEnabledPlaybackActions(
            buttons.fold(
                PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                        or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                        or PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED
            ) { acc, button ->
                acc or when (button) {
                    is NotificationButton.PLAY_PAUSE -> {
                        PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE
                    }
                    is NotificationButton.BACKWARD -> {
                        rewindIcon = button.icon ?: rewindIcon
                        PlaybackStateCompat.ACTION_REWIND
                    }
                    is NotificationButton.FORWARD -> {
                        forwardIcon = button.icon ?: forwardIcon
                        PlaybackStateCompat.ACTION_FAST_FORWARD
                    }
                    is NotificationButton.SEEK_TO -> {
                        PlaybackStateCompat.ACTION_SEEK_TO
                    }
                    is NotificationButton.STOP -> {
                        stopIcon = button.icon ?: stopIcon
                        PlaybackStateCompat.ACTION_STOP
                    }
                    else -> {
                        0
                    }
                }
            }
        )
        descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                return notificationMetadata?.title
                    ?: player.mediaMetadata.title
                    ?: ""
            }

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                return config.pendingIntent
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return notificationMetadata?.artist
                    ?: player.mediaMetadata.artist
                    ?: player.mediaMetadata.albumArtist
                    ?: ""
            }

            override fun getCurrentSubText(player: Player): CharSequence? {
                return player.mediaMetadata.displayTitle
            }

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback,
            ): Bitmap? {
                val holder = getCurrentItemHolder() ?: return null
                val source = notificationMetadata?.artworkUrl ?: player.mediaMetadata.artworkUri
                val data = player.mediaMetadata.artworkData

                if (notificationMetadata?.artworkUrl == null && data != null) {
                    return BitmapFactory.decodeByteArray(data, 0, data.size)
                }

                if (source == null) {
                    return null
                }

                if (holder.artworkBitmap != null) {
                    return holder.artworkBitmap
                }

                context.imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(source)
                        .target { result ->
                            val bitmap = (result as BitmapDrawable).bitmap
                            holder.artworkBitmap = bitmap
                            callback.onBitmap(bitmap)
                        }
                        .build()
                )
                return holder.artworkBitmap
            }
        }

        if (needsCustomActionsToAddMissingButtons) {
            val customActionProviders = buttons
                .sortedBy {
                    when (it) {
                        is NotificationButton.BACKWARD -> 1
                        is NotificationButton.FORWARD -> 2
                        is NotificationButton.STOP -> 3
                        else -> 4
                    }
                }
                .mapNotNull {
                    when (it) {
                        is NotificationButton.BACKWARD -> {
                            createMediaSessionAction(rewindIcon ?: DEFAULT_REWIND_ICON, REWIND)
                        }
                        is NotificationButton.FORWARD -> {
                            createMediaSessionAction(forwardIcon ?: DEFAULT_FORWARD_ICON, FORWARD)
                        }
                        is NotificationButton.STOP -> {
                            createMediaSessionAction(stopIcon ?: DEFAULT_STOP_ICON, STOP)
                        }
                        else -> {
                            null
                        }
                    }
                }
            mediaSessionConnector.setCustomActionProviders(*customActionProviders.toTypedArray())
        }

        internalNotificationManager =
            PlayerNotificationManager.Builder(context, NOTIFICATION_ID, CHANNEL_ID)
                .apply {
                    setChannelNameResourceId(R.string.playback_channel_name)
                    setMediaDescriptionAdapter(descriptionAdapter)
                    setCustomActionReceiver(customActionReceiver)
                    setNotificationListener(this@NotificationManager)

                    internalNotificationManager.apply {
                        showPlayPauseButton = false
                        showForwardButton = false
                        showRewindButton = false
                        showNextButton = false
                        showPreviousButton = false
                        showStopButton = false
                    }

                    for (button in buttons) {
                        if (button == null) continue
                        when (button) {
                            is NotificationButton.PLAY_PAUSE -> {
                                button.playIcon?.let { setPlayActionIconResourceId(it) }
                                button.pauseIcon?.let { setPauseActionIconResourceId(it) }
                            }
                            is NotificationButton.STOP -> button.icon?.let {
                                setStopActionIconResourceId(
                                    it
                                )
                            }
                            is NotificationButton.FORWARD -> button.icon?.let {
                                setFastForwardActionIconResourceId(
                                    it
                                )
                            }
                            is NotificationButton.BACKWARD -> button.icon?.let {
                                setRewindActionIconResourceId(
                                    it
                                )
                            }
                            is NotificationButton.NEXT -> button.icon?.let {
                                setNextActionIconResourceId(
                                    it
                                )
                            }
                            is NotificationButton.PREVIOUS -> button.icon?.let {
                                setPreviousActionIconResourceId(
                                    it
                                )
                            }
                            else -> {}
                        }
                    }
                }.build().apply {
                    setColor(config.accentColor ?: Color.TRANSPARENT)
                    config.smallIcon?.let { setSmallIcon(it) }
                    for (button in buttons) {
                        if (button == null) continue
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
                            else -> {}
                        }
                    }
                    setMediaSessionToken(mediaSession.sessionToken)
                    setPlayer(player)
                }
    }

    fun hideNotification() = scope.launch {
        internalNotificationManager?.setPlayer(null)
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        scope.launch {
            event.updateNotificationState(
                NotificationState.POSTED(
                    notificationId,
                    notification,
                    ongoing
                )
            )
        }
    }

    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        scope.launch {
            event.updateNotificationState(NotificationState.CANCELLED(notificationId))
        }
    }

    internal fun destroy() = scope.launch {
        internalNotificationManager?.setPlayer(null)
    }

    private fun createMediaSessionAction(
        @DrawableRes drawableRes: Int,
        actionName: String
    ): MediaSessionConnector.CustomActionProvider {
        return object : MediaSessionConnector.CustomActionProvider {
            override fun getCustomAction(player: Player): PlaybackStateCompat.CustomAction? {
                return PlaybackStateCompat.CustomAction.Builder(actionName, actionName, drawableRes)
                    .build()
            }

            override fun onCustomAction(player: Player, action: String, extras: Bundle?) {
                handlePlayerAction(action)
            }
        }
    }

    companion object {
        // Due to the removal of rewind, forward, and stop buttons from the standard notification
        // controls in Android 13, custom actions are implemented to support them
        // https://developer.android.com/about/versions/13/behavior-changes-13#playback-controls
        private val needsCustomActionsToAddMissingButtons = Build.VERSION.SDK_INT >= 33
        private const val REWIND = "rewind"
        private const val FORWARD = "forward"
        private const val STOP = "stop"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "kotlin_audio_player"
        private val DEFAULT_STOP_ICON =
            com.google.android.exoplayer2.ui.R.drawable.exo_notification_stop
        private val DEFAULT_REWIND_ICON =
            com.google.android.exoplayer2.ui.R.drawable.exo_notification_rewind
        private val DEFAULT_FORWARD_ICON =
            com.google.android.exoplayer2.ui.R.drawable.exo_notification_fastforward
    }
}
