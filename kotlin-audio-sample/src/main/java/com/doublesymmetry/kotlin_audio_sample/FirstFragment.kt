package com.doublesymmetry.kotlin_audio_sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.doublesymmetry.kotlin_audio_sample.databinding.FragmentFirstBinding
import com.doublesymmetry.kotlinaudio.models.*
import com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private lateinit var player: QueuedAudioPlayer

    enum class SeekDirection { Forward, Backward }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        player = QueuedAudioPlayer(requireActivity(), playerConfig = PlayerConfig(interceptPlayerActionsTriggeredExternally = true))
        player.add(firstItem)
        player.add(secondItem)
        player.play()

        binding.buttonNext.setOnClickListener {
            player.next()
        }

        binding.buttonPrevious.setOnClickListener {
            player.previous()
        }

        binding.buttonPlay.setOnClickListener {
            player.play()
        }

        binding.buttonPause.setOnClickListener {
            player.pause()
        }

        binding.buttonRewind.setOnClickListener {
            seek(SeekDirection.Backward)
        }

        binding.buttonForward.setOnClickListener {
            seek(SeekDirection.Forward)
        }

        setupNotification()
        observeEvents()
    }

    private fun seek(direction: SeekDirection) {
        val seekTime = when (direction) {
            SeekDirection.Forward -> player.position + 1000
            SeekDirection.Backward -> player.position - 1000L
        }

        player.seek(seekTime, TimeUnit.MILLISECONDS)
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    player.event.stateChange.collect {
                        binding.textviewStatus.text = it.name
                        when (it) {
                            AudioPlayerState.PLAYING -> {
                                binding.buttonPlay.isEnabled = false
                                binding.buttonPause.isEnabled = true
                            }
                            else -> {
                                binding.buttonPlay.isEnabled = true
                                binding.buttonPause.isEnabled = false
                            }
                        }
                    }
                }

                launch {
                    player.event.audioItemTransition.collect {
                        binding.textviewTitle.text = player.currentItem?.title
                        binding.textviewArtist.text = player.currentItem?.artist
                        binding.textviewQueue.text = "${player.currentIndex + 1} / ${player.items.size}"
                    }
                }

                launch {
                    player.event.onPlayerActionTriggeredExternally.collect {
                        Timber.d(it.toString())
                        when (it) {
                            MediaSessionCallback.PLAY -> player.play()
                            MediaSessionCallback.PAUSE -> player.pause()
                            MediaSessionCallback.NEXT -> player.next()
                            MediaSessionCallback.PREVIOUS -> player.previous()
                            MediaSessionCallback.STOP -> player.stop()
                            MediaSessionCallback.FORWARD -> seek(SeekDirection.Forward)
                            MediaSessionCallback.REWIND -> seek(SeekDirection.Backward)
                            is MediaSessionCallback.SEEK -> player.seek(it.positionMs, TimeUnit.MILLISECONDS)
                            else -> Timber.d("Event not handled")
                        }
                    }
                }
            }
        }
    }

    private fun setupNotification() {
        val notificationConfig = NotificationConfig(
            listOf(
                NotificationButton.PLAY_PAUSE(),
                NotificationButton.STOP(),
                NotificationButton.NEXT(isCompact = true),
                NotificationButton.PREVIOUS(isCompact = true),
                NotificationButton.BACKWARD(isCompact = true)
            ), null, null, null
        )
        player.notificationManager.createNotification(notificationConfig)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val firstItem = DefaultAudioItem(
            "https://cdn.pixabay.com/download/audio/2022/08/31/audio_419263fc12.mp3?filename=leonell-cassio-the-blackest-bouquet-118766.mp3", MediaType.DEFAULT,
            title = "Song 1",
            artwork = "https://upload.wikimedia.org/wikipedia/en/0/0b/DirtyComputer.png",
            artist = "Artist 1",
            duration = 221000
        )

        val secondItem = DefaultAudioItem(
            "https://cdn.pixabay.com/download/audio/2022/08/25/audio_4f3b0a816e.mp3?filename=tuesday-glitch-soft-hip-hop-118327.mp3", MediaType.DEFAULT,
            title = "Song 2",
            artwork = "https://images-na.ssl-images-amazon.com/images/I/A18QUHExFgL._SL1500_.jpg",
            artist = "Artist 2",
            duration = 127000
        )
    }
}
