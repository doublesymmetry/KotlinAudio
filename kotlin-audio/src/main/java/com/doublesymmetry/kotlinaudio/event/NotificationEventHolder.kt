package com.doublesymmetry.kotlinaudio.event

import com.doublesymmetry.kotlinaudio.models.NotificationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NotificationEventHolder {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var _notificationStateChange = MutableSharedFlow<NotificationState>(1)
    var notificationStateChange = _notificationStateChange.asSharedFlow()

    internal fun updateNotificationState(state: NotificationState) {
        coroutineScope.launch {
            _notificationStateChange.emit(state)
        }
    }
}