package com.doublesymmetry.kotlinaudio.models

sealed class AudioItemTransitionReason(val position: Long) {
    /**
     * Playback has automatically transitioned to the next [AudioItem].
     *
     * This reason also indicates a transition caused by another player.
     */
    class AUTO(position: Long) : AudioItemTransitionReason(position)

    /**
     * A seek to another [AudioItem] has occurred. Usually triggered when calling
     * [QueuedAudioPlayer.next][com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer.next]
     * or [QueuedAudioPlayer.previous][com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer.previous].
     */
    class SEEK_TO_ANOTHER_AUDIO_ITEM(position: Long) : AudioItemTransitionReason(position)

    /**
     * The [AudioItem] has been repeated.
     */
    class REPEAT(position: Long) : AudioItemTransitionReason(position)

    /**
     * The current [AudioItem] has changed because of a change in the queue. This can either be if
     * the [AudioItem] previously being played has been removed, or when the queue becomes non-empty
     * after being empty.
     */
    class QUEUE_CHANGED(position: Long) : AudioItemTransitionReason(position)
}
