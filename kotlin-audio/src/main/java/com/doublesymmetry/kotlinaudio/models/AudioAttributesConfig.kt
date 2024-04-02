package com.doublesymmetry.kotlinaudio.models

data class AudioAttributeConfig(

    /**
     * Whether audio focus should be managed automatically. See https://medium.com/google-exoplayer/easy-audio-focus-with-exoplayer-a2dcbbe4640e
     */
    val handleAudioFocus: Boolean = false,
    /**
     * The audio content type.
     */
    val audioContentType: AudioContentType = AudioContentType.MUSIC,

    /**
     * The audio usage type.
     */
    var audioUsageType: AudioUsageType = AudioUsageType.MEDIA
)