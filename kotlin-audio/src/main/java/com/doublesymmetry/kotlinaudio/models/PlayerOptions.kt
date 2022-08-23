package com.doublesymmetry.kotlinaudio.models

interface PlayerOptions {
    var alwaysPauseOnInterruption: Boolean

    /**
     * Toggle whether or not a player action has been triggered from an outside source should be intercepted.
     *
     * The sources can be: media buttons on headphones, Android Wear, Android Auto, Google Assistant, media notification, etc.
     *
     * Setting this to true enables the use of [com.doublesymmetry.kotlinaudio.event.PlayerEventHolder.onPlayerActionTriggeredExternally] events.
     */
    var interceptPlayerActionsTriggeredExternally: Boolean
}

internal class DefaultPlayerOptions(
    override var alwaysPauseOnInterruption: Boolean = false,
    override var interceptPlayerActionsTriggeredExternally: Boolean = false,
) : PlayerOptions
