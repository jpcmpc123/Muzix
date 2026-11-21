package com.maxrave.simpmusic.expect

import com.maxrave.simpmusic.recognition.Recognition
import com.maxrave.simpmusic.recognition.SongRecognizer

internal actual object PlatformSongRecognizer {
    actual suspend fun recognize(
        onAmplitude: (Float) -> Unit,
        onLowAudio: (Boolean) -> Unit,
        onState: (Recognition) -> Unit,
    ): Recognition =
        SongRecognizer.recognize(
            onAmplitude = onAmplitude,
            onLowAudio = onLowAudio,
            onState = onState,
        )
}
