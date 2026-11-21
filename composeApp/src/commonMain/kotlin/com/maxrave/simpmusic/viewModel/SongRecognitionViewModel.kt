package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.expect.PlatformSongRecognizer
import com.maxrave.simpmusic.recognition.Recognition
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class SongRecognitionUiState(
    val recognition: Recognition = Recognition.Listening,
    val amplitude: Float = 0f,
    val lowAudio: Boolean = false,
    val matchedTrack: SongsResult? = null,
    val isResolvingTrack: Boolean = false,
)

internal class SongRecognitionViewModel(
    private val searchRepository: SearchRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(SongRecognitionUiState())
    val uiState: StateFlow<SongRecognitionUiState> = _uiState.asStateFlow()

    private var recognitionJob: Job? = null
    private var resolveJob: Job? = null
    private var sessionActive = false

    fun startIfNeeded() {
        if (sessionActive) return
        startRecognition()
    }

    fun retry() {
        startRecognition()
    }

    fun cancelListening() {
        recognitionJob?.cancel()
        recognitionJob = null
        _uiState.update { it.copy(amplitude = 0f, lowAudio = false) }
    }

    fun resetSession() {
        sessionActive = false
        recognitionJob?.cancel()
        recognitionJob = null
        resolveJob?.cancel()
        resolveJob = null
        _uiState.value = SongRecognitionUiState()
    }

    private fun startRecognition() {
        sessionActive = true
        recognitionJob?.cancel()
        resolveJob?.cancel()
        _uiState.value = SongRecognitionUiState(recognition = Recognition.Listening)

        recognitionJob =
            viewModelScope.launch {
                if (mediaPlayerHandler.controlState.value.isPlaying) {
                    runCatching { mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause) }
                }
                val result =
                    PlatformSongRecognizer.recognize(
                        onAmplitude = { amp ->
                            _uiState.update { state ->
                                if (state.recognition is Recognition.Listening || state.recognition is Recognition.Identifying) {
                                    state.copy(amplitude = amp)
                                } else {
                                    state
                                }
                            }
                        },
                        onLowAudio = { low ->
                            _uiState.update { it.copy(lowAudio = low) }
                        },
                        onState = { progress ->
                            _uiState.update { it.copy(recognition = progress) }
                        },
                    )
                _uiState.update {
                    it.copy(
                        recognition = result,
                        amplitude = 0f,
                        lowAudio = false,
                    )
                }
                if (result is Recognition.Found) {
                    resolveMatchedTrack(result)
                }
            }
    }

    private fun resolveMatchedTrack(found: Recognition.Found) {
        val query = "${found.title} ${found.artist}".trim()
        if (query.isEmpty()) return
        resolveJob?.cancel()
        _uiState.update { it.copy(isResolvingTrack = true, matchedTrack = null) }
        resolveJob =
            viewModelScope.launch {
                searchRepository.getSearchDataSong(query).collect { res ->
                    when (res) {
                        is Resource.Success -> {
                            val best = pickBestSongMatch(found, res.data.orEmpty())
                            _uiState.update { it.copy(matchedTrack = best, isResolvingTrack = false) }
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isResolvingTrack = false) }
                        }
                    }
                }
            }
    }

    fun playMatchedTrack(): Boolean {
        val song = _uiState.value.matchedTrack ?: return false
        val firstTrack: Track = song.toTrack()
        setQueueData(
            QueueData.Data(
                listTracks = arrayListOf(firstTrack),
                firstPlayedTrack = firstTrack,
                playlistId = "RDAMVM${firstTrack.videoId}",
                playlistName = "\"${firstTrack.title}\" Radio",
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(firstTrack, type = Config.SONG_CLICK)
        return true
    }

    override fun onCleared() {
        recognitionJob?.cancel()
        resolveJob?.cancel()
        super.onCleared()
    }

    private fun pickBestSongMatch(
        found: Recognition.Found,
        candidates: List<SongsResult>,
    ): SongsResult? {
        if (candidates.isEmpty()) return null
        val targetTitle = found.title.lowercase()
        val targetArtist = found.artist.lowercase()
        return candidates.maxByOrNull { candidate ->
            var score = 0
            val cTitle = candidate.title?.lowercase().orEmpty()
            val cArtists = candidate.artists?.joinToString(" ") { it.name.lowercase() }.orEmpty()
            if (cTitle == targetTitle) {
                score += 4
            } else if (cTitle.contains(targetTitle) || targetTitle.contains(cTitle)) {
                score += 2
            }
            if (targetArtist.isNotEmpty() &&
                (cArtists.contains(targetArtist) || targetArtist.split(" ", "&", ",").any { part -> part.length > 2 && cArtists.contains(part) })
            ) {
                score += 3
            }
            score
        } ?: candidates.first()
    }
}
