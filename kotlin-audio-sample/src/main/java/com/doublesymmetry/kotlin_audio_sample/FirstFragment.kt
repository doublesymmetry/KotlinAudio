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
        player.add(janelleSound)
        player.add(lordeSound)
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
                NotificationButton.PLAY(),
                NotificationButton.PAUSE(),
                NotificationButton.NEXT(),
                NotificationButton.PREVIOUS()
            ),
            null,
            null,
            null
        )
        player.notificationManager.createNotification(notificationConfig)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val janelleSound = DefaultAudioItem(
            "rawresource:///${R.raw.kalimba}",
            MediaType.DEFAULT,
            title = "Dirty Computer",
            artwork = "https://upload.wikimedia.org/wikipedia/en/0/0b/DirtyComputer.png",
            artist = "Janelle Monáe",
            options = AudioItemOptions(
                resourceId = R.raw.kalimba
            )
        )

        private val lordeSound = DefaultAudioItem(
            "https://file-examples-com.github.io/uploads/2017/11/file_example_MP3_1MG.mp3",
            MediaType.DEFAULT,
            title = "Melodrama",
            artwork = "https://images-na.ssl-images-amazon.com/images/I/A18QUHExFgL._SL1500_.jpg",
            artist = "Lorde"
        )
    }
}
