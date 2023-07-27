package com.doublesymmetry.kotlinaudio.models

import android.support.v4.media.RatingCompat

/**
 * Defines the capabilities supported by the media session and whether they're also supported by the notification.
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showPlayPauseButton]
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showStopButton]
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showRewindButton]
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showRewindButtonCompact]
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showForwardButton]
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showForwardButtonCompact]
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showNextButton]
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showNextButtonCompact]
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showPreviousButton]
 * @see [com.doublesymmetry.kotlinaudio.notification.NotificationManager.showPreviousButtonCompact]
 */
sealed class Capability {
    /**
     * Play and pause capability with optional configuration for the notification.
     * @param showInNotification Whether to show the button in the notification. Defaults to true.
     * @param notificationConfig The configuration for the notification button.
     */
    data class PLAY_PAUSE(val showInNotification: Boolean = true, val notificationConfig: NofiticationPlayPauseActionConfig? = null): Capability()

    /** Play from a media id capability. Used in the media session. */
    object PLAY_FROM_ID: Capability()
    /** Play from search capability. Used in the media session. */
    object PLAY_FROM_SEARCH: Capability()
    /**
     * Stop capability with optional configuration for the notification.
     * @param showInNotification Whether to show the button in the notification. Defaults to true.
     * @param notificationConfig The configuration for the notification button.
     */
    data class STOP(val showInNotification: Boolean = true, val notificationConfig: NofiticationIconActionConfig? = null): Capability()
    /** Seek to capability. Used in the media session. */
    object SEEK_TO: Capability()
    /** Skip to queue item capability. Used in the media session. */
    object SKIP: Capability()
    /**
     * Skip to next item capability with optional configuration for the notification.
     * @param showInNotification Whether to show the button in the notification. Defaults to true.
     * @param notificationConfig The configuration for the notification button.
     */
    data class NEXT(val showInNotification: Boolean = true, val notificationConfig: NofiticationActionConfig? = null): Capability()
    /**
     * Skip to previous item capability with optional configuration for the notification.
     * @param showInNotification Whether to show the button in the notification. Defaults to true.
     * @param notificationConfig The configuration for the notification button.
     */
    data class PREVIOUS(val showInNotification: Boolean = true, val notificationConfig: NofiticationActionConfig? = null): Capability()
    /**
     * Skip forward by interval capability with optional configuration for the notification.
     * @param showInNotification Whether to show the button in the notification. Defaults to true.
     * @param notificationConfig The configuration for the notification button.
     */
    data class FORWARD(val showInNotification: Boolean = true, val notificationConfig: NofiticationActionConfig? = null): Capability()
    /**
     * Skip backward by interval capability with optional configuration for the notification.
     * @param showInNotification Whether to show the button in the notification. Defaults to true.
     * @param notificationConfig The configuration for the notification button.
     */
    data class BACKWARD(val showInNotification: Boolean = true, val notificationConfig: NofiticationActionConfig? = null): Capability()
    /** Set rating capability. Used in the media session. */
    data class SET_RATING(val type: Int): Capability()
}

data class CapabilitiesConfig(
    val capabilities: List<Capability>,
    val notificationConfig: NotificationConfig,
)

/** Custom extension to filter out only those capabilities that are supported by the notification */
fun List<Capability>.filterForNotification(): List<Capability> {
    return this.filter {
        (it is Capability.PLAY_PAUSE && it.showInNotification)
                || (it is Capability.STOP && it.showInNotification)
                || (it is Capability.NEXT && it.showInNotification)
                || (it is Capability.PREVIOUS && it.showInNotification)
                || (it is Capability.FORWARD && it.showInNotification)
                || (it is Capability.BACKWARD && it.showInNotification)
    } ?: emptyList()
}
