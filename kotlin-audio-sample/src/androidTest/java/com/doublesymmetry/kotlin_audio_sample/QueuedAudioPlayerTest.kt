package com.doublesymmetry.kotlin_audio_sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class QueuedAudioPlayerTest {
    @Test
    fun load_new_item() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val audioPlayer = QueuedAudioPlayer(appContext)
        assertNull(audioPlayer.currentItem)
        audioPlayer.add(firstItem, playWhenReady = false)
        assertNotNull(audioPlayer.currentItem)
    }
}