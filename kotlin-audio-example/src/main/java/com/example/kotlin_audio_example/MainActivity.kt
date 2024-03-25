package com.example.kotlin_audio_example

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.doublesymmetry.kotlinaudio.models.AudioPlayerState
import com.doublesymmetry.kotlinaudio.models.DefaultAudioItem
import com.doublesymmetry.kotlinaudio.models.MediaSessionCallback
import com.doublesymmetry.kotlinaudio.models.AAMediaSessionCallBack
import com.doublesymmetry.kotlinaudio.models.MediaType
import com.doublesymmetry.kotlinaudio.models.NotificationButton
import com.doublesymmetry.kotlinaudio.models.NotificationConfig
import com.doublesymmetry.kotlinaudio.models.RepeatMode
import com.doublesymmetry.kotlinaudio.models.PlayerConfig
import com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer
import com.example.kotlin_audio_example.ui.component.ActionBottomSheet
import com.example.kotlin_audio_example.ui.component.PlayerControls
import com.example.kotlin_audio_example.ui.component.TrackDisplay
import com.example.kotlin_audio_example.ui.theme.KotlinAudioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    private lateinit var player: QueuedAudioPlayer

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.plant(Timber.DebugTree())
        super.onCreate(savedInstanceState)
        val mAAMediaSessionCallBack = object: AAMediaSessionCallBack {
            override fun handlePlayFromMediaId(mediaId: String?, extras: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun handlePlayFromSearch(query: String?, extras: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun handleSkipToQueueItem(id: Long) {
                TODO("Not yet implemented")
            }
        }
        player = QueuedAudioPlayer(
            this, playerConfig = PlayerConfig(
                interceptPlayerActionsTriggeredExternally = true,
                handleAudioBecomingNoisy = true,
                handleAudioFocus = true
            ),
            mediaSessionCallback = mAAMediaSessionCallBack,
        )
        player.add(tracks)
        player.playerOptions.repeatMode = RepeatMode.ALL
        player.play()

        setupNotification()

        setContent {
            val state = player.event.stateChange.collectAsState(initial = AudioPlayerState.IDLE)
            var title by remember { mutableStateOf("") }
            var artist by remember { mutableStateOf("") }
            var artwork by remember { mutableStateOf("") }
            var position by remember { mutableStateOf(0L) }
            var duration by remember { mutableStateOf(0L) }
            var isLive by remember { mutableStateOf(false) }

            var showSheet by remember { mutableStateOf(false) }

            if (showSheet) {
                ActionBottomSheet(
                    onDismiss = { showSheet = false },
                    onRandomMetadata = {
                        val currentIndex = player.currentIndex
                        val track = tracks[currentIndex].copy(
                            title = "Random Title - ${System.currentTimeMillis()}",
                            artwork = "https://random.imagecdn.app/800/800?dummy=${System.currentTimeMillis()}",
                        )
                        player.replaceItem(currentIndex, track)
                    }
                )
            }

            KotlinAudioTheme {
                MainScreen(
                    title = title,
                    artist = artist,
                    artwork = artwork,
                    position = position,
                    duration = duration,
                    isLive = isLive,
                    onPrevious = { player.previous() },
                    onNext = { player.next() },
                    isPaused = state.value != AudioPlayerState.PLAYING,
                    onTopBarAction = { showSheet = true },
                    onPlayPause = {
                        if (player.playerState == AudioPlayerState.PLAYING) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    },
                    onSeek = { player.seek(it, TimeUnit.MILLISECONDS) }
                )
            }

            LaunchedEffect(key1 = player, key2 = player.event.audioItemTransition, key3 = player.event.onPlayerActionTriggeredExternally) {
                player.event.audioItemTransition
                    .onEach {
                        title = player.currentItem?.title ?: ""
                        artist = player.currentItem?.artist ?: ""
                        artwork = player.currentItem?.artwork ?: ""
                        duration =  player.currentItem?.duration ?: 0
                        isLive = player.isCurrentMediaItemLive
                    }
                    .launchIn(this)

                player.event.onPlayerActionTriggeredExternally
                    .onEach {
                        when (it) {
                            MediaSessionCallback.PLAY -> player.play()
                            MediaSessionCallback.PAUSE -> player.pause()
                            MediaSessionCallback.NEXT -> player.next()
                            MediaSessionCallback.PREVIOUS -> player.previous()
                            MediaSessionCallback.STOP -> player.stop()
                            is MediaSessionCallback.SEEK -> player.seek(
                                it.positionMs,
                                TimeUnit.MILLISECONDS
                            )
                            else -> Timber.d("Event not handled")
                        }
                    }
                    .launchIn(this)
            }

            LaunchedEffect(Unit) {
                while(true) {
                    position = player.position
                    duration = player.duration
                    isLive = player.isCurrentMediaItemLive

                    delay(1.seconds / 30)
                }
            }
        }
    }

    private fun setupNotification() {
        val notificationConfig = NotificationConfig(
            listOf(
                NotificationButton.PLAY_PAUSE(),
                NotificationButton.NEXT(isCompact = true),
                NotificationButton.PREVIOUS(isCompact = true),
                NotificationButton.SEEK_TO
            ), accentColor = null, smallIcon = null, pendingIntent = null
        )
        player.notificationManager.createNotification(notificationConfig)
    }

    companion object {
        val tracks = listOf(
            DefaultAudioItem(
                "https://rntp.dev/example/Longing.mp3",
                MediaType.DEFAULT,
                title = "Longing",
                artwork = "https://rntp.dev/example/Longing.jpeg",
                artist = "David Chavez",
                duration = 143 * 1000,
            ),
            DefaultAudioItem(
                "https://rntp.dev/example/Soul%20Searching.mp3",
                MediaType.DEFAULT,
                title = "Soul Searching (Demo)",
                artwork = "https://rntp.dev/example/Soul%20Searching.jpeg",
                artist = "David Chavez",
                duration = 77 * 1000,
            ),
            DefaultAudioItem(
                "https://rntp.dev/example/Lullaby%20(Demo).mp3",
                MediaType.DEFAULT,
                title = "Lullaby (Demo)",
                artwork = "https://rntp.dev/example/Lullaby%20(Demo).jpeg",
                artist = "David Chavez",
                duration = 71 * 1000,
            ),
            DefaultAudioItem(
                "https://rntp.dev/example/Rhythm%20City%20(Demo).mp3",
                MediaType.DEFAULT,
                title = "Rhythm City (Demo)",
                artwork = "https://rntp.dev/example/Rhythm%20City%20(Demo).jpeg",
                artist = "David Chavez",
                duration = 106 * 1000,
            ),
            DefaultAudioItem(
                "https://rntp.dev/example/hls/whip/playlist.m3u8",
                MediaType.HLS,
                title = "Whip",
                artwork = "https://rntp.dev/example/hls/whip/whip.jpeg",
            ),
            DefaultAudioItem(
                "https://ais-sa5.cdnstream1.com/b75154_128mp3",
                MediaType.DEFAULT,
                title = "Smooth Jazz 24/7",
                artwork = "https://rntp.dev/example/smooth-jazz-24-7.jpeg",
                artist = "New York, NY",
            ),
            DefaultAudioItem(
                "https://traffic.libsyn.com/atpfm/atp545.mp3",
                title = "Chapters",
                artwork = "https://random.imagecdn.app/800/800?dummy=1",
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    title: String,
    artist: String,
    artwork: String,
    position: Long,
    duration: Long,
    isLive: Boolean,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    isPaused: Boolean,
    onTopBarAction: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = "Kotlin Audio Example",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    IconButton(onClick = onTopBarAction) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
            TrackDisplay(
                title = title,
                artist = artist,
                artwork = artwork,
                position = position,
                duration = duration,
                isLive = isLive,
                onSeek = onSeek,
                modifier = Modifier.padding(top = 46.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            PlayerControls(
                onPrevious = onPrevious,
                onNext = onNext,
                isPaused = isPaused,
                onPlayPause = onPlayPause,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 60.dp)
            )
        }
    }
}
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ContentPreview() {
    KotlinAudioTheme {
        MainScreen(
            title = "Title",
            artist = "Artist",
            artwork = "",
            position = 1000,
            duration = 6000,
            isLive = false,
            isPaused = true
        )
    }
}
