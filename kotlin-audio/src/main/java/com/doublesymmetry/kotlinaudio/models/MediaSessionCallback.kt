package com.doublesymmetry.kotlinaudio.models

import android.os.Bundle
import android.support.v4.media.RatingCompat


sealed class MediaSessionCallback {
    class PLAY(val extras: Bundle?): MediaSessionCallback()
    class PAUSE(val extras: Bundle?): MediaSessionCallback()
    class NEXT(val extras: Bundle?): MediaSessionCallback()
    class PREVIOUS(val extras: Bundle?): MediaSessionCallback()
    class FORWARD(val extras: Bundle?): MediaSessionCallback()
    class REWIND(val extras: Bundle?): MediaSessionCallback()
    class STOP(val extras: Bundle?): MediaSessionCallback()
    class SEEK(val position: Long, val extras: Bundle?): MediaSessionCallback()
    class RATING(val rating: RatingCompat, val extras: Bundle?): MediaSessionCallback()
}
