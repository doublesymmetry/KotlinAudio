package com.doublesymmetry.kotlinaudio

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.doublesymmetry.kotlinaudio.models.*
import com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.until
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
                    cacheConfig = CacheConfig(maxCacheSize = 1024 * 50)
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
                SharedPlayer.instance.add(firstItem, playWhenReady = false)

                assertNotNull(SharedPlayer.instance.currentItem)
            }
        }

        @Test
        fun whenAddingOneItemAndLoadingAnother_thenShouldHaveReplacedItem() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.load(secondItem, playWhenReady = false)

                assertEquals(secondItem.audioUrl, SharedPlayer.instance.currentItem?.audioUrl)
            }
        }

        @Test
        fun whenAddingMultipleItems_thenReturnNotNull() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)

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
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)

                assertEquals(1, SharedPlayer.instance.nextItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndCallingNext_thenShouldContainZeroItems() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)
                SharedPlayer.instance.next()

                assertEquals(0, SharedPlayer.instance.nextItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndCallingNextAndCallingPrevious_thenShouldContainOneItem() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)
                SharedPlayer.instance.next()
                SharedPlayer.instance.previous()

                assertEquals(1, SharedPlayer.instance.nextItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndRemovingOneItem_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)
                SharedPlayer.instance.remove(1)

                assertTrue(SharedPlayer.instance.nextItems.isEmpty())
            }
        }

        @Test
        fun whenAddingTwoItemsAndJumpingToLastItem_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)
                SharedPlayer.instance.jumpToItem(1)

                assertTrue(SharedPlayer.instance.nextItems.isEmpty())
            }
        }

        @Test
        fun whenAddingTwoItemsAndRemovingUpcomingItems_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)
                SharedPlayer.instance.removeUpcomingItems()

                assertTrue(SharedPlayer.instance.nextItems.isEmpty())
            }
        }

        @Test
        fun whenAddingTwoItemsAndStopping_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)
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
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)

                assertEquals(0, SharedPlayer.instance.previousItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndCallingNext_thenShouldHaveOneItem() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)
                SharedPlayer.instance.next()

                assertEquals(1, SharedPlayer.instance.previousItems.size)
            }
        }

        @Test
        fun whenAddingTwoItemsAndRemovingPreviousItems_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)
                SharedPlayer.instance.next()
                SharedPlayer.instance.removePreviousItems()

                assertTrue(SharedPlayer.instance.previousItems.isEmpty())
            }
        }

        @Test
        fun whenAddingTwoItemsAndStopping_thenShouldBeEmpty() {
            scope.launch {
                SharedPlayer.instance.add(firstItem, playWhenReady = false)
                SharedPlayer.instance.add(secondItem, playWhenReady = false)
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
                SharedPlayer.instance.add(listOf(firstItem, secondItem))
                SharedPlayer.instance.next()

                assertEquals(secondItem, SharedPlayer.instance.currentItem)
                assertEquals(0, SharedPlayer.instance.nextItems.size)
                assertEquals(1, SharedPlayer.instance.currentIndex)
//                await()
//                    .pollInSameThread()
//                    .until { SharedPlayer.instance.event.stateChange.value == AudioPlayerState.PLAYING }
            }
        }

        @Test
        fun givenPlayerIsPausedAndCallingNext_thenShouldGoToNextAndPlay() {
            scope.launch {
                SharedPlayer.instance.add(listOf(firstItem, secondItem), playWhenReady = false)
                SharedPlayer.instance.pause()
                SharedPlayer.instance.next()

                assertEquals(secondItem, SharedPlayer.instance.currentItem)
                assertEquals(0, SharedPlayer.instance.nextItems.size)
                assertEquals(1, SharedPlayer.instance.currentIndex)
//                await().untilCallTo { SharedPlayer.instance.event.stateChange } matches { it?.value == AudioPlayerState.PLAYING }
            }
        }
    }

    @Nested
    inner class OnPrevious {
        @Test
        fun givenPlayerIsPlayingAndCallingPrevious_thenShouldGoToPreviousAndPlay() {
            scope.launch {
                SharedPlayer.instance.add(listOf(firstItem, secondItem))
                SharedPlayer.instance.next()
                SharedPlayer.instance.previous()

                assertEquals(firstItem, SharedPlayer.instance.currentItem)
                assertEquals(1, SharedPlayer.instance.nextItems.size)
                assertEquals(0, SharedPlayer.instance.previousItems.size)
                assertEquals(0, SharedPlayer.instance.currentIndex)
//                await()
//                    .pollInSameThread()
//                    .until { SharedPlayer.instance.event.stateChange.value == AudioPlayerState.PLAYING }
            }
        }

        @Test
        fun givenPlayerIsPausedAndCallingPrevious_thenShouldGoToPreviousAndPlay() {
            scope.launch {
                SharedPlayer.instance.add(listOf(firstItem, secondItem), playWhenReady = false)
                SharedPlayer.instance.next()
                SharedPlayer.instance.pause()
                SharedPlayer.instance.previous()

                assertEquals(firstItem, SharedPlayer.instance.currentItem)
                assertEquals(1, SharedPlayer.instance.nextItems.size)
                assertEquals(0, SharedPlayer.instance.previousItems.size)
                assertEquals(0, SharedPlayer.instance.currentIndex)
//                await()
//                    .pollInSameThread()
//                    .until { SharedPlayer.instance.event.stateChange.value == AudioPlayerState.PLAYING }
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
                    SharedPlayer.instance.add(listOf(firstItem, secondItem))
                    SharedPlayer.instance.playerOptions.repeatMode =
                        com.doublesymmetry.kotlinaudio.models.RepeatMode.OFF
                    SharedPlayer.instance.seek(0.0682.toLong(), TimeUnit.SECONDS)
                    SharedPlayer.instance.play()

//                    await()
//                        .pollInSameThread()
//                        .until { SharedPlayer.instance.nextItems.isEmpty() }
//                    await()
//                        .pollInSameThread()
//                        .until { SharedPlayer.instance.event.stateChange.value == AudioPlayerState.PLAYING }
                }
            }
        }
    }

    companion object {
        private val firstItem = DefaultAudioItem(
            "https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3", MediaType.DEFAULT,
            title = "Dirty Computer",
            artwork = "https://upload.wikimedia.org/wikipedia/en/0/0b/DirtyComputer.png",
            artist = "Janelle Monáe"
        )

        private val secondItem = DefaultAudioItem(
            "https://file-examples-com.github.io/uploads/2017/11/file_example_MP3_1MG.mp3", MediaType.DEFAULT,
            title = "Melodrama",
            artwork = "https://images-na.ssl-images-amazon.com/images/I/A18QUHExFgL._SL1500_.jpg",
            artist = "Lorde"
        )
    }
}