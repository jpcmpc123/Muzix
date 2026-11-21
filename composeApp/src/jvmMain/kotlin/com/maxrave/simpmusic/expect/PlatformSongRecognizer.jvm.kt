package com.maxrave.simpmusic.expect

import com.maxrave.simpmusic.recognition.Recognition

internal actual object PlatformSongRecognizer {
    actual suspend fun recognize(
        onAmplitude: (Float) -> Unit,
        onLowAudio: (Boolean) -> Unit,
        onState: (Recognition) -> Unit,
    ): Recognition = Recognition.Failed
}
