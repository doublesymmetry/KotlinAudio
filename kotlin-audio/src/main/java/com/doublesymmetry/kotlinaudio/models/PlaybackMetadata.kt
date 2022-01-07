package com.doublesymmetry.kotlinaudio.models
data class PlaybackMetadata(val source: String) {
    var title: String? = null;
    var url: String? = null;
    var artist: String? = null;
    var album: String? = null;
    var date: String? = null;
    var genre: String? = null;
}
