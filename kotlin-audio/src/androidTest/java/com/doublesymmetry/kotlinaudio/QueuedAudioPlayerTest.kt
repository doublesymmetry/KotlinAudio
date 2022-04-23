package com.doublesymmetry.kotlinaudio

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.doublesymmetry.kotlinaudio.models.*
import com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Rule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

private object SharedPlayer {
    private var queuedAudioPlayer: QueuedAudioPlayer? = null

    val instance: QueuedAudioPlayer
        get() {
            if (queuedAudioPlayer == null) {
                val appContext = InstrumentationRegistry.getInstrumentation().targetContext
                queuedAudioPlayer = QueuedAudioPlayer(
                    appContext,
                    cacheConfig = CacheConfig(maxCacheSize = (1024 * 50).toLong())
                )
            }
            return queuedAudioPlayer!!
        }
}

class QueuedAudioPlayerTest {
    private val scope = MainScope()

    @Rule
    var mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.INTERNET)

    @BeforeEach
    fun setup() {
        scope.launch {
            SharedPlayer.instance.stop()
        }
    }

    @Nested
    inner class CurrentItem {
        @Test
        fun thenReturnNull() {
            scope.launch {
                assertNull(SharedPlayer.instance.currentItem)
            }
        }

        @Test
        fun whenAddingOneItem_thenReturnNotNull() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)

                assertNotNull(SharedPlayer.instance.currentItem)
            }
        }

        @Test
        fun whenAddingOneItemAndLoadingAnother_thenShouldHaveReplacedItem() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.load(shortTestSound, playWhenReady = false)

                assertEquals(shortTestSound.audioUrl, SharedPlayer.instance.currentItem?.audioUrl)
            }
        }

        @Test
        fun whenAddingMultipleItems_thenReturnNotNull() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)

                assertNotNull(SharedPlayer.instance.currentItem)
            }
        }
    }

    @Nested
    inner class NextItems {
        @Test
        fun thenBeEmpty() {
            scope.launch {
                assertTrue(SharedPlayer.instance.nextItems.isEmpty())
            }
        }

        @Test
        fun whenAddingTwoItems_thenShouldContainOneItem() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)

                assertEquals(1, SharedPlayer.instance.nextItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndCallingNext_thenShouldContainZeroItems() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)
                SharedPlayer.instance.next()

                assertEquals(0, SharedPlayer.instance.nextItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndCallingNextAndCallingPrevious_thenShouldContainOneItem() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)
                SharedPlayer.instance.next()
                SharedPlayer.instance.previous()

                assertEquals(1, SharedPlayer.instance.nextItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndRemovingOneItem_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)
                SharedPlayer.instance.remove(1)

                assertTrue(SharedPlayer.instance.nextItems.isEmpty())
            }
        }

        @Test
        fun whenAddingTwoItemsAndJumpingToLastItem_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)
                SharedPlayer.instance.jumpToItem(1)

                assertTrue(SharedPlayer.instance.nextItems.isEmpty())
            }
        }

        @Test
        fun whenAddingTwoItemsAndRemovingUpcomingItems_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)
                SharedPlayer.instance.removeUpcomingItems()

                assertTrue(SharedPlayer.instance.nextItems.isEmpty())
            }
        }

        @Test
        fun whenAddingTwoItemsAndStopping_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)
                SharedPlayer.instance.stop()

                assertTrue(SharedPlayer.instance.nextItems.isEmpty())
            }
        }
    }

    @Nested
    inner class PreviousItems {
        @Test
        fun thenShouleBeEmpty() {
            assertTrue(SharedPlayer.instance.previousItems.isEmpty())
        }

        @Test
        fun whenAddingTwoItems_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)

                assertEquals(0, SharedPlayer.instance.previousItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndCallingNext_thenShouldHaveOneItem() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)
                SharedPlayer.instance.next()

                assertEquals(1, SharedPlayer.instance.previousItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndRemovingPreviousItems_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)
                SharedPlayer.instance.next()
                SharedPlayer.instance.removePreviousItems()

                assertTrue(SharedPlayer.instance.previousItems.isEmpty())
            }
        }

        @Test
        fun whenAddingTwoItemsAndStopping_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(testSound, playWhenReady = false)
                SharedPlayer.instance.add(shortTestSound, playWhenReady = false)
                SharedPlayer.instance.stop()

                assertTrue(SharedPlayer.instance.previousItems.isEmpty())
            }
        }
    }

    @Nested
    inner class OnNext {
        @Test
        fun givenPlayerIsPlayingAndCallingNext_thenShouldGoToNextAndPlay() {
            scope.launch {
                SharedPlayer.instance.add(listOf(testSound, shortTestSound))
                SharedPlayer.instance.next()

                assertEquals(shortTestSound, SharedPlayer.instance.currentItem)
                assertEquals(0, SharedPlayer.instance.nextItems.size)
                assertEquals(1, SharedPlayer.instance.currentIndex)

                assertEquals(AudioPlayerState.BUFFERING, SharedPlayer.instance.playerState)
            }
        }

        @Test
        fun givenPlayerIsPausedAndCallingNext_thenShouldGoToNextAndNotPlay() {
            scope.launch {
                SharedPlayer.instance.add(listOf(testSound, shortTestSound), playWhenReady = false)
                SharedPlayer.instance.pause()
                SharedPlayer.instance.next()

                assertEquals(shortTestSound, SharedPlayer.instance.currentItem)
                assertEquals(0, SharedPlayer.instance.nextItems.size)
                assertEquals(1, SharedPlayer.instance.currentIndex)

                assertEquals(AudioPlayerState.LOADING, SharedPlayer.instance.playerState)
            }
        }
    }

    @Nested
    inner class OnPrevious {
        @Test
        fun givenPlayerIsPlayingAndCallingPrevious_thenShouldGoToPreviousAndPlay() {
            scope.launch {
                SharedPlayer.instance.add(listOf(testSound, shortTestSound))
                SharedPlayer.instance.next()
                assertEquals(shortTestSound, SharedPlayer.instance.currentItem)
                SharedPlayer.instance.previous()

                assertEquals(testSound, SharedPlayer.instance.currentItem)
                assertEquals(1, SharedPlayer.instance.nextItems.size)
                assertEquals(0, SharedPlayer.instance.previousItems.size)
                assertEquals(0, SharedPlayer.instance.currentIndex)

                assertEquals(AudioPlayerState.BUFFERING, SharedPlayer.instance.playerState)
            }
        }

        @Test
        fun givenPlayerIsPausedAndCallingPrevious_thenShouldGoToPreviousAndNotPlay() {
            scope.launch {
                SharedPlayer.instance.add(listOf(testSound, shortTestSound), playWhenReady = false)
                SharedPlayer.instance.next()
                assertEquals(shortTestSound, SharedPlayer.instance.currentItem)
                SharedPlayer.instance.pause()
                SharedPlayer.instance.previous()

                assertEquals(testSound, SharedPlayer.instance.currentItem)
                assertEquals(1, SharedPlayer.instance.nextItems.size)
                assertEquals(0, SharedPlayer.instance.previousItems.size)
                assertEquals(0, SharedPlayer.instance.currentIndex)

                assertEquals(AudioPlayerState.LOADING, SharedPlayer.instance.playerState)
            }
        }
    }

    @Nested
    inner class RepeatMode {
        @Nested
        inner class Off {
            @Test
            fun whenAllowPlaybackToEnd_thenShouldMoveToNextItem() {
                scope.launch {
                    InstrumentationRegistry.getInstrumentation().targetContext.assets

                    SharedPlayer.instance.add(listOf(testSound, shortTestSound))
                    SharedPlayer.instance.playerOptions.repeatMode =
                        com.doublesymmetry.kotlinaudio.models.RepeatMode.OFF
                    SharedPlayer.instance.seek(0.0682.toLong(), TimeUnit.SECONDS)
                    SharedPlayer.instance.play()

                    assertEquals(AudioPlayerState.BUFFERING, SharedPlayer.instance.playerState)

//                    await()
//                        .pollInSameThread()
//                        .untilCallTo { SharedPlayer.instance.nextItems }
//                        .matches { it?.isEmpty() == true }
                }
            }
        }
    }

    companion object {
        private val testSound = DefaultAudioItem(
            "asset:///audio/TestSound.m4a", MediaType.DEFAULT,
        )

        private val shortTestSound = DefaultAudioItem(
            "asset:///audio/ShortTestSound.m4a", MediaType.DEFAULT,
        )
    }
}