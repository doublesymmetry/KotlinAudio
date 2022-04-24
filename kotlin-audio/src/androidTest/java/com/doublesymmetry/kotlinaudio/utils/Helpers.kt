package com.doublesymmetry.kotlinaudio.utils

var isCIEnv = (System.getenv("CI") ?: "false") == "true"
