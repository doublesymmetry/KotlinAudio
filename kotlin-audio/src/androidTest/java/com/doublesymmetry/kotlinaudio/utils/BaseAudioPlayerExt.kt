package com.doublesymmetry.kotlinaudio.utils

import com.doublesymmetry.kotlinaudio.players.BaseAudioPlayer
import org.junit.jupiter.api.Assertions
import java.time.Duration
import java.util.concurrent.TimeUnit

suspend fun BaseAudioPlayer.seekWithAssertion(duration: Long, unit: TimeUnit) {
    seek(duration, unit)
    assertEventually(Duration.ofSeconds(1)) {
        Assertions.assertEquals(Duration.ofSeconds(duration), Duration.ofMillis(position))
    }
}
