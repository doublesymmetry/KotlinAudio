package com.doublesymmetry.kotlinaudio.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.doublesymmetry.kotlinaudio.R
import com.doublesymmetry.kotlinaudio.event.NotificationEventHolder
import com.doublesymmetry.kotlinaudio.event.PlayerEventHolder
import com.doublesymmetry.kotlinaudio.models.MediaSessionCallback
import com.doublesymmetry.kotlinaudio.models.NotificationButton
import com.doublesymmetry.kotlinaudio.models.NotificationConfig
import com.doublesymmetry.kotlinaudio.models.NotificationMetadata
import com.doublesymmetry.kotlinaudio.models.NotificationState
import com.doublesymmetry.kotlinaudio.players.components.getAudioItemHolder
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.CustomActionReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationManager internal constructor(
    private val context: Context,
    private val player: Player,
    private val mediaSession: MediaSessionCompat,
    private val mediaSessionConnector: MediaSessionConnector,
    val event: NotificationEventHolder,
    val playerEventHolder: PlayerEventHolder
) : PlayerNotificationManager.NotificationListener {
    private var pendingIntent: PendingIntent? = null
    private val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return getTitle() ?: ""
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return pendingIntent
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return getArtist() ?: ""
        }

        override fun getCurrentSubText(player: Player): CharSequence? {
            return player.mediaMetadata.displayTitle
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback,
        ): Bitmap? {
            val bitmap = getArtworkBitmap()
            if (bitmap != null) {
                return bitmap
            }
            val artwork = getMediaItemArtworkUrl()
            val holder = player.currentMediaItem?.getAudioItemHolder()
            if (artwork != null && holder?.artworkBitmap == null) {
                context.imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(artwork)
                        .target { result ->
                            val resultBitmap = (result as BitmapDrawable).bitmap
                            holder?.artworkBitmap = resultBitmap
                            invalidate()
                        }
                        .build()
                )
            }
            return iconPlaceholder
        }
    }

    private var internalNotificationManager: PlayerNotificationManager? = null
    private val scope = MainScope()
    private val buttons = mutableSetOf<NotificationButton?>()
    private var invalidateThrottleCount = 0
    private var notificationMetadataBitmap: Bitmap? = null
    private var notificationMetadataArtworkDisposable: Disposable? = null
    private var iconPlaceholder = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    var notificationMetadata: NotificationMetadata? = null
        set(value) {
            if (value == null) {
                val changed = field != null
                if (changed) {
                    field = null
                    notificationMetadataBitmap = null
                    invalidate()
                }
                return
            }
            val holder = player.currentMediaItem?.getAudioItemHolder()
            val artworkChanged = field?.artworkUrl != value.artworkUrl
                && holder?.audioItem?.artwork != value.artworkUrl
            val titleChanged = holder?.audioItem?.title != value.title
            val artistChanged = holder?.audioItem?.artist != value.artist

            if (artworkChanged) {
                notificationMetadataBitmap = null
                // Cancel loading previous artwork:
                notificationMetadataArtworkDisposable?.dispose()
                if (value.artworkUrl != null) {
                    notificationMetadataArtworkDisposable = context.imageLoader.enqueue(
                        ImageRequest.Builder(context)
                            .data(value.artworkUrl)
                            .target { result ->
                                notificationMetadataBitmap = (result as BitmapDrawable).bitmap
                                invalidate()
                            }
                            .build()
                    )
                } else {
                    notificationMetadataArtworkDisposable = null
                }
            }
            if (artworkChanged || titleChanged || artistChanged) {
                field = value
                invalidate()
            }
        }

    private fun getTitle(index: Int? = null): String? {
        val mediaItem = if (index == null) player.getCurrentMediaItem()
            else player.getMediaItemAt(index)
        val isCurrent = index == null || index == player.currentMediaItemIndex
        return ((if (isCurrent) notificationMetadata else null)?.title
            ?: mediaItem?.mediaMetadata?.title
            ?: mediaItem?.getAudioItemHolder()?.audioItem?.title)?.toString()
    }

    private fun getArtist(index: Int? = null): String? {
        val mediaItem = if (index == null) player.getCurrentMediaItem()
            else player.getMediaItemAt(index)
        val isCurrent = index == null || index == player.currentMediaItemIndex
        return (
            (if (isCurrent) notificationMetadata else null)?.artist
            ?: mediaItem?.mediaMetadata?.artist
            ?: mediaItem?.mediaMetadata?.albumArtist
            ?: mediaItem?.getAudioItemHolder()?.audioItem?.artist
        )?.toString()
    }

    private fun getGenre(index: Int? = null): String? {
        val mediaItem = if (index == null) player.getCurrentMediaItem()
            else player.getMediaItemAt(index)
        return mediaItem?.mediaMetadata?.genre?.toString()
    }

    private fun getAlbumTitle(index: Int? = null): String? {
        val mediaItem = if (index == null) player.getCurrentMediaItem()
            else player.getMediaItemAt(index)
        return (mediaItem?.mediaMetadata?.albumTitle
            ?: mediaItem?.getAudioItemHolder()?.audioItem?.albumTitle)?.toString()
    }

    private fun getArtworkUrl(index: Int? = null): String? {
        val isCurrent = index == null || index == player.currentMediaItemIndex
        return (
            (if (isCurrent) notificationMetadata else null)?.artworkUrl
            ?: getMediaItemArtworkUrl(index)
        )?.toString()
    }

    private fun getMediaItemArtworkUrl(index: Int? = null): String? {
        val mediaItem = if (index == null) player.getCurrentMediaItem()
            else player.getMediaItemAt(index)
        return (
            mediaItem?.mediaMetadata?.artworkUri
            ?: mediaItem?.getAudioItemHolder()?.audioItem?.artwork
        )?.toString()
    }

    private fun getArtworkBitmap(index: Int? = null): Bitmap? {
        val mediaItem = if (index == null) player.getCurrentMediaItem()
            else player.getMediaItemAt(index)
        val isCurrent = index == null || index == player.currentMediaItemIndex
        val artworkData = player.mediaMetadata.artworkData
        return (
            if (isCurrent && notificationMetadata?.artworkUrl != null)
                notificationMetadataBitmap
            else
                null
        ) ?: (
            if (isCurrent && artworkData != null)
                BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
            else
                null
        ) ?: mediaItem?.getAudioItemHolder()?.artworkBitmap
    }

    private fun getDuration(index: Int? = null): Long? {
        val mediaItem = if (index == null) player.getCurrentMediaItem()
            else player.getMediaItemAt(index)
        return mediaItem?.getAudioItemHolder()?.audioItem?.duration ?: -1
    }

    private fun getUserRating(index: Int? = null): RatingCompat? {
        val mediaItem = if (index == null) player.getCurrentMediaItem()
            else player.getMediaItemAt(index)
        return RatingCompat.fromRating(mediaItem?.mediaMetadata?.userRating)
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
                    val title = getTitle(windowIndex)
                    val artist = getArtist(windowIndex)
                    return MediaDescriptionCompat.Builder().apply {
                        setTitle(title)
                        setSubtitle(artist)
                        setExtras(Bundle().apply {
                            title?.let {
                                putString(MediaMetadataCompat.METADATA_KEY_TITLE, it)
                            }
                            artist?.let {
                                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it)
                            }
                        })
                        setIconUri(mediaItem.mediaMetadata.artworkUri ?: Uri.parse(audioItemHolder.audioItem.artwork))
                        setIconBitmap(audioItemHolder.artworkBitmap)
                    }.build()
                }
            }
        )
        mediaSessionConnector.setMetadataDeduplicationEnabled(true)
    }

    public fun getMediaMetadataCompat(): MediaMetadataCompat {
        return MediaMetadataCompat.Builder().apply {
            getArtist()?.let {
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it)
            }
            getTitle()?.let {
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, it)
            }
            getAlbumTitle()?.let {
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it)
            }
            getGenre()?.let {
                putString(MediaMetadataCompat.METADATA_KEY_GENRE, it)
            }
            getDuration()?.let {
                putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it)
            }
            getArtworkUrl()?.let {
                putString(MediaMetadataCompat.METADATA_KEY_ART_URI, it)
            }
            getArtworkBitmap()?.let {
                putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it);
                putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it);
            }
            getUserRating()?.let {
                putRating(MediaMetadataCompat.METADATA_KEY_RATING, it)
            }
        }.build()
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
        if (invalidateThrottleCount++ == 0) {
            scope.launch {
                internalNotificationManager?.invalidate()
                mediaSessionConnector.invalidateMediaSessionQueue()
                mediaSessionConnector.invalidateMediaSessionMetadata()
                delay(300)
                val wasThrottled = invalidateThrottleCount > 1
                invalidateThrottleCount = 0
                if (wasThrottled) {
                    invalidate()
                }
            }
        }
    }

    /**
     * Create a media player notification that automatically updates. Call this
     * method again with a different configuration to update the notification.
     */
    fun createNotification(config: NotificationConfig) = scope.launch {
        if (isNotificationButtonsChanged(config.buttons)) {
            hideNotification()
        }

        buttons.apply {
            clear()
            addAll(config.buttons)
        }

        stopIcon = null
        forwardIcon = null
        rewindIcon = null

        updateMediaSessionPlaybackActions()

        pendingIntent = config.pendingIntent
        showPlayPauseButton = false
        showForwardButton = false
        showRewindButton = false
        showNextButton = false
        showPreviousButton = false
        showStopButton = false
        if (internalNotificationManager == null) {
            internalNotificationManager =
                PlayerNotificationManager.Builder(context, NOTIFICATION_ID, CHANNEL_ID)
                    .apply {
                        setChannelNameResourceId(R.string.playback_channel_name)
                        setMediaDescriptionAdapter(descriptionAdapter)
                        setCustomActionReceiver(customActionReceiver)
                        setNotificationListener(this@NotificationManager)

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
                        setMediaSessionToken(mediaSession.sessionToken)
                        setPlayer(player)
                    }
        }
        setupInternalNotificationManager(config)
    }

    private fun isNotificationButtonsChanged(newButtons: List<NotificationButton>): Boolean {
        val currentNotificationButtonsMapByType = buttons.filterNotNull().associateBy { it::class }
        return newButtons.any { newButton ->
            when (newButton) {
                is NotificationButton.PLAY_PAUSE -> {
                    (currentNotificationButtonsMapByType[NotificationButton.PLAY_PAUSE::class] as? NotificationButton.PLAY_PAUSE).let { currentButton ->
                        newButton.pauseIcon != currentButton?.pauseIcon || newButton.playIcon != currentButton?.playIcon
                    }
                }

                is NotificationButton.STOP -> {
                    (currentNotificationButtonsMapByType[NotificationButton.STOP::class] as? NotificationButton.STOP).let { currentButton ->
                        newButton.icon != currentButton?.icon
                    }
                }

                is NotificationButton.FORWARD -> {
                    (currentNotificationButtonsMapByType[NotificationButton.FORWARD::class] as? NotificationButton.FORWARD).let { currentButton ->
                        newButton.icon != currentButton?.icon
                    }
                }

                is NotificationButton.BACKWARD -> {
                    (currentNotificationButtonsMapByType[NotificationButton.BACKWARD::class] as? NotificationButton.BACKWARD).let { currentButton ->
                        newButton.icon != currentButton?.icon
                    }
                }

                is NotificationButton.NEXT -> {
                    (currentNotificationButtonsMapByType[NotificationButton.NEXT::class] as? NotificationButton.NEXT).let { currentButton ->
                        newButton.icon != currentButton?.icon
                    }
                }

                is NotificationButton.PREVIOUS -> {
                    (currentNotificationButtonsMapByType[NotificationButton.PREVIOUS::class] as? NotificationButton.PREVIOUS).let { currentButton ->
                        newButton.icon != currentButton?.icon
                    }
                }

                else -> false
            }
        }
    }

    private fun updateMediaSessionPlaybackActions() {
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
    }

    private fun setupInternalNotificationManager(config: NotificationConfig) {
        internalNotificationManager?.run {
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
        }
    }

    fun hideNotification() {
        internalNotificationManager?.setPlayer(null)
        internalNotificationManager = null
        invalidate()
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
