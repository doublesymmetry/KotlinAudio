package com.doublesymmetry.kotlinaudio.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun <T> Flow<T>.throttle(ms: Long): Flow<T> = channelFlow {
    var isThrottling = false
    var lastValue: T? = null

    collect { value ->
        if (!isThrottling) {
            isThrottling = true
            launch {
                send(value)
                delay(ms)
                lastValue?.let { send(it) }
                isThrottling = false
                lastValue = null
            }
        } else {
            lastValue = value
        }
    }
}
