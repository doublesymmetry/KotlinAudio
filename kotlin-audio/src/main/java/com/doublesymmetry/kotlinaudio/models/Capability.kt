package com.doublesymmetry.kotlinaudio.models

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
sealed class Capability(open val showInNotification: Boolean = true) {
    data class PlayPause(
        override val showInNotification: Boolean = true,
        val notificationOptions: NofiticationPlayPauseActionOptions = NofiticationPlayPauseActionOptions.DEFAULT
    ) : Capability()

    object PlayFromId : Capability(showInNotification = false)

    object PlayFromSearch : Capability(showInNotification = false)

    data class Stop(
        override val showInNotification: Boolean = true,
        val notificationOptions: NofiticationIconActionOptions? = null
    ) : Capability()

    object SeekTo : Capability(showInNotification = false)

    object Skip : Capability(showInNotification = false)

    data class Next(
        override val showInNotification: Boolean = true,
        val notificationOptions: NofiticationActionOptions = NofiticationActionOptions.DEFAULT
    ) : Capability()

    data class Previous(
        override val showInNotification: Boolean = true,
        val notificationOptions: NofiticationActionOptions = NofiticationActionOptions.DEFAULT
    ) : Capability()

    data class Forward(
        override val showInNotification: Boolean = true,
        val notificationOptions: NofiticationActionOptions = NofiticationActionOptions.DEFAULT
    ) : Capability()

    data class Backward(
        override val showInNotification: Boolean = true,
        val notificationOptions: NofiticationActionOptions = NofiticationActionOptions.DEFAULT
    ) : Capability()

    data class SetRating(val type: Int) : Capability(showInNotification = false)
}


data class CapabilitiesConfig(
    val capabilities: List<Capability>,
    val notificationOptions: NotificationOptions,
)

/** Custom extension to filter out only those capabilities that are supported by the notification */
fun List<Capability>.filterForNotification(): List<Capability> {
    return this.filter { it.showInNotification }
}
