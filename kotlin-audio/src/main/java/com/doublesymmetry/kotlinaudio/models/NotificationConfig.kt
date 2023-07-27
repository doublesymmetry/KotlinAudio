package com.doublesymmetry.kotlinaudio.models

import android.app.PendingIntent
import androidx.annotation.DrawableRes

/**
 * Used to configure the player notification.
 * @param accentColor The accent color of the notification.
 * @param smallIcon The small icon of the notification which is also shown in the system status bar.
 * @param pendingIntent The [PendingIntent] that would be called when tapping on the notification itself.
 */
data class NotificationConfig(
    val accentColor: Int? = null,
    @DrawableRes val smallIcon: Int? = null,
    val pendingIntent: PendingIntent? = null
)

/** Used to configure the properties of a standard notification button */
data class NofiticationActionConfig(
    @DrawableRes val icon: Int? = null,
    val isCompact: Boolean = false
)

/** Used to configure the properties of a standard notification button */
data class NofiticationIconActionConfig(
    @DrawableRes val icon: Int? = null,
)

/** Used to configure the properties of a standard notification button */
data class NofiticationPlayPauseActionConfig(
    @DrawableRes val playIcon: Int? = null,
    @DrawableRes var pauseIcon: Int? = null,
    val isCompact: Boolean = false
)