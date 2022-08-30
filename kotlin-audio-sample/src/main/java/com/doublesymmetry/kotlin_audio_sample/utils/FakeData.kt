package com.doublesymmetry.kotlin_audio_sample.utils

import com.doublesymmetry.kotlinaudio.models.DefaultAudioItem
import com.doublesymmetry.kotlinaudio.models.MediaType


val firstItem = DefaultAudioItem(
    "https://storage.jamendo.com/download/track/1960851/mp35", MediaType.DEFAULT,
    title = "Dirty Computer",
    artwork = "https://upload.wikimedia.org/wikipedia/en/0/0b/DirtyComputer.png",
    artist = "Janelle Monáe"
)

val secondItem = DefaultAudioItem(
    "https://storage.jamendo.com/download/track/1960171/mp35/", MediaType.DEFAULT,
    title = "Melodrama",
    artwork = "https://images-na.ssl-images-amazon.com/images/I/A18QUHExFgL._SL1500_.jpg",
    artist = "Lorde"
)