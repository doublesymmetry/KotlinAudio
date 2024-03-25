package com.doublesymmetry.kotlinaudio.models

import android.os.Bundle

interface AAMediaSessionCallBack {
    fun handlePlayFromMediaId(mediaId: String?, extras: Bundle?)
    fun handlePlayFromSearch(query: String?, extras: Bundle?)
    fun handleSkipToQueueItem(id: Long)
}