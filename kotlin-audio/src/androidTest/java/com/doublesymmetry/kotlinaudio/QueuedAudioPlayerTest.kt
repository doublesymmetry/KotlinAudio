package com.doublesymmetry.kotlinaudio

import androidx.test.platform.app.InstrumentationRegistry
import com.doublesymmetry.kotlinaudio.models.*
import com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer
import com.doublesymmetry.kotlinaudio.utils.*
import com.doublesymmetry.kotlinaudio.models.RepeatMode.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.time.Duration
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
            testPlayer.add(TestSound.default)

            assertNotNull(testPlayer.currentItem)
        }

        @Test
        fun givenAddedOneItemAndLoadingAnother_thenShouldHaveReplacedItem() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.load(TestSound.long)

            assertEquals(TestSound.long.audioUrl, testPlayer.currentItem?.audioUrl)
        }

        @Test
        fun givenAddedTwoItemAndMovingFirstAboveSecond_thenShouldHaveMovedItem() = runBlocking(Dispatchers.Main) {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val audioPlayer = QueuedAudioPlayer(appContext)

            audioPlayer.add(TestSound.short)
            audioPlayer.add(TestSound.long)
            assertEquals(audioPlayer.currentItem?.audioUrl, audioPlayer.items[0].audioUrl)
            assertEquals(audioPlayer.currentItem?.audioUrl, TestSound.short.audioUrl)
            audioPlayer.move(0, 1)
            assertEquals(audioPlayer.currentItem?.audioUrl, audioPlayer.items[1].audioUrl)
            assertNotEquals(audioPlayer.currentItem?.audioUrl, audioPlayer.items[0].audioUrl)
        }


        @Test
        fun givenAddedMultipleItems_thenReturnNotNull() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)

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
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)

            assertEquals(1, testPlayer.nextItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndCallingNext_thenShouldContainZeroItems() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)
            testPlayer.next()

            assertEquals(0, testPlayer.nextItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndCallingNextAndPrevious_thenShouldContainOneItem() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)
            testPlayer.next()
            testPlayer.previous()

            assertEquals(1, testPlayer.nextItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndRemovingLastItem_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)
            testPlayer.remove(1)

            assertTrue(testPlayer.nextItems.isEmpty())
        }

        @Test
        fun givenAddedTwoItemsAndJumpingToLast_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)
            testPlayer.jumpToItem(1)

            assertTrue(testPlayer.nextItems.isEmpty())
        }

        @Test
        fun givenAddedTwoItemsAndRemovingUpcomingItems_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)
            testPlayer.removeUpcomingItems()

            assertTrue(testPlayer.nextItems.isEmpty())
        }

        @Test
        fun giveNoItems_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(emptyList())
            testPlayer.removeUpcomingItems()
            assertEquals(testPlayer.nextItems.size, 0)
        }

        @Test
        fun givenAddedTwoItemsAndStopping_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)
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
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)

            assertEquals(0, testPlayer.previousItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndCallingNext_thenShouldHaveOneItem() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)
            testPlayer.next()

            assertEquals(1, testPlayer.previousItems.size)
        }

        @Test
        fun givenAddedTwoItemsAndRemovedPreviousItems_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)
            testPlayer.next()
            testPlayer.removePreviousItems()

            assertTrue(testPlayer.previousItems.isEmpty())
        }

        @Test
        fun givenAddedTwoItemsAndStopped_thenShouldBeEmpty() = runBlocking(Dispatchers.Main) {
            testPlayer.add(TestSound.short)
            testPlayer.add(TestSound.long)
            testPlayer.stop()

            assertTrue(testPlayer.previousItems.isEmpty())
        }
    }

    @Nested
    inner class OnNext {
        @Test
        fun givenPlayerIsPlayingAndCallingNext_thenShouldGoToNextAndPlay() = runBlocking(Dispatchers.Main) {
            testPlayer.play()
            testPlayer.add(listOf(TestSound.short, TestSound.long))
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
            testPlayer.add(listOf(TestSound.short, TestSound.long))
            testPlayer.next()

            eventually {
                assertEquals(1, testPlayer.previousItems.size)
                assertEquals(0, testPlayer.nextItems.size)
                assertEquals(TestSound.long, testPlayer.currentItem)
                assertEquals(AudioPlayerState.READY, testPlayer.playerState)
            }
        }
    }

    @Nested
    inner class OnPrevious {
        @Test
        fun givenPlayerIsPlayingAndCallingPrevious_thenShouldGoToPreviousAndPlay() = runBlocking(Dispatchers.Main) {
            var first = TestSound.fiveSeconds
            var second = TestSound.short
            testPlayer.play()
            testPlayer.add(listOf(first, second))
            assertEquals(first, testPlayer.currentItem)
            testPlayer.next()
            assertEquals(second, testPlayer.currentItem)
            testPlayer.previous()
            assertEquals(first, testPlayer.currentItem)
            testPlayer.seekAndWaitForNextTrackTransition(4.5.toLong(), TimeUnit.SECONDS)
            eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                assertEquals(1, testPlayer.previousItems.size)
                assertEquals(0, testPlayer.nextItems.size)
                assertEquals(second, testPlayer.currentItem)
                assertEquals(AudioPlayerState.ENDED, testPlayer.playerState)
            })
        }

        @Test
        fun givenPlayerIsPausedAndCallingPrevious_thenShouldGoToPreviousAndNotPlay() = runBlocking(Dispatchers.Main) {
            testPlayer.pause()
            testPlayer.add(listOf(TestSound.short, TestSound.long))
            testPlayer.next()
            assertEquals(TestSound.long, testPlayer.currentItem)
            testPlayer.previous()
            eventually {
                assertEquals(0, testPlayer.previousItems.size)
                assertEquals(1, testPlayer.nextItems.size)
                assertEquals(testPlayer.currentItem, TestSound.short)
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
                testPlayer.play()
                testPlayer.add(listOf(TestSound.short, TestSound.fiveSeconds))
                testPlayer.playerOptions.repeatMode = OFF
                testPlayer.seekAndWaitForNextTrackTransition(0.0682.toLong(), TimeUnit.SECONDS)

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.fiveSeconds, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                })
            }

            // TODO: Fix known bug from: https://github.com/doublesymmetry/react-native-track-player/pull/1501
            @Test
            fun givenAddedTwoItemsAndAllowingPlaybackToEndTwice_whenRepeatModeOff_thenShouldStopPlayback() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(listOf(TestSound.short, TestSound.short))
                testPlayer.playerOptions.repeatMode = OFF
                testPlayer.seekAndWaitForNextTrackTransition(0.0682.toLong(), TimeUnit.SECONDS)
                testPlayer.seekAndWaitForNextTrackTransition(0.0682.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.short, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.ENDED, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndCallingNext_whenRepeatModeOff_thenShouldMoveToNextItemAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(listOf(TestSound.short, TestSound.long))
                testPlayer.playerOptions.repeatMode = OFF
                testPlayer.nextAndWaitForNextTrackTransition()

                eventually {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndCallingNextTwice_thenShouldDoNothingOnSecondNext() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(listOf(TestSound.short, TestSound.fiveSeconds))
                testPlayer.playerOptions.repeatMode = OFF
                testPlayer.nextAndWaitForNextTrackTransition()
                testPlayer.next()

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.fiveSeconds, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                })
            }

            @Test
            fun givenAddedOneItemAndAllowingPlaybackToEnd_thenShouldStopPlayback() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(TestSound.short)
                testPlayer.playerOptions.repeatMode = OFF
                testPlayer.seekAndWaitForNextTrackTransition(0.0682.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.short, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.ENDED, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedOneItemAndCallingNext_thenShouldDoNothing() = runBlocking(Dispatchers.Main) {
                testPlayer.add(TestSound.fiveSeconds, true)
                testPlayer.playerOptions.repeatMode = OFF
                testPlayer.nextAndWaitForNextTrackTransition()

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.fiveSeconds, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                })
            }
        }

        @Nested
        inner class Track {
            @Test
            fun givenAddedTwoItemsAndAllowingPlaybackToEnd_whenRepeatModeOne_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(listOf(TestSound.fiveSeconds, TestSound.short))
                testPlayer.playerOptions.repeatMode = ONE
                testPlayer.seekAndWaitForNextTrackTransition(4.5.toLong(), TimeUnit.SECONDS)

                eventually {
                    assertTrue(testPlayer.position < 4500)
                    assertEquals(1, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndCallingNext_whenRepeatModeOne_thenShouldMoveToNextItemAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(listOf(TestSound.short, TestSound.long))
                testPlayer.playerOptions.repeatMode = ONE
                testPlayer.nextAndWaitForNextTrackTransition()

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                })
            }

            @Test
            fun givenAddedOneItemAndAllowingPlaybackToEnd_whenRepeatModeOne_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(TestSound.fiveSeconds)
                testPlayer.playerOptions.repeatMode = ONE
                testPlayer.seekAndWaitForNextTrackTransition(4.5.toLong(), TimeUnit.SECONDS)
                val currentPosition = testPlayer.position

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertTrue(testPlayer.position < currentPosition)
                    assertEquals(0, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                })
            }

            @Test
            fun givenAddedOneItemAndCallingNext_whenRepeatModeOne_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(TestSound.fiveSeconds)
                testPlayer.playerOptions.repeatMode = ONE
                testPlayer.nextAndWaitForNextTrackTransition()

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertTrue(testPlayer.position > 0)
                    assertEquals(0, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                })
            }
        }

        @Nested
        inner class Queue {
            @Test
            fun givenAddedTwoItemsAndAllowingPlaybackToEnd_whenRepeatModeAll_thenShouldMoveToNextItemAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(listOf(TestSound.fiveSeconds, TestSound.long))
                testPlayer.playerOptions.repeatMode = ALL
                testPlayer.seekAndWaitForNextTrackTransition(4.5.toLong(), TimeUnit.SECONDS)

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.long, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                })
            }

            @Test
            fun givenAddedTwoItemsAndAllowingPlaybackToEndTwice_whenRepeatModeAll_thenShouldMoveToFirstTrackAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(listOf(TestSound.long, TestSound.fiveSeconds))
                testPlayer.playerOptions.repeatMode = ALL
                testPlayer.nextAndWaitForNextTrackTransition()
                testPlayer.seekAndWaitForNextTrackTransition(4.5.toLong(), TimeUnit.SECONDS)

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                    assertEquals(1, testPlayer.nextItems.size)
                    assertEquals(TestSound.long, testPlayer.currentItem)
                })
            }

            @Test
            fun givenAddedTwoItemsAndCallingNext_whenRepeatModeAll_thenShouldMoveToNextItemAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(listOf(TestSound.fiveSeconds, TestSound.fiveSeconds2))
                testPlayer.playerOptions.repeatMode = ALL
                testPlayer.nextAndWaitForNextTrackTransition()

                eventually {
                    assertTrue(testPlayer.nextItems.isEmpty())
                    assertEquals(TestSound.fiveSeconds2, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

            @Test
            fun givenAddedTwoItemsAndCallingNextTwice_whenRepeatModeAll_thenShouldMoveToFirstTrackAndPlay() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(listOf(TestSound.fiveSeconds, TestSound.short))
                testPlayer.playerOptions.repeatMode = ALL
                testPlayer.nextAndWaitForNextTrackTransition()
                testPlayer.nextAndWaitForNextTrackTransition()

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertEquals(1, testPlayer.nextItems.size)
                    assertEquals(TestSound.fiveSeconds, testPlayer.currentItem)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                })
            }

            @Test
            fun givenAddedOneItemAndAllowingPlaybackToEnd_whenRepeatModeAll_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.playerOptions.repeatMode = ALL
                testPlayer.add(TestSound.fiveSeconds)
                testPlayer.seekAndWaitForNextTrackTransition(4.5.toLong(), TimeUnit.SECONDS)

                eventually(Duration.ofSeconds(30), Dispatchers.Main, fun () {
                    assertTrue(testPlayer.position < 4000 && testPlayer.position > 0)
                    assertEquals(0, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                })
            }

            @Test
            fun givenAddedOneItemAndCallingNext_whenRepeatModeAll_thenShouldRestartCurrentItem() = runBlocking(Dispatchers.Main) {
                testPlayer.play()
                testPlayer.add(TestSound.fiveSeconds)
                testPlayer.playerOptions.repeatMode = ALL
                testPlayer.nextAndWaitForNextTrackTransition()

                eventually {
                    assertTrue(testPlayer.position > 0)
                    assertEquals(0, testPlayer.nextItems.size)
                    assertEquals(0, testPlayer.currentIndex)
                    assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
                }
            }

        }
    }

    @Nested
    inner class QueueManipulations {
        @Nested
        inner class Remove {
            @Test
            fun givenAddedThreeItemsAnDeletingWithIndexesInNonDescendingOrder_thenShouldDeleteCorrectItems() =
                runBlocking(Dispatchers.Main) {
                    testPlayer.add(listOf(TestSound.short, TestSound.fiveSeconds, TestSound.fiveSeconds2))
                    testPlayer.remove(listOf(0, 1))
                    assertEquals(testPlayer.items.size, 1)
                    assertEquals(testPlayer.items[0], TestSound.fiveSeconds2)
                }
        }
    }
}
