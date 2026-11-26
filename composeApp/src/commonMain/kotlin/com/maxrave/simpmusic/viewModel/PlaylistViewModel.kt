@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.maxrave.simpmusic.viewModel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADED
import com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADING
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.Author
import com.maxrave.domain.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.domain.data.model.browse.playlist.PlaylistExtras
import com.maxrave.domain.data.model.browse.playlist.PlaylistState
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_to_playlist
import simpmusic.composeapp.generated.resources.auto_created_by_youtube_music
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.playlist_is_empty
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.radio_not_available
import simpmusic.composeapp.generated.resources.shuffle
import simpmusic.composeapp.generated.resources.shuffle_not_available
import simpmusic.composeapp.generated.resources.suggest
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

    private val _suggestedTracks = MutableStateFlow<List<Track>>(emptyList())
    val suggestedTracks: StateFlow<List<Track>> = _suggestedTracks.asStateFlow()

    private val _suggestionsReloadParams = MutableStateFlow<String?>(null)
    val suggestionsReloadParams: StateFlow<String?> = _suggestionsReloadParams.asStateFlow()

    private val _suggestionsLoading = MutableStateFlow(false)
    val suggestionsLoading: StateFlow<Boolean> = _suggestionsLoading.asStateFlow()

    private val _relatedPlaylists = MutableStateFlow<List<PlaylistsResult>>(emptyList())
    val relatedPlaylists: StateFlow<List<PlaylistsResult>> = _relatedPlaylists.asStateFlow()

    private val _addingSuggestionIds = MutableStateFlow<Set<String>>(emptySet())
    val addingSuggestionIds: StateFlow<Set<String>> = _addingSuggestionIds.asStateFlow()

    private val _localSongsById = MutableStateFlow<Map<String, SongEntity>>(emptyMap())
    val localSongsById: StateFlow<Map<String, SongEntity>> = _localSongsById.asStateFlow()

    private var collectDownloadedJob: Job? = null
    private var _downloadedList = MutableStateFlow<List<String>>(emptyList())
    val downloadedList: StateFlow<List<String>> = _downloadedList

    private var playlistEntityJob: Job? = null
    private var newUpdateJob: Job? = null
    private var checkDownloadedPlaylist: Job? = null
    private var loadDataJob: Job? = null
    private var continuationBgJob: Job? = null
    private var extrasJob: Job? = null
    private var localSongsLookupJob: Job? = null

    private companion object {
        const val MAX_BG_CONTINUATION_PAGES = 2
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
                                                tracks.forEachIndexed { _, track ->
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
        extrasJob?.cancel()
        extrasJob = null
        localSongsLookupJob?.cancel()
        localSongsLookupJob = null
        playlistEntityJob?.cancel()
        playlistEntityJob = null
        checkDownloadedPlaylist?.cancel()
        checkDownloadedPlaylist = null
        _uiState.value = Loading
        _tracks.value = emptyList()
        _continuation.value = null
        _tracksListState.value = ListState.IDLE
        _suggestedTracks.value = emptyList()
        _suggestionsReloadParams.value = null
        _suggestionsLoading.value = false
        _relatedPlaylists.value = emptyList()
        _addingSuggestionIds.value = emptySet()
        _localSongsById.value = emptyMap()
        _playlistEntity.value = null
        _downloadedList.value = emptyList()
        _listColors.value = emptyList()
    }

    private fun refreshLocalSongsIncremental(tracksToCheck: List<Track>) {
        if (tracksToCheck.isEmpty()) return
        val existing = _localSongsById.value
        val missingIds =
            tracksToCheck
                .asSequence()
                .map { it.videoId }
                .filter { it.isNotBlank() && it !in existing }
                .distinct()
                .toList()
        if (missingIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val fetched = songRepository.getSongsByListVideoId(missingIds).firstOrNull().orEmpty()
            if (fetched.isEmpty()) return@launch
            _localSongsById.update { current ->
                val next = HashMap<String, SongEntity>(current.size + fetched.size)
                next.putAll(current)
                for (song in fetched) {
                    next[song.videoId] = song
                }
                next
            }
        }
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
                                    Logger.d(tag, "Radio data: ${data.first.tracks.size} tracks")
                                    _tracks.value = data.first.tracks
                                    _continuation.value = data.second
                                    _tracksListState.value =
                                        if (data.second.isNullOrEmpty()) ListState.PAGINATION_EXHAUST else ListState.IDLE
                                    refreshLocalSongsIncremental(data.first.tracks)
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
                                    log("Playlist endpoint: ${data.first.shuffleEndpoint}")
                                    val initialTracks = data.first.tracks
                                    val nextCont = data.second
                                    val resolvedCount = maxOf(data.first.trackCount, initialTracks.size)
                                    val existingEntity =
                                        withContext(Dispatchers.IO) {
                                            playlistRepository.getPlaylist(data.first.id).firstOrNull()
                                        }
                                    val entityToInsert =
                                        if (existingEntity == null) data.first.toPlaylistEntity() else null
                                    _playlistEntity.value = existingEntity ?: entityToInsert
                                    _tracks.value = initialTracks
                                    _continuation.value = nextCont
                                    _tracksListState.value =
                                        if (nextCont.isNullOrEmpty()) ListState.PAGINATION_EXHAUST else ListState.IDLE
                                    refreshLocalSongsIncremental(initialTracks)
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
                                    observeAndPersistPlaylistEntity(
                                        id = data.first.id,
                                        entityToInsert = entityToInsert,
                                        tracksToPersist = initialTracks,
                                    )
                                    if (!nextCont.isNullOrEmpty()) {
                                        preloadRemainingContinuationInBackground(data.first.id, nextCont)
                                    }
                                    loadPlaylistExtras(
                                        playlistId = data.first.id,
                                        extrasContinuation = data.first.extrasContinuation,
                                        initialExtras = data.first.initialExtras,
                                    )
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
        entityToInsert: PlaylistEntity?,
        tracksToPersist: List<Track>,
    ) {
        playlistEntityJob?.cancel()
        playlistEntityJob =
            viewModelScope.launch(Dispatchers.IO) {
                if (entityToInsert != null) {
                    playlistRepository.insertAndReplacePlaylist(entityToInsert)
                }
                playlistRepository.updatePlaylistInLibrary(
                    playlistId = id,
                    inLibrary = now(),
                )
                if (tracksToPersist.isNotEmpty()) {
                    launch {
                        songRepository.insertSongs(
                            tracksToPersist.map {
                                it.toSongEntity().copy(inLibrary = Config.REMOVED_SONG_DATE_TIME)
                            },
                        )
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
                // Yield initial mobile bandwidth so header hero artwork and extras complete cleanly first.
                delay(600L)
                val seenTokens = HashSet<String>()
                var nextCont: String? = initialContinuation
                var pagesLoaded = 0
                while (!nextCont.isNullOrEmpty() && pagesLoaded < MAX_BG_CONTINUATION_PAGES && seenTokens.add(nextCont)) {
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
                    refreshLocalSongsIncremental(moreTracks)
                    nextCont = res.second
                    _continuation.value = nextCont
                    pagesLoaded++
                    launch(Dispatchers.IO) {
                        songRepository.insertSongs(
                            moreTracks.map {
                                it.toSongEntity().copy(inLibrary = Config.REMOVED_SONG_DATE_TIME)
                            },
                        )
                    }
                    if (!nextCont.isNullOrEmpty() && pagesLoaded < MAX_BG_CONTINUATION_PAGES) {
                        delay(250L)
                    }
                }
                if (nextCont.isNullOrEmpty()) {
                    _continuation.value = null
                    _tracksListState.value = ListState.PAGINATION_EXHAUST
                } else {
                    _continuation.value = nextCont
                    _tracksListState.value = ListState.IDLE
                }
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
                        if (addedTracks.isNotEmpty()) {
                            _tracks.update { existing -> existing + addedTracks }
                            refreshLocalSongsIncremental(addedTracks)
                            launch(Dispatchers.IO) {
                                songRepository.insertSongs(
                                    addedTracks.map {
                                        it.toSongEntity().copy(inLibrary = Config.REMOVED_SONG_DATE_TIME)
                                    },
                                )
                            }
                        }
                        if (res.second.isNullOrEmpty()) {
                            _continuation.value = null
                            _tracksListState.value = ListState.PAGINATION_EXHAUST
                        } else {
                            _continuation.value = res.second
                            _tracksListState.value = ListState.IDLE
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
                    val cachedSongsMap: Map<String, SongEntity>
                    val cachedSongs =
                        withContext(Dispatchers.IO) {
                            playlistEntity.tracks?.let { trackIds ->
                                songRepository
                                    .getSongsByListVideoId(trackIds)
                                    .singleOrNull()
                            } ?: emptyList()
                        }
                    cachedSongsMap = cachedSongs.associateBy { it.videoId }
                    _playlistEntity.value = playlistEntity
                    _localSongsById.value = cachedSongsMap
                    _tracks.value = cachedSongs.map { it.toTrack() }
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
                                Logger.d(tag, "List song: ${listSong?.size ?: 0}")
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
                val index = loadedList.indexOfFirst { it.videoId == videoId }
                if (index < 0) return
                val clickedSong = loadedList[index]
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
        continuationBgJob?.cancel()
        viewModelScope.launch {
            if (tracksListState.value == ListState.PAGINATION_EXHAUST) {
                val currentTracks = tracks.value
                _playlistEntity.value
                    ?.copy(
                        tracks = currentTracks.toListVideoId(),
                        trackCount = currentTracks.size,
                    )?.let {
                        playlistRepository.insertAndReplacePlaylist(it)
                    }
                callback(currentTracks)
                return@launch
            }
            val id = uiState.value.data?.id ?: return@launch
            val seenTokens = HashSet<String>()
            while (tracksListState.value != ListState.PAGINATION_EXHAUST) {
                val cont = continuation.value
                if (cont.isNullOrEmpty() || !seenTokens.add(cont)) {
                    _continuation.value = null
                    _tracksListState.value = ListState.PAGINATION_EXHAUST
                    break
                }
                _tracksListState.value = ListState.PAGINATING
                val res =
                    songRepository
                        .getContinueTrack(
                            id,
                            cont,
                            fromPlaylist = true,
                        ).firstOrNull()
                val addedTracks = res?.first.orEmpty()
                if (addedTracks.isNotEmpty()) {
                    _tracks.update { existing -> existing + addedTracks }
                    refreshLocalSongsIncremental(addedTracks)
                    launch(Dispatchers.IO) {
                        songRepository.insertSongs(
                            addedTracks.map {
                                it.toSongEntity().copy(inLibrary = Config.REMOVED_SONG_DATE_TIME)
                            },
                        )
                    }
                }
                val nextCont = res?.second
                if (res == null || nextCont.isNullOrEmpty()) {
                    _continuation.value = null
                    _tracksListState.value = ListState.PAGINATION_EXHAUST
                    break
                } else {
                    _continuation.value = nextCont
                }
            }
            val finalTracks = tracks.value
            _playlistEntity.value
                ?.copy(
                    tracks = finalTracks.toListVideoId(),
                    trackCount = finalTracks.size,
                )?.let {
                    playlistRepository.insertAndReplacePlaylist(it)
                }
            callback(finalTracks)
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

    private fun loadPlaylistExtras(
        playlistId: String,
        extrasContinuation: String? = null,
        initialExtras: PlaylistExtras? = null,
    ) {
        val bareId = playlistId.removePrefix("VL")
        extrasJob?.cancel()
        extrasJob =
            viewModelScope.launch {
                if (extrasContinuation == null && initialExtras == null) {
                    delay(150L)
                }
                _suggestionsLoading.value = true
                try {
                    playlistRepository
                        .getPlaylistExtras(
                            playlistId = bareId,
                            initialContinuation = extrasContinuation,
                            initialExtras = initialExtras,
                        ).collectLatest { res ->
                            if (res is Resource.Success && res.data != null) {
                                val extras = res.data!!
                                if (bareId != "LM") {
                                    _suggestionsReloadParams.value = extras.reloadParams
                                    _suggestedTracks.value = extras.suggestions
                                }
                                _relatedPlaylists.value = extras.relatedPlaylists
                            }
                        }
                } finally {
                    _suggestionsLoading.value = false
                }
            }
    }

    fun reloadSuggestions() {
        val reloadParams = _suggestionsReloadParams.value ?: return
        if (_suggestionsLoading.value) return
        viewModelScope.launch {
            _suggestionsLoading.value = true
            try {
                playlistRepository.reloadPlaylistSuggestions(reloadParams).collectLatest { res ->
                    if (res is Resource.Success && res.data != null) {
                        val (nextParam, newTracks) = res.data!!
                        if (nextParam != null) _suggestionsReloadParams.value = nextParam
                        if (newTracks.isNotEmpty()) _suggestedTracks.value = newTracks
                    } else if (res is Resource.Error) {
                        makeToast(res.message ?: getString(Res.string.error))
                    }
                }
            } finally {
                _suggestionsLoading.value = false
            }
        }
    }

    suspend fun addTrackToPlaylist(track: Track): Boolean {
        val playlistId = uiState.value.data?.id?.removePrefix("VL") ?: return false
        var added = false
        playlistRepository.addTrackToYouTubePlaylist(playlistId, track.videoId).collectLatest { res ->
            when (res) {
                is Resource.Success -> added = true
                is Resource.Error -> makeToast(res.message ?: getString(Res.string.error))
            }
        }
        if (added) {
            _suggestedTracks.update { list -> list.filterNot { it.videoId == track.videoId } }
            _tracks.update { list -> if (list.any { it.videoId == track.videoId }) list else list + track }
            _uiState.update { state ->
                val data = state.data ?: return@update state
                Success(data.copy(trackCount = maxOf(data.trackCount + 1, _tracks.value.size)))
            }
            launchSongPersist(track)
            refreshLocalSongsIncremental(listOf(track))
            makeToast(getString(Res.string.added_to_playlist))
        }
        return added
    }

    private fun launchSongPersist(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            songRepository
                .insertSong(
                    track.toSongEntity().copy(
                        inLibrary = Config.REMOVED_SONG_DATE_TIME,
                    ),
                ).firstOrNull()
        }
    }

    fun addSuggestedTrack(track: Track) {
        if (track.videoId in _addingSuggestionIds.value) return
        viewModelScope.launch {
            _addingSuggestionIds.update { it + track.videoId }
            try {
                addTrackToPlaylist(track)
            } finally {
                _addingSuggestionIds.update { it - track.videoId }
            }
        }
    }

    fun playSuggestedTrack(track: Track) {
        val data = uiState.value.data ?: return
        val list = _suggestedTracks.value.ifEmpty { listOf(track) }
        val index = list.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
        setQueueData(
            QueueData.Data(
                listTracks = ArrayList(list),
                firstPlayedTrack = track,
                playlistId = "RDAMVM${track.videoId}",
                playlistName = "${getString(Res.string.playlist)} \"${data.title}\" ${getString(Res.string.suggest)}",
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(
            track,
            Config.PLAYLIST_CLICK,
            index,
        )
    }

    override fun onCleared() {
        super.onCleared()
        collectDownloadedJob?.cancel()
        playlistEntityJob?.cancel()
        extrasJob?.cancel()
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