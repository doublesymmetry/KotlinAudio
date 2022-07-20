package com.doublesymmetry.kotlinaudio

import androidx.test.platform.app.InstrumentationRegistry
import com.doublesymmetry.kotlinaudio.models.*
import com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer
import com.doublesymmetry.kotlinaudio.utils.*
import com.doublesymmetry.kotlinaudio.models.RepeatMode.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.util.concurrent.TimeUnit

class QueuedAudioPlayerTest {
    private lateinit var testPlayer: QueuedAudioPlayer

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        testPlayer = QueuedAudioPlayer(
            appContext,
            cacheConfig = CacheConfig(maxCacheSize = (1024 * 50).toLong(), identifier = testInfo.displayName)
        )
    }

    @Nested
    inner class CurrentItem {
        @Test
        fun thenReturnNull() = runBlocking(Dispatchers.Main) {
            assertNull(testPlayer.currentItem)
        }

        @Test
        fun givenAddedOneItem_thenReturnNotNull() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)

            assertNotNull(testPlayer.currentItem)
        }

        @Test
        fun givenAddedOneItemAndLoadingAnother_thenShouldHaveReplacedItem() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.load(TestSound.short, playWhenReady = false)

            assertEquals(TestSound.short.audioUrl, testPlayer.currentItem?.audioUrl)
        }

        @Test
        fun givenAddedMultipleItems_thenReturnNotNull() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)

            assertNotNull(testPlayer.currentItem)
        }
    }

    @Nested
    inner class NextItems {
        @Test
        fun thenBeEmpty() = runBlocking(Dispatchers.Main) {
            assertTrue(testPlayer.nextItems.isEmpty())
        }

        @Test
        fun givenAddedTwoItems_thenShouldContainOneItem() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)

            assertEquals(1, testPlayer.nextItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndCallingNext_thenShouldContainZeroItems() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)
            testPlayer.next()

            assertEquals(0, testPlayer.nextItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndCallingNextAndPrevious_thenShouldContainOneItem() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)
            testPlayer.next()
            testPlayer.previous()

            assertEquals(1, testPlayer.nextItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndRemovingLastItem_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)
            testPlayer.remove(1)

            assertTrue(testPlayer.nextItems.isEmpty())
        }

        @Test
        fun givenAddedTwoItemsAndJumpingToLast_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)
            testPlayer.jumpToItem(1)

            assertTrue(testPlayer.nextItems.isEmpty())
        }

        @Test
        fun givenAddedTwoItemsAndRemovingUpcomingItems_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)
            testPlayer.removeUpcomingItems()

            assertTrue(testPlayer.nextItems.isEmpty())
        }

        @Test
        fun givenAddedTwoItemsAndStopping_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)
            testPlayer.stop()

            assertTrue(testPlayer.nextItems.isEmpty())
        }
    }

    @Nested
    inner class PreviousItems {
        @Test
        fun thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            assertTrue(testPlayer.previousItems.isEmpty())
        }

        @Test
        fun givenAddedTwoItems_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)

            assertEquals(0, testPlayer.previousItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndCallingNext_thenShouldHaveOneItem() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)
            testPlayer.next()

            assertEquals(1, testPlayer.previousItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndRemovedPreviousItems_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)
            testPlayer.next()
            testPlayer.removePreviousItems()

            assertTrue(testPlayer.previousItems.isEmpty())
        }

        @Test
        fun givenAddedTwoItemsAndStopped_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.short, playWhenReady = false)
            testPlayer.stop()

            assertTrue(testPlayer.previousItems.isEmpty())
        }
    }

    @Nested
    inner class OnNext {
        @Test
        fun givenPlayerIsPlayingAndCallingNext_thenShouldGoToNextAndPlay() = runBlocking(Dispatchers.Main) {
            testPlayer.add(listOf(TestSound.default, TestSound.long))
            testPlayer.next()

            eventually {
                assertEquals(1, testPlayer.previousItems.size)
                assertEquals(0, testPlayer.nextItems.size)
                assertEquals(TestSound.long, testPlayer.currentItem)
                assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
            }
        }

        @Test
        fun givenPlayerIsPausedAndCallingNext_thenShouldGoToNextAndNotPlay() = runBlocking(Dispatchers.Main) {
            testPlayer.add(listOf(TestSound.default, TestSound.short), playWhenReady = false)
            testPlayer.next()

            eventually {
                assertEquals(1, testPlayer.previousItems.size)
                assertEquals(0, testPlayer.nextItems.size)
                assertEquals(TestSound.short, testPlayer.currentItem)
                assertEquals(AudioPlayerState.READY, testPlayer.playerState)
            }
        }
    }

    @Nested
    inner class OnPrevious {
        @Test
        fun givenPlayerIsPlayingAndCallingPrevious_thenShouldGoToPreviousAndPlay() = runBlocking(Dispatchers.Main) {
            testPlayer.add(listOf(TestSound.long, TestSound.default), playWhenReady = false)
            testPlayer.next()
            assertEquals(TestSound.default, testPlayer.currentItem)
            testPlayer.previous()

            eventually {
                assertEquals(0, testPlayer.previousItems.size)
                assertEquals(1, testPlayer.nextItems.size)
                assertEquals(TestSound.long, testPlayer.currentItem)
                assertEquals(AudioPlayerState.READY, testPlayer.playerState)
            }
        }

        @Test
        fun givenPlayerIsPausedAndCallingPrevious_thenShouldGoToPreviousAndNotPlay() = runBlocking(Dispatchers.Main) {
            testPlayer.add(listOf(TestSound.default, TestSound.short), playWhenReady = false)
            testPlayer.next()
            assertEquals(TestSound.short, testPlayer.currentItem)
            testPlayer.previous()

            eventually {
                assertEquals(0, testPlayer.previousItems.size)
                assertEquals(1, testPlayer.nextItems.size)
                assertEquals(TestSound.default, testPlayer.currentItem)
                assertEquals(AudioPlayerState.READY, testPlayer.playerState)
            }
        }
    }

    @Nested
    inner class RepeatMode {
        @Nested
        inner class Off {
            @Test
            fun givenAddedTwoItemsAndAllowingPlaybackToEnd_whenRepeatModeOff_thenShouldMoveToNextItemAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.short, TestSound.long))
                testPlayer.playerOptions.repeatMode = OFF

                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.seekAndWaitCompletion(0.0682.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndAllowingPlaybackToEndTwice_whenRepeatModeOff_thenShouldStopPlayback() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.short, TestSound.short))
                testPlayer.playerOptions.repeatMode = OFF
                testPlayer.seekAndWaitForNextTrackTransition(0.0682.toLong(), TimeUnit.SECONDS)

                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.seekAndWaitCompletion(0.0682.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.short, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.ENDED, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndCallingNext_whenRepeatModeOff_thenShouldMoveToNextItemAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.short, TestSound.long))
                testPlayer.playerOptions.repeatMode = OFF

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.next()

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndCallingNextTwice_thenShouldDoNothingOnSecondNext() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.short, TestSound.long))
                testPlayer.playerOptions.repeatMode = OFF
                testPlayer.nextAndWaitForNextTrackTransition()
                testPlayer.next()

                eventually {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedOneItemAndAllowingPlaybackToEnd_thenShouldStopPlayback() = runBlocking(Dispatchers.Main) {
                testPlayer.add(TestSound.short)
                testPlayer.playerOptions.repeatMode = OFF

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.seekAndWaitCompletion(0.0682.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertFalse(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.short, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.ENDED, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedOneItemAndCallingNext_thenShouldDoNothing() = runBlocking(Dispatchers.Main) {
                testPlayer.add(TestSound.long)
                testPlayer.playerOptions.repeatMode = OFF

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this, 300) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.SEEK_TO_ANOTHER_AUDIO_ITEM }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.next()

                eventually {
                    assertFalse(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }
        }

        @Nested
        inner class Track {
            //FAILS
            @Test
            fun givenAddedTwoItemsAndAllowingPlaybackToEnd_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.long, TestSound.short))
                testPlayer.playerOptions.repeatMode = ONE

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this, 4000) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.REPEAT }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.seekAndWaitCompletion(347.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.position < 300)
                    assertEquals(1, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndCallingNext_whenRepeatModeOne_thenShouldMoveToNextItemAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.short, TestSound.long))
                testPlayer.playerOptions.repeatMode = ONE

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.REPEAT }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.next()

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            // FAILS
            @Test
            fun givenAddedOneItemAndAllowingPlaybackToEnd_whenRepeatModeOne_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.add(TestSound.long)
                testPlayer.playerOptions.repeatMode = ONE

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this, 10000) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.REPEAT }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.seekAndWaitCompletion(347.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.position < 300)
                    assertEquals(0, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            // FAILS
            @Test
            fun givenAddedOneItemAndCallingNext_whenRepeatModeOne_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.add(TestSound.long)
                testPlayer.playerOptions.repeatMode = ONE

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.REPEAT }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                // TODO: Bug - calling next when repeat mode is ONE does not do anything.
                testPlayer.next()

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.position < 300)
                    assertEquals(0, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }
        }

        @Nested
        inner class Queue {
            @Test
            fun givenAddedTwoItemsAndAllowingPlaybackToEnd_whenRepeatModeAll_thenShouldMoveToNextItemAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.short, TestSound.long))
                testPlayer.playerOptions.repeatMode = ALL

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.seekAndWaitCompletion(0.0682.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndAllowingPlaybackToEndTwice_whenRepeatModeAll_thenShouldMoveToFirstTrackAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.long, TestSound.long2))
                testPlayer.playerOptions.repeatMode = ALL
                testPlayer.seekAndWaitForNextTrackTransition(347.toLong(), TimeUnit.SECONDS)

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.seekAndWaitCompletion(142.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertEquals(1, testPlayer.nextItems.size)
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndCallingNext_whenRepeatModeAll_thenShouldMoveToNextItemAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.short, TestSound.long))
                testPlayer.playerOptions.repeatMode = ALL

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.next()

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndCallingNextTwice_thenShouldMoveToFirstTrackAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.add(listOf(TestSound.long, TestSound.short))
                testPlayer.playerOptions.repeatMode = ALL
                testPlayer.nextAndWaitForNextTrackTransition()

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.next()

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertEquals(1, testPlayer.nextItems.size)
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            // FAILS
            @Test
            fun givenAddedOneItemAndAllowingPlaybackToEnd_whenRepeatModeAll_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.add(TestSound.long)
                testPlayer.playerOptions.repeatMode = ALL

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this, 2000) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.seekAndWaitCompletion(347.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.position < 300)
                    assertEquals(0, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            // FAILS
            @Test
            fun givenAddedOneItemAndCallingNext_whenRepeatModeAll_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.add(TestSound.long)
                testPlayer.playerOptions.repeatMode = ALL

                // setup transition expectations
                var hasMetItemTransitionExpectation = false
                launchWithTimeoutSync(this, 2000) {
                    testPlayer.event.audioItemTransition
                        .waitUntil { it == AudioItemTransitionReason.AUTO }
                        .collect { hasMetItemTransitionExpectation = true }
                }

                testPlayer.next()

                eventually {
//                    assertTrue(hasMetItemTransitionExpectation)
                    assertTrue(testPlayer.position < 300)
                    assertEquals(0, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }
        }
    }
}
