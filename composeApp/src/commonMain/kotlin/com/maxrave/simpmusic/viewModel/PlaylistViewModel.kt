@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.maxrave.simpmusic.viewModel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADED
import com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADING
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.Author
import com.maxrave.domain.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.domain.data.model.browse.playlist.PlaylistState
import com.maxrave.domain.extension.now
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.collectLatestResource
import com.maxrave.domain.utils.isRadioPlaylistId
import com.maxrave.domain.utils.toListVideoId
import com.maxrave.domain.utils.toPlaylistEntity
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.PlaylistUIState.Error
import com.maxrave.simpmusic.viewModel.PlaylistUIState.Loading
import com.maxrave.simpmusic.viewModel.PlaylistUIState.Success
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.auto_created_by_youtube_music
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.playlist_is_empty
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.radio_not_available
import simpmusic.composeapp.generated.resources.shuffle
import simpmusic.composeapp.generated.resources.shuffle_not_available
import simpmusic.composeapp.generated.resources.synced
import simpmusic.composeapp.generated.resources.syncing
import simpmusic.composeapp.generated.resources.view_count

class PlaylistViewModel(
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
) : BaseViewModel() {
    val downloadUtils: DownloadHandler by inject<DownloadHandler>()
    private var _uiState = MutableStateFlow<PlaylistUIState>(Loading)
    val uiState: StateFlow<PlaylistUIState> = _uiState

    private var _listColors = MutableStateFlow<List<Color>>(emptyList())
    val listColors: StateFlow<List<Color>> = _listColors

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private var _playlistEntity: MutableStateFlow<PlaylistEntity?> = MutableStateFlow(null)
    var playlistEntity: StateFlow<PlaylistEntity?> = _playlistEntity

    val downloadState = _playlistEntity.map { it?.downloadState ?: 0 }.stateIn(viewModelScope, WhileSubscribed(1000), 0)
    val liked = _playlistEntity.map { it?.liked == true }.stateIn(viewModelScope, WhileSubscribed(1000), false)

    private var _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks

    private var _tracksListState = MutableStateFlow<ListState>(ListState.IDLE)
    val tracksListState: StateFlow<ListState> = _tracksListState

    private var collectDownloadedJob: Job? = null
    private var _downloadedList = MutableStateFlow<List<String>>(emptyList())
    val downloadedList: StateFlow<List<String>> = _downloadedList

    private var playlistEntityJob: Job? = null
    private var newUpdateJob: Job? = null
    private var checkDownloadedPlaylist: Job? = null
    private var loadDataJob: Job? = null
    private var continuationBgJob: Job? = null

    private companion object {
        const val MAX_PRELOAD_CONTINUATION_PAGES = 15
        const val PLAYLIST_PRELOAD_TIMEOUT_MS = 8_000L
    }

    init {
        viewModelScope.launch {
            val listTrackStringJob =
                launch(Dispatchers.IO) {
                    downloadState
                        .collectLatest { state ->
                            newUpdateJob?.cancel()
                            val id = playlistEntity.value?.id ?: return@collectLatest
                            if (state == STATE_DOWNLOADING || state == STATE_DOWNLOADED) {
                                getFullTracks { tracks ->
                                    newUpdateJob =
                                        launch {
                                            val listSongs = songRepository.getSongsByListVideoId(tracks.toListVideoId()).firstOrNull() ?: emptyList()
                                            if (state == STATE_DOWNLOADED && listSongs.isNotEmpty()) {
                                                listSongs.filter { it.downloadState != STATE_DOWNLOADED }.let { notDownloaded ->
                                                    if (notDownloaded.isNotEmpty()) {
                                                        downloadTracks(notDownloaded.map { it.videoId })
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADING)
                                                    } else {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                                                    }
                                                }
                                            }
                                            downloadUtils.downloads.collectLatest { downloads ->
                                                var count = 0
                                                tracks.forEachIndexed { index, track ->
                                                    val trackDownloadState = downloads[track.videoId]?.first?.state
                                                    val videoDownloadState =
                                                        downloads[track.videoId]?.second?.state ?: DownloadHandler.State.STATE_COMPLETED
                                                    if (trackDownloadState == DownloadHandler.State.STATE_DOWNLOADING ||
                                                        videoDownloadState == DownloadHandler.State.STATE_DOWNLOADING
                                                    ) {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADING)
                                                    } else if (trackDownloadState == DownloadHandler.State.STATE_COMPLETED &&
                                                        videoDownloadState == DownloadHandler.State.STATE_COMPLETED
                                                    ) {
                                                        count++
                                                    }
                                                    if (count == tracks.size) {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        }
                }
            listTrackStringJob.join()
        }
    }

    private fun updatePlaylistDownloadState(
        id: String,
        state: Int,
    ) {
        viewModelScope.launch {
            playlistRepository.updatePlaylistDownloadState(id, state)
            delay(500)
            _playlistEntity.update {
                it?.copy(downloadState = state)
            }
        }
    }

    private fun downloadTracks(listJob: List<String>) {
        viewModelScope.launch {
            listJob.forEach { videoId ->
                songRepository.getSongById(videoId).singleOrNull()?.let { song ->
                    if (song.downloadState != STATE_DOWNLOADED) {
                        downloadUtils.downloadTrack(videoId, song.title, song.thumbnails ?: "")
                    }
                }
            }
        }
    }

    private fun resetData() {
        loadDataJob?.cancel()
        loadDataJob = null
        continuationBgJob?.cancel()
        continuationBgJob = null
        playlistEntityJob?.cancel()
        playlistEntityJob = null
        checkDownloadedPlaylist?.cancel()
        checkDownloadedPlaylist = null
        _uiState.value = Loading
        _tracks.value = emptyList()
        _continuation.value = null
        _tracksListState.value = ListState.IDLE
        _playlistEntity.value = null
        _downloadedList.value = emptyList()
        _listColors.value = emptyList()
    }

    fun getData(id: String) {
        resetData()
        loadDataJob =
            viewModelScope.launch {
                // Check radio
                if (id.isRadioPlaylistId()) {
                    playlistRepository
                        .getRadio(
                            id,
                            radioString = getString(Res.string.radio),
                            defaultDescription = getString(Res.string.auto_created_by_youtube_music),
                            viewString = getString(Res.string.view_count),
                        ).collect { res ->
                            val data = res.data
                            when (res) {
                                is Resource.Success if (data != null) -> {
                                    Logger.d(tag, "Radio data: $data")
                                    _tracks.value = data.first.tracks
                                    _continuation.value = data.second
                                    _tracksListState.value =
                                        if (data.second.isNullOrEmpty()) ListState.PAGINATION_EXHAUST else ListState.IDLE
                                    _uiState.value =
                                        Success(
                                            data =
                                                PlaylistState(
                                                    id = data.first.id,
                                                    title = data.first.title,
                                                    isRadio = true,
                                                    author = data.first.author,
                                                    thumbnail =
                                                        data.first.thumbnails
                                                            .lastOrNull()
                                                            ?.url,
                                                    description = data.first.description,
                                                    trackCount = data.first.trackCount,
                                                    year = data.first.year,
                                                ),
                                        )
                                    launch(Dispatchers.IO) {
                                        playlistRepository.insertRadioPlaylist(data.first.toPlaylistEntity())
                                    }
                                }

                                else -> {
                                    _uiState.value = Error(res.message ?: "Empty response")
                                }
                            }
                        }
                } else {
                    // This is an online playlist
                    playlistRepository
                        .getPlaylistData(id, getString(Res.string.view_count))
                        .collect { res ->
                            val data = res.data
                            when (res) {
                                is Resource.Success if (data != null) -> {
                                    Logger.d(tag, "Playlist data: $data")
                                    log("Playlist endpoint: ${data.first.shuffleEndpoint}")
                                    val allTracks = data.first.tracks.toMutableList()
                                    var nextCont = data.second
                                    if (!nextCont.isNullOrEmpty()) {
                                        withTimeoutOrNull(PLAYLIST_PRELOAD_TIMEOUT_MS) {
                                            var pagesLoaded = 0
                                            while (!nextCont.isNullOrEmpty() && pagesLoaded < MAX_PRELOAD_CONTINUATION_PAGES) {
                                                val contRes =
                                                    songRepository
                                                        .getContinueTrack(
                                                            data.first.id,
                                                            nextCont!!,
                                                            fromPlaylist = true,
                                                        ).firstOrNull() ?: break
                                                val moreTracks = contRes.first
                                                if (moreTracks.isNullOrEmpty()) {
                                                    nextCont = null
                                                    break
                                                }
                                                allTracks.addAll(moreTracks)
                                                nextCont = contRes.second
                                                pagesLoaded++
                                            }
                                        }
                                    }
                                    val resolvedCount = maxOf(data.first.trackCount, allTracks.size)
                                    val fullBrowse =
                                        data.first.copy(
                                            tracks = allTracks,
                                            trackCount = resolvedCount,
                                        )
                                    val existingEntity =
                                        withContext(Dispatchers.IO) {
                                            playlistRepository.getPlaylist(data.first.id).firstOrNull()
                                        }
                                    if (existingEntity == null) {
                                        val createdEntity = fullBrowse.toPlaylistEntity()
                                        withContext(Dispatchers.IO) {
                                            playlistRepository.insertAndReplacePlaylist(createdEntity)
                                        }
                                        _playlistEntity.value = createdEntity
                                    } else {
                                        _playlistEntity.value = existingEntity
                                    }
                                    _tracks.value = allTracks
                                    _continuation.value = nextCont
                                    _tracksListState.value =
                                        if (nextCont.isNullOrEmpty()) ListState.PAGINATION_EXHAUST else ListState.IDLE
                                    _uiState.value =
                                        Success(
                                            data =
                                                PlaylistState(
                                                    id = data.first.id,
                                                    title = data.first.title,
                                                    isRadio = false,
                                                    author = data.first.author,
                                                    thumbnail =
                                                        data.first.thumbnails
                                                            .lastOrNull()
                                                            ?.url,
                                                    description = data.first.description,
                                                    trackCount = resolvedCount,
                                                    year = data.first.year,
                                                    shuffleEndpoint = data.first.shuffleEndpoint,
                                                    radioEndpoint = data.first.radioEndpoint,
                                                ),
                                        )
                                    observeAndPersistPlaylistEntity(id = data.first.id, tracksToPersist = allTracks)
                                    if (!nextCont.isNullOrEmpty()) {
                                        preloadRemainingContinuationInBackground(data.first.id, nextCont!!)
                                    }
                                }

                                else -> {
                                    getPlaylistEntity(id)
                                }
                            }
                        }
                }
            }
    }

    private fun observeAndPersistPlaylistEntity(
        id: String,
        tracksToPersist: List<Track>,
    ) {
        playlistEntityJob?.cancel()
        playlistEntityJob =
            viewModelScope.launch(Dispatchers.IO) {
                playlistRepository.updatePlaylistInLibrary(
                    playlistId = id,
                    inLibrary = now(),
                )
                launch {
                    tracksToPersist.forEach { track ->
                        songRepository
                            .insertSong(
                                track.toSongEntity().copy(
                                    inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                ),
                            ).firstOrNull()
                    }
                }
                playlistRepository.getPlaylist(id).collectLatest { playlist ->
                    if (playlist != null) {
                        _playlistEntity.value = playlist
                    }
                }
            }
    }

    private fun preloadRemainingContinuationInBackground(
        playlistId: String,
        initialContinuation: String,
    ) {
        continuationBgJob?.cancel()
        continuationBgJob =
            viewModelScope.launch {
                var nextCont: String? = initialContinuation
                while (!nextCont.isNullOrEmpty()) {
                    _tracksListState.value = ListState.PAGINATING
                    val res =
                        songRepository
                            .getContinueTrack(
                                playlistId,
                                nextCont,
                                fromPlaylist = true,
                            ).firstOrNull() ?: break
                    val moreTracks = res.first
                    if (moreTracks.isNullOrEmpty()) {
                        nextCont = null
                        break
                    }
                    _tracks.update { existing -> existing + moreTracks }
                    nextCont = res.second
                    _continuation.value = nextCont
                    launch(Dispatchers.IO) {
                        moreTracks.forEach { track ->
                            songRepository
                                .insertSong(
                                    track.toSongEntity().copy(
                                        inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                    ),
                                ).firstOrNull()
                        }
                    }
                }
                _continuation.value = null
                _tracksListState.value = ListState.PAGINATION_EXHAUST
            }
    }

    fun getContinuationTrack(
        playlistId: String,
        continuation: String?,
    ) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _tracksListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                _tracksListState.value = ListState.PAGINATING
                songRepository
                    .getContinueTrack(
                        playlistId,
                        continuation,
                        fromPlaylist = true,
                    ).collectLatest { res ->
                        val addedTracks = res.first ?: emptyList()
                        _tracks.update {
                            val newList = it.toMutableList()
                            newList.addAll(addedTracks)
                            newList
                        }
                        if (res.second.isNullOrEmpty()) {
                            _continuation.value = null
                            _tracksListState.value = ListState.PAGINATION_EXHAUST
                        } else {
                            _continuation.value = res.second
                            _tracksListState.value = ListState.IDLE
                        }
                        if (addedTracks.isNotEmpty()) {
                            launch(Dispatchers.IO) {
                                addedTracks.forEach { track ->
                                    songRepository
                                        .insertSong(
                                            track
                                                .toSongEntity()
                                                .copy(
                                                    inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                                ),
                                        ).singleOrNull()
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun getPlaylistEntity(
        id: String,
        playlistBrowse: PlaylistBrowse? = null,
    ) {
        playlistEntityJob?.cancel()
        playlistEntityJob =
            viewModelScope.launch {
                val playlistEntity =
                    withContext(Dispatchers.IO) {
                        playlistRepository.getPlaylist(id).firstOrNull()
                    }
                if (playlistEntity != null) {
                    val cachedSongs =
                        withContext(Dispatchers.IO) {
                            playlistEntity.tracks?.let { trackIds ->
                                songRepository
                                    .getSongsByListVideoId(trackIds)
                                    .singleOrNull()
                                    ?.map { it.toTrack() }
                            } ?: emptyList()
                        }
                    _playlistEntity.value = playlistEntity
                    _tracks.value = cachedSongs
                    _continuation.value = null
                    _tracksListState.value = ListState.PAGINATION_EXHAUST
                    _uiState.value =
                        Success(
                            data =
                                PlaylistState(
                                    id = playlistEntity.id,
                                    title = playlistEntity.title,
                                    isRadio = false,
                                    author =
                                        Author(
                                            id = "",
                                            name = playlistEntity.author ?: "",
                                        ),
                                    thumbnail = playlistEntity.thumbnails,
                                    description = playlistEntity.description,
                                    trackCount = playlistEntity.trackCount,
                                    year = playlistEntity.year ?: now().year.toString(),
                                ),
                        )
                    launch(Dispatchers.IO) {
                        playlistRepository.updatePlaylistInLibrary(
                            playlistId = id,
                            inLibrary = now(),
                        )
                    }
                    if (playlistEntity.downloadState != STATE_DOWNLOADED) {
                        checkDownloadedPlaylist =
                            launch(Dispatchers.IO) {
                                val listSong =
                                    songRepository
                                        .getSongsByListVideoId(
                                            playlistEntity.tracks ?: emptyList(),
                                        ).firstOrNull()
                                Logger.d(tag, "List song: $listSong")
                                if (!listSong.isNullOrEmpty() && listSong.size == playlistEntity.tracks?.size &&
                                    listSong.all {
                                        it.downloadState == STATE_DOWNLOADED
                                    }
                                ) {
                                    updatePlaylistDownloadState(playlistEntity.id, STATE_DOWNLOADED)
                                }
                            }
                    }
                } else {
                    _uiState.value = Error("Empty response")
                }
            }
    }

    fun setBrush(listColors: List<Color>) {
        _listColors.value = listColors
    }

    fun updatePlaylistLiked(
        liked: Boolean,
        id: String,
    ) {
        viewModelScope.launch {
            val tempLiked = if (liked) 1 else 0
            playlistRepository.updatePlaylistLiked(id, tempLiked)
            _playlistEntity.update {
                it?.copy(
                    liked = tempLiked == 1,
                )
            }
            getFullTracks { }
        }
    }

    fun onUIEvent(event: PlaylistUIEvent) {
        val data = uiState.value.data ?: return
        when (event) {
            is PlaylistUIEvent.ItemClick -> {
                val videoId = event.videoId
                val loadedList = tracks.value
                val clickedSong = loadedList.firstOrNull { it.videoId == videoId } ?: return
                val index = loadedList.indexOf(clickedSong)
                setQueueData(
                    QueueData.Data(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        firstPlayedTrack = clickedSong,
                        playlistId = data.id,
                        playlistName = "${
                            getString(
                                Res.string.playlist,
                            )
                        } \"${data.title}\"",
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = continuation.value,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    index,
                )
            }

            PlaylistUIEvent.PlayAll -> {
                val loadedList = tracks.value
                if (loadedList.isEmpty()) {
                    makeToast(
                        getString(Res.string.playlist_is_empty),
                    )
                    return
                }
                val clickedSong = loadedList.first()
                setQueueData(
                    QueueData.Data(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        firstPlayedTrack = clickedSong,
                        playlistId = data.id,
                        playlistName = "${
                            getString(
                                Res.string.playlist,
                            )
                        } \"${data.title}\"",
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = continuation.value,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    0,
                )
            }

            PlaylistUIEvent.Shuffle -> {
                val shuffleEndpoint = data.shuffleEndpoint
                if (shuffleEndpoint == null) {
                    makeToast(
                        getString(Res.string.shuffle_not_available),
                    )
                    return
                } else {
                    viewModelScope.launch {
                        songRepository.getRadioFromEndpoint(shuffleEndpoint).collectLatest { res ->
                            val result = res.data
                            when (res) {
                                is Resource.Success if (result != null) -> {
                                    Logger.d(tag, "Shuffle data: ${result.first.size}")
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = result.first.toCollection(arrayListOf<Track>()),
                                            firstPlayedTrack = result.first.firstOrNull() ?: return@collectLatest,
                                            playlistId = shuffleEndpoint.playlistId,
                                            playlistName = "\"${data.title}\" ${getString(Res.string.shuffle)}",
                                            playlistType = PlaylistType.RADIO,
                                            continuation = result.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        result.first.firstOrNull() ?: return@collectLatest,
                                        Config.RADIO_CLICK,
                                        0,
                                    )
                                }

                                else -> {
                                    makeToast(
                                        res.message ?: getString(Res.string.error),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PlaylistUIEvent.StartRadio -> {
                val radioEndpoint = data.radioEndpoint
                if (radioEndpoint == null) {
                    makeToast(
                        getString(Res.string.radio_not_available),
                    )
                    return
                } else {
                    viewModelScope.launch {
                        songRepository.getRadioFromEndpoint(radioEndpoint).collectLatest { res ->
                            val result = res.data
                            when (res) {
                                is Resource.Success if (result != null) -> {
                                    Logger.d(tag, "Radio data: ${result.first.size}")
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = result.first.toCollection(arrayListOf<Track>()),
                                            firstPlayedTrack = result.first.firstOrNull() ?: return@collectLatest,
                                            playlistId = radioEndpoint.playlistId,
                                            playlistName = "\"${data.title}\" ${getString(Res.string.radio)}",
                                            playlistType = PlaylistType.RADIO,
                                            continuation = result.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        result.first.firstOrNull() ?: return@collectLatest,
                                        Config.RADIO_CLICK,
                                        0,
                                    )
                                }

                                else -> {
                                    makeToast(
                                        res.message ?: getString(Res.string.error),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PlaylistUIEvent.Download -> {
                downloadFullPlaylist()
            }

            PlaylistUIEvent.Favorite -> {
                updatePlaylistLiked(!liked.value, data.id)
            }
        }
    }

    fun getFullTracks(callback: (List<Track>) -> Unit) {
        viewModelScope.launch {
            if (tracksListState.value == ListState.PAGINATION_EXHAUST) {
                _playlistEntity.value
                    ?.copy(
                        tracks = tracks.value.toListVideoId(),
                        trackCount = tracks.value.size,
                    )?.let {
                        playlistRepository.insertAndReplacePlaylist(it)
                    }
                callback(tracks.value)
            } else {
                val id = uiState.value.data?.id ?: return@launch
                tracksListState.collectLatest { state ->
                    if (state == ListState.PAGINATION_EXHAUST) {
                        _playlistEntity.value
                            ?.copy(
                                tracks = tracks.value.toListVideoId(),
                                trackCount = tracks.value.size,
                            )?.let {
                                playlistRepository.insertAndReplacePlaylist(it)
                            }
                        callback(tracks.value)
                    } else if (state != ListState.PAGINATING) {
                        getContinuationTrack(id, continuation.value)
                    }
                }
            }
        }
    }

    fun saveToLocal(tracks: List<Track>) {
        viewModelScope.launch {
            val data = uiState.value.data ?: return@launch
            localPlaylistRepository
                .syncYouTubePlaylistToLocalPlaylist(
                    data,
                    tracks,
                    getString(Res.string.synced),
                    getString(Res.string.error),
                ).collectLatestResource(
                    onSuccess = {
                        makeToast(it)
                    },
                    onLoading = {
                        makeToast(getString(Res.string.syncing))
                    },
                    onError = {
                        makeToast(it)
                    },
                )
        }
    }

    fun downloadFullPlaylist() {
        viewModelScope.launch {
            val id = playlistEntity.value?.id ?: return@launch
            makeToast(getString(Res.string.downloading))
            updatePlaylistDownloadState(id, STATE_DOWNLOADING)
            getFullTracks { tracks ->
                tracks.forEach {
                    viewModelScope.launch {
                        downloadUtils.downloadTrack(it.videoId, it.title, it.thumbnails?.lastOrNull()?.url ?: "")
                    }
                }
            }
        }
    }

    fun updatePlaylistTitle(
        title: String,
        id: String,
    ) {
        viewModelScope.launch {
            playlistRepository
                .updateYourYouTubePlaylistTitle(
                    id,
                    title,
                ).collect {
                    when (it) {
                        is Resource.Success -> {
                            getData(id)
                            makeToast(it.data ?: getString(Res.string.synced))
                        }

                        is Resource.Error -> {
                            makeToast(it.message ?: getString(Res.string.error))
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectDownloadedJob?.cancel()
        playlistEntityJob?.cancel()
    }
}

sealed class PlaylistUIState(
    val data: PlaylistState? = null,
    val message: String? = null,
) {
    data object Loading : PlaylistUIState()

    class Success(
        data: PlaylistState,
    ) : PlaylistUIState(
            data = data,
        )

    class Error(
        message: String? = null,
    ) : PlaylistUIState(
            message = message,
        )
}

sealed class PlaylistUIEvent {
    data object PlayAll : PlaylistUIEvent()

    data object Shuffle : PlaylistUIEvent()

    data object StartRadio : PlaylistUIEvent()

    data class ItemClick(
        val videoId: String,
    ) : PlaylistUIEvent()

    data object Favorite : PlaylistUIEvent()

    data object Download : PlaylistUIEvent()
}

enum class ListState {
    IDLE,
    LOADING,
    PAGINATING,
    ERROR,
    PAGINATION_EXHAUST,
}