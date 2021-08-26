package com.doublesymmetry.kotlin_audio_sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class QueuedAudioPlayerTest {
    //region currentItem
    @Test
    fun currentItem_should_be_null() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val audioPlayer = QueuedAudioPlayer(appContext)

        assertNull(audioPlayer.currentItem)
    }
    @Test
    fun currentItem_when_adding_one_item_should_not_be_null() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val audioPlayer = QueuedAudioPlayer(appContext)

        audioPlayer.add(firstItem, playWhenReady = false)
        assertNotNull(audioPlayer.currentItem)
    }
    @Test
    fun currentItem_when_adding_one_item_then_loading_a_new_item() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val audioPlayer = QueuedAudioPlayer(appContext)

        audioPlayer.add(firstItem, playWhenReady = false)
        audioPlayer.load(secondItem, playWhenReady = false)
        assertNotEquals(audioPlayer.currentItem?.audioUrl, firstItem.audioUrl)
    }
    @Test
    fun currentItem_when_adding_multiple_items_should_not_be_null() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val audioPlayer = QueuedAudioPlayer(appContext)

        audioPlayer.add(listOf(firstItem, secondItem), playWhenReady = false)
        assertNotNull(audioPlayer.currentItem)
    }
    //endregion

    //region nextItems
    @Test
    fun nextItems_should_be_empty() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val audioPlayer = QueuedAudioPlayer(appContext)

        assertEquals(audioPlayer.nextItems.size, 0)
    }

    @Test
    fun nextItems_when_adding_two_items_should_contain_one_item() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val audioPlayer = QueuedAudioPlayer(appContext)

        audioPlayer.add(listOf(firstItem, secondItem), playWhenReady = false)
        assertEquals(audioPlayer.nextItems.size, 1)
    }

    @Test
    fun nextItems_when_adding_two_items_then_calling_next_should_contain_zero_items() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val audioPlayer = QueuedAudioPlayer(appContext)

        audioPlayer.add(listOf(firstItem, secondItem), playWhenReady = false)
        audioPlayer.next()
        assertEquals(audioPlayer.nextItems.size, 0)
    }

    @Test
    fun nextItems_when_adding_two_items_then_calling_next_then_calling_previous_should_contain_one_item() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val audioPlayer = QueuedAudioPlayer(appContext)

        audioPlayer.add(listOf(firstItem, secondItem), playWhenReady = false)
        audioPlayer.next()
        audioPlayer.previous()
        assertEquals(audioPlayer.nextItems.size, 1)
    }
    //endregion
}