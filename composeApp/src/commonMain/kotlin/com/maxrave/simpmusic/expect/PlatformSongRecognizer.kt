package com.maxrave.simpmusic.expect

import com.maxrave.simpmusic.recognition.Recognition

internal expect object PlatformSongRecognizer {
    suspend fun recognize(
        onAmplitude: (Float) -> Unit,
        onLowAudio: (Boolean) -> Unit,
        onState: (Recognition) -> Unit,
    ): Recognition
}
