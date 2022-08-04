package com.doublesymmetry.kotlinaudio.event

import com.doublesymmetry.kotlinaudio.models.*
import com.google.android.exoplayer2.PlaybackException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerEventHolder {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var _stateChange = MutableStateFlow(AudioPlayerState.IDLE)
    var stateChange = _stateChange.asStateFlow()

    private var _playbackEnd = MutableSharedFlow<PlaybackEndedReason?>(1)
    var playbackEnd = _playbackEnd.asSharedFlow()

    private var _audioItemTransition = MutableSharedFlow<AudioItemTransitionReason?>(1)
    var audioItemTransition = _audioItemTransition.asSharedFlow()

    private var _onAudioFocusChanged = MutableSharedFlow<FocusChangeData>(1)
    var onAudioFocusChanged = _onAudioFocusChanged.asSharedFlow()

    private var _onPlaybackMetadata = MutableSharedFlow<PlaybackMetadata>(1)
    var onPlaybackMetadata = _onPlaybackMetadata.asSharedFlow()

    private var _onPlaybackException = MutableSharedFlow<PlaybackException>(1)
    var onPlaybackException = _onPlaybackException.asSharedFlow()

    internal fun updateAudioPlayerState(state: AudioPlayerState) {
        coroutineScope.launch {
            _stateChange.emit(state)
        }
    }

    internal fun updatePlaybackEndedReason(reason: PlaybackEndedReason) {
        coroutineScope.launch {
            _playbackEnd.emit(reason)
        }
    }

    internal fun updateAudioItemTransition(reason: AudioItemTransitionReason) {
        coroutineScope.launch {
            _audioItemTransition.emit(reason)
        }
    }


    internal fun updateOnAudioFocusChanged(isPaused: Boolean, isPermanent: Boolean) {
        coroutineScope.launch {
            _onAudioFocusChanged.emit(FocusChangeData(isPaused, isPermanent))
        }
    }

    internal fun updateOnPlaybackMetadata(metadata: PlaybackMetadata) {
        coroutineScope.launch {
            _onPlaybackMetadata.emit(metadata)
        }
    }

    internal fun updateOnPlaybackException(error: PlaybackException) {
        coroutineScope.launch {
            _onPlaybackException.emit(error)
        }
    }
}