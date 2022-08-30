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
import com.doublesymmetry.kotlin_audio_sample.utils.firstItem
import com.doublesymmetry.kotlin_audio_sample.utils.secondItem
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
            seekBackward()
        }

        binding.buttonForward.setOnClickListener {
            seekForward()
        }

        setupNotification()
        observeEvents()
    }

    private fun seekForward() {
        var forward = player.position
        forward += 1000

        player.seek(forward, TimeUnit.MILLISECONDS)
    }

    private fun seekBackward() {
        var rewind = player.position
        rewind -= 1000

        player.seek(rewind, TimeUnit.MILLISECONDS)
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    player.event.stateChange.collect {
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
                            MediaSessionCallback.FORWARD -> seekForward()
                            MediaSessionCallback.REWIND -> seekBackward()
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
            ), null, null, null
        )
        player.notificationManager.createNotification(notificationConfig)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}