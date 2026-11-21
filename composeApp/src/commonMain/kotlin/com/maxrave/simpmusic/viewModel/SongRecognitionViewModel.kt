package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.collectLatestResource
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.expect.PlatformSongRecognizer
import com.maxrave.simpmusic.recognition.Recognition
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_to_playlist
import simpmusic.composeapp.generated.resources.added_to_youtube_playlist
import simpmusic.composeapp.generated.resources.error

internal data class SongRecognitionUiState(
    val recognition: Recognition = Recognition.Listening,
    val amplitude: Float = 0f,
    val lowAudio: Boolean = false,
    val matchedTrack: SongsResult? = null,
    val isResolvingTrack: Boolean = false,
    val isLiked: Boolean = false,
    val listLocalPlaylist: List<LocalPlaylistEntity> = emptyList(),
    val listYouTubePlaylist: List<PlaylistsResult> = emptyList(),
)

internal class SongRecognitionViewModel(
    private val searchRepository: SearchRepository,
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(SongRecognitionUiState())
    val uiState: StateFlow<SongRecognitionUiState> = _uiState.asStateFlow()

    private var recognitionJob: Job? = null
    private var resolveJob: Job? = null
    private var likeObserverJob: Job? = null
    private var playlistsJob: Job? = null
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
        likeObserverJob?.cancel()
        likeObserverJob = null
        playlistsJob?.cancel()
        playlistsJob = null
        _uiState.value = SongRecognitionUiState()
    }

    private fun startRecognition() {
        sessionActive = true
        recognitionJob?.cancel()
        resolveJob?.cancel()
        likeObserverJob?.cancel()
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
                if (result is Recognition.Found) {
                    val resolveDeferred =
                        viewModelScope.async {
                            val query = "${result.title} ${result.artist}".trim()
                            if (query.isEmpty()) return@async null
                            val res = searchRepository.getSearchDataSong(query).firstOrNull()
                            when (res) {
                                is Resource.Success -> pickBestSongMatch(result, res.data.orEmpty())
                                else -> null
                            }
                        }
                    resolveJob = resolveDeferred
                    val best = withTimeoutOrNull(5_000L) { resolveDeferred.await() }
                    val stillResolving = best == null && resolveDeferred.isActive
                    _uiState.update {
                        it.copy(
                            recognition = result,
                            amplitude = 0f,
                            lowAudio = false,
                            matchedTrack = best,
                            isResolvingTrack = stillResolving,
                            isLiked = false,
                        )
                    }
                    if (best != null) {
                        observeLikedState(best.videoId)
                    } else if (stillResolving) {
                        viewModelScope.launch {
                            val lateBest = runCatching { resolveDeferred.await() }.getOrNull()
                            _uiState.update { state ->
                                if (state.recognition == result) {
                                    state.copy(matchedTrack = lateBest, isResolvingTrack = false)
                                } else {
                                    state
                                }
                            }
                            if (lateBest != null && _uiState.value.recognition == result) {
                                observeLikedState(lateBest.videoId)
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            recognition = result,
                            amplitude = 0f,
                            lowAudio = false,
                        )
                    }
                }
            }
    }

    private fun observeLikedState(videoId: String) {
        likeObserverJob?.cancel()
        if (videoId.isEmpty()) return
        likeObserverJob =
            viewModelScope.launch {
                songRepository.getSongAsFlow(videoId).collectLatest { song ->
                    _uiState.update { it.copy(isLiked = song?.liked == true) }
                }
            }
    }

    fun playTrack(song: SongsResult) {
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
    }

    fun toggleLike() {
        val song = _uiState.value.matchedTrack ?: return
        val nextLikeStatus = if (_uiState.value.isLiked) 0 else 1
        _uiState.update { it.copy(isLiked = nextLikeStatus == 1) }
        viewModelScope.launch {
            val existing = songRepository.getSongById(song.videoId).lastOrNull()
            if (existing == null) {
                songRepository.insertSong(song.toTrack().toSongEntity()).singleOrNull()
            }
            songRepository.updateLikeStatus(song.videoId, nextLikeStatus)
        }
    }

    fun loadPlaylistsForSave() {
        playlistsJob?.cancel()
        playlistsJob =
            viewModelScope.launch {
                launch {
                    localPlaylistRepository.getAllLocalPlaylists().collectLatest { list ->
                        _uiState.update { it.copy(listLocalPlaylist = list) }
                    }
                }
                launch {
                    playlistRepository.getLibraryPlaylist().collect { data ->
                        _uiState.update { state ->
                            state.copy(
                                listYouTubePlaylist =
                                    data?.filter { it.browseId != "VLLM" } ?: emptyList(),
                            )
                        }
                    }
                }
            }
    }

    fun addToLocalPlaylist(playlistId: Long) {
        val song = _uiState.value.matchedTrack ?: return
        viewModelScope.launch {
            val target = _uiState.value.listLocalPlaylist.find { it.id == playlistId } ?: return@launch
            if ((target.tracks ?: emptyList()).contains(song.videoId)) return@launch
            val entity =
                songRepository.getSongById(song.videoId).lastOrNull()
                    ?: song.toTrack().toSongEntity().also {
                        songRepository.insertSong(it).singleOrNull()
                    }
            localPlaylistRepository
                .addTrackToLocalPlaylist(
                    id = playlistId,
                    song = entity,
                    successMessage = getString(Res.string.added_to_playlist),
                    updatedYtMessage = getString(Res.string.added_to_youtube_playlist),
                    errorMessage = getString(Res.string.error),
                ).collectLatestResource(
                    onSuccess = { makeToast(it ?: getString(Res.string.added_to_playlist)) },
                    onError = { makeToast(it) },
                )
        }
    }

    fun addToYouTubePlaylist(browseId: String) {
        val song = _uiState.value.matchedTrack ?: return
        viewModelScope.launch {
            localPlaylistRepository
                .addYouTubePlaylistItem(
                    youtubePlaylistId = browseId,
                    videoId = song.videoId,
                ).collectLatestResource(
                    onSuccess = { makeToast(it) },
                    onError = { makeToast(it) },
                )
        }
    }

    override fun onCleared() {
        recognitionJob?.cancel()
        resolveJob?.cancel()
        likeObserverJob?.cancel()
        playlistsJob?.cancel()
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
