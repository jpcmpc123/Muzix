package com.maxrave.simpmusic.recognition

/** State shown by the song recognition sheet and returned by the platform recognizer. */
internal sealed interface Recognition {
    data object Listening : Recognition

    data class Identifying(
        val attempt: Int,
        val maxAttempts: Int,
    ) : Recognition

    data class Found(
        val title: String,
        val artist: String,
        val coverArt: String?,
        val album: String? = null,
        val year: String? = null,
        val isrc: String? = null,
        val youtubeVideoId: String? = null,
    ) : Recognition

    data object NoMatch : Recognition

    data object Failed : Recognition
}
