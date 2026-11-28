package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.playlist.DualPlaylistCreationResult
import com.maxrave.domain.data.model.library.LibraryCollectionPreview
import com.maxrave.domain.data.model.library.LibraryOverview
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.ChartItem
import com.maxrave.domain.data.type.MonthlyRecapItem
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.extension.now
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.isRadioPlaylistId
import com.maxrave.simpmusic.ui.screen.home.analytics.monthFullNameResource
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_local_playlist
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.playlist_created_local_sync_failed
import simpmusic.composeapp.generated.resources.playlist_created_synced
import simpmusic.composeapp.generated.resources.wrapped_recap_month
import simpmusic.composeapp.generated.resources.wrapped_recap_month_year

class LibraryViewModel(
    private val dataStoreManager: DataStoreManager,
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val commonRepository: CommonRepository,
    private val playlistRepository: PlaylistRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val podcastRepository: PodcastRepository,
    private val cacheRepository: CacheRepository,
) : BaseViewModel() {
    private val _currentScreen: MutableStateFlow<LibraryChipType> = MutableStateFlow(LibraryChipType.YOUR_LIBRARY)
    val currentScreen: StateFlow<LibraryChipType> get() = _currentScreen.asStateFlow()

    // A chip was picked (a tap, or a deep link) before the one saved last time finished loading.
    private var screenChosen = false
    private val _recentlyAdded: MutableStateFlow<LocalResource<List<RecentlyType>>> =
        MutableStateFlow(LocalResource.Loading())
    val recentlyAdded: StateFlow<LocalResource<List<RecentlyType>>> get() = _recentlyAdded.asStateFlow()

    private val _yourLocalPlaylist: MutableStateFlow<LocalResource<List<LocalPlaylistEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val yourLocalPlaylist: StateFlow<LocalResource<List<LocalPlaylistEntity>>> get() = _yourLocalPlaylist.asStateFlow()

    private val _youTubePlaylist: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubePlaylist: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubePlaylist.asStateFlow()

    private val _isRefreshingYouTubePlaylists: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshingYouTubePlaylists: StateFlow<Boolean> get() = _isRefreshingYouTubePlaylists.asStateFlow()

    private val _isCreatingPlaylist: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCreatingPlaylist: StateFlow<Boolean> get() = _isCreatingPlaylist.asStateFlow()

    private val _libraryLayoutMode: MutableStateFlow<LibraryLayoutMode> = MutableStateFlow(LibraryLayoutMode.LIST)

    /** Rows or a two-column grid for the YouTube playlists in Your library; remembered across launches. */
    val libraryLayoutMode: StateFlow<LibraryLayoutMode> get() = _libraryLayoutMode.asStateFlow()

    private var youTubePlaylistJob: Job? = null
    private var syncedYouTubeCookieHash: Int? = null
    private var lastAppliedCookieHash: Int? = null

    private val _youTubeMixForYou: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubeMixForYou: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubeMixForYou.asStateFlow()

    private val _favoritePlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePlaylist.asStateFlow()

    private val _favoritePodcasts: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePodcasts: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePodcasts.asStateFlow()

    private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedPlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _downloadedPlaylist.asStateFlow()

    private val _chartPlaylists: MutableStateFlow<LocalResource<List<ChartItem>>> =
        MutableStateFlow(LocalResource.Loading())
    val chartPlaylists: StateFlow<LocalResource<List<ChartItem>>> get() = _chartPlaylists.asStateFlow()

    private val _listCanvasSong: MutableStateFlow<LocalResource<List<SongEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val listCanvasSong: StateFlow<LocalResource<List<SongEntity>>> get() = _listCanvasSong.asStateFlow()

    /**
     * The months the Wrapped tab offers a recap for, newest first.
     *
     * A [MonthlyRecapItem] rather than the destination's own
     * [LibraryDynamicPlaylistType.MonthlyRecap]: the tab draws these through the shared
     * `GridLibraryPlaylist`, which renders only [PlaylistType]s, and a tile needs a title and a
     * cover on top of the year and month the destination carries. The destination is rebuilt from
     * the year and month when a tile is tapped.
     */
    private val _monthlyRecaps: MutableStateFlow<LocalResource<List<MonthlyRecapItem>>> =
        MutableStateFlow(LocalResource.Loading())
    val monthlyRecaps: StateFlow<LocalResource<List<MonthlyRecapItem>>> get() = _monthlyRecaps.asStateFlow()

    private val _accountThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val accountThumbnail: StateFlow<String?> get() = _accountThumbnail.asStateFlow()

    /** Counts and newest artwork for the four Your library cards; null until the first read lands. */
    private val _libraryOverview: MutableStateFlow<LibraryOverview?> = MutableStateFlow(null)
    val libraryOverview: StateFlow<LibraryOverview?> get() = _libraryOverview.asStateFlow()

    /**
     * The Cached card's count and newest artwork. Kept apart from [libraryOverview] because the
     * player cache is no table to observe: the screen asks again each time it is shown.
     */
    private val _cachedPreview: MutableStateFlow<LibraryCollectionPreview?> = MutableStateFlow(null)
    val cachedPreview: StateFlow<LibraryCollectionPreview?> get() = _cachedPreview.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }

    /**
     * Whether the Wrapped chip has anything behind it.
     *
     * The same setting the Analytics tab follows, read the same way — Wrapped and the recaps are
     * built entirely from `playback_event`, which local tracking is what fills.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val localTrackingEnabled = dataStoreManager.localTrackingEnabled.mapLatest { it == DataStoreManager.TRUE }

    init {
        viewModelScope.launch {
            _libraryLayoutMode.value = parseLibraryLayoutMode(dataStoreManager.getString(LIBRARY_LAYOUT_MODE_KEY).first())
        }
    }

    fun setLibraryLayoutMode(mode: LibraryLayoutMode) {
        _libraryLayoutMode.value = mode
        viewModelScope.launch { dataStoreManager.putString(LIBRARY_LAYOUT_MODE_KEY, mode.storedValue) }
    }

    init {
        viewModelScope.launch {
            val currentScreenJob =
                launch {
                    dataStoreManager.getString("library_current_screen").first()?.let { chipType ->
                        LibraryChipType.fromStringValue(chipType)?.let { saved ->
                            val normalized =
                                when (saved) {
                                    LibraryChipType.YOUTUBE_MUSIC_PLAYLIST,
                                    LibraryChipType.YOUTUBE_MIX_FOR_YOU,
                                    LibraryChipType.LOCAL_PLAYLIST -> LibraryChipType.YOUR_LIBRARY
                                    LibraryChipType.DEVICE_AUDIO ->
                                        if (com.maxrave.simpmusic.expect.isDeviceAudioSupported()) saved else LibraryChipType.YOUR_LIBRARY
                                    else -> saved
                                }
                            // The chip picked meanwhile wins over the one saved last time.
                            if (!screenChosen) _currentScreen.value = normalized
                        }
                    }
                }
            val sessionJob =
                launch {
                    combine(
                        dataStoreManager.loggedIn.distinctUntilChanged(),
                        dataStoreManager.cookie.distinctUntilChanged(),
                    ) { logged, cookie ->
                        (logged == DataStoreManager.TRUE) to cookie
                    }.collect { (isLogged, cookie) ->
                        _accountThumbnail.value = dataStoreManager.getString("AccountThumbUrl").first().takeIf { !it.isNullOrEmpty() }
                        val cookieHash = cookie.hashCode()
                        if (!isLogged) {
                            lastAppliedCookieHash = null
                            syncedYouTubeCookieHash = null
                            youTubePlaylistJob?.cancel()
                            _isRefreshingYouTubePlaylists.value = false
                            _youTubePlaylist.value = LocalResource.Success(emptyList())
                            dataStoreManager.putString(YT_PLAYLISTS_CACHE_KEY, "")
                        } else {
                            if (lastAppliedCookieHash != null && lastAppliedCookieHash != cookieHash) {
                                syncedYouTubeCookieHash = null
                                _youTubePlaylist.value = LocalResource.Loading()
                            }
                            lastAppliedCookieHash = cookieHash
                            warmUpYouTubePlaylists(forceRefresh = false)
                        }
                    }
                }
            currentScreenJob.join()
            sessionJob.join()
        }
        viewModelScope.launch {
            commonRepository.getLibraryOverview().collect { _libraryOverview.value = it }
        }
    }

    fun refreshCachedPreview() {
        viewModelScope.launch {
            val songs = loadCachedSongs(cacheRepository, songRepository)
            // The same three the other cards fan out.
            _cachedPreview.value = LibraryCollectionPreview(songs.size, songs.mapNotNull { it.thumbnails }.take(3))
        }
    }

    fun setCurrentScreen(chipType: LibraryChipType) {
        screenChosen = true
        _currentScreen.value = chipType
        viewModelScope.launch {
            dataStoreManager.putString("library_current_screen", chipType.toStringValue())
        }
    }

    fun getRecentlyAdded() {
        viewModelScope.launch {
            commonRepository.getAllRecentData().collectLatest { data ->
                val temp: MutableList<RecentlyType> = mutableListOf()
                temp.addAll(data)
                temp
                    .find {
                        it is PlaylistEntity && it.id.isRadioPlaylistId()
                    }.let {
                        temp.remove(it)
                    }
                temp.removeIf { it is SongEntity && it.inLibrary == Config.REMOVED_SONG_DATE_TIME }
                temp.reverse()
                _recentlyAdded.value = LocalResource.Success(temp.toImmutableList())
            }
        }
    }

    fun warmUpYouTubePlaylists(forceRefresh: Boolean = false) {
        if (!forceRefresh && youTubePlaylistJob?.isActive == true) return
        youTubePlaylistJob?.cancel()
        youTubePlaylistJob =
            viewModelScope.launch {
                val isLogged = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE
                if (!isLogged) {
                    syncedYouTubeCookieHash = null
                    _isRefreshingYouTubePlaylists.value = false
                    _youTubePlaylist.value = LocalResource.Success(emptyList())
                    return@launch
                }
                val cookieHash = dataStoreManager.cookie.first().hashCode()
                val cachedFromDisk =
                    if (_youTubePlaylist.value !is LocalResource.Success || _youTubePlaylist.value.data.isNullOrEmpty()) {
                        decodeYouTubePlaylistsCache(
                            raw = dataStoreManager.getString(YT_PLAYLISTS_CACHE_KEY).first(),
                            expectedAccountHash = cookieHash,
                        )?.also { restored ->
                            _youTubePlaylist.value = LocalResource.Success(restored)
                        }
                    } else {
                        null
                    }
                val hasVisibleData =
                    !_youTubePlaylist.value.data.isNullOrEmpty() || !cachedFromDisk.isNullOrEmpty()
                if (!forceRefresh && syncedYouTubeCookieHash == cookieHash && hasVisibleData) {
                    return@launch
                }
                if (forceRefresh) {
                    _isRefreshingYouTubePlaylists.value = true
                } else if (!hasVisibleData) {
                    _youTubePlaylist.value = LocalResource.Loading()
                }
                try {
                    playlistRepository.getLibraryPlaylist().collect { data ->
                        if (data != null) {
                            syncedYouTubeCookieHash = cookieHash
                            val current = _youTubePlaylist.value.data
                            if (_youTubePlaylist.value !is LocalResource.Success || current != data) {
                                _youTubePlaylist.value = LocalResource.Success(data)
                            }
                            dataStoreManager.putString(
                                YT_PLAYLISTS_CACHE_KEY,
                                encodeYouTubePlaylistsCache(cookieHash, data),
                            )
                        } else if (!hasVisibleData) {
                            _youTubePlaylist.value = LocalResource.Success(emptyList())
                        }
                    }
                } finally {
                    _isRefreshingYouTubePlaylists.value = false
                }
            }
    }

    fun getYouTubePlaylist() {
        warmUpYouTubePlaylists(forceRefresh = true)
    }

    fun clearYouTubePlaylist() {
        youTubePlaylistJob?.cancel()
        syncedYouTubeCookieHash = null
        _isRefreshingYouTubePlaylists.value = false
        _youTubePlaylist.value = LocalResource.Success(emptyList())
    }

    fun getYouTubeMixedForYou() {
        _youTubeMixForYou.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getMixedForYou().collect { data ->
                _youTubeMixForYou.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeLoggedIn(): Boolean = runBlocking { dataStoreManager.loggedIn.first() } == DataStoreManager.TRUE

    fun getPlaylistFavorite() {
        viewModelScope.launch {
            albumRepository.getLikedAlbums().collect { album ->
                val temp: MutableList<PlaylistType> = mutableListOf()
                temp.addAll(album)
                playlistRepository.getLikedPlaylists().collect { playlist ->
                    temp.addAll(playlist)
                    val sortedList =
                        temp.sortedWith<PlaylistType>(
                            Comparator { p0, p1 ->
                                val timeP0: LocalDateTime? =
                                    when (p0) {
                                        is AlbumEntity -> p0.favoriteAt ?: p0.inLibrary
                                        is PlaylistEntity -> p0.favoriteAt ?: p0.inLibrary
                                        else -> null
                                    }
                                val timeP1: LocalDateTime? =
                                    when (p1) {
                                        is AlbumEntity -> p1.favoriteAt ?: p1.inLibrary
                                        is PlaylistEntity -> p1.favoriteAt ?: p1.inLibrary
                                        else -> null
                                    }
                                if (timeP0 == null || timeP1 == null) {
                                    return@Comparator if (timeP0 == null && timeP1 == null) {
                                        0
                                    } else if (timeP0 == null) {
                                        -1
                                    } else {
                                        1
                                    }
                                }
                                timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
                            },
                        )
                    _favoritePlaylist.value = LocalResource.Success(sortedList)
                }
            }
        }
    }

    fun getFavoritePodcasts() {
        viewModelScope.launch {
            podcastRepository.getFavoritePodcasts().collectLatest { podcasts ->
                val sortedList = podcasts.sortedByDescending { it.favoriteTime }
                _favoritePodcasts.value = LocalResource.Success(sortedList)
            }
        }
    }

    fun getCanvasSong() {
        _listCanvasSong.value = LocalResource.Loading()
        viewModelScope.launch {
            songRepository.getCanvasSong(max = 5).collect { data ->
                _listCanvasSong.value = LocalResource.Success(data)
            }
        }
    }

    fun getLocalPlaylist() {
        _yourLocalPlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            localPlaylistRepository.getAllLocalPlaylists().collect { values ->
//                    _listLocalPlaylist.postValue(values)
                _yourLocalPlaylist.value = LocalResource.Success(values.reversed())
            }
        }
    }

    fun getDownloadedPlaylist() {
        viewModelScope.launch {
            playlistRepository.getAllDownloadedPlaylist().collect { values ->
                _downloadedPlaylist.value = LocalResource.Success(values)
            }
        }
    }

    /**
     * Which of the last twelve months the user actually listened in, and what each tile shows.
     *
     * A month with no plays is left out rather than shown empty: a "Recap March" that opens onto
     * nothing is worse than no row at all. Twelve is a cap, not a quota — a new install shows one
     * row, or none.
     *
     * The count comes first and gates everything after it: twelve `COUNT`s over an indexed
     * timestamp range are cheap, so the months with nothing in them are dropped before anything
     * asks them for a ranking. Only the survivors pay for a cover.
     *
     * Title and cover are resolved here rather than in the tile, which cannot suspend: the title
     * needs a month name out of a string resource with a format argument, and the cover needs a
     * ranking query followed by a song lookup.
     */
    fun getMonthlyRecaps() {
        _monthlyRecaps.value = LocalResource.Loading()
        viewModelScope.launch {
            val today = now().date
            val thisMonth = LocalDate(today.year, today.month, 1)
            val months =
                (0 until MONTHS_OF_RECAP)
                    .map { thisMonth.minus(it, DateTimeUnit.MONTH) }
                    .mapNotNull { firstDay ->
                        val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                        val start = firstDay.atTime(0, 0)
                        val end = lastDay.atTime(23, 59, 59)
                        val plays =
                            analyticsRepository
                                .getPlaybackEventCountInRange(
                                    startTimestamp = start,
                                    endTimestamp = end,
                                ).firstOrNull() ?: 0L
                        if (plays <= 0L) return@mapNotNull null
                        MonthlyRecapItem(
                            year = firstDay.year,
                            month = firstDay.month.number,
                            title = recapTitle(firstDay.year, firstDay.month, today.year),
                        )
                    }
            _monthlyRecaps.value = LocalResource.Success(months)
        }
    }

    /**
     * "Recap January", or "Recap January 2025" once the year stops being obvious.
     *
     * The same rule and the same two format strings as the header the tile opens — see
     * [LibraryDynamicPlaylistType.title]. Fully qualified because [BaseViewModel] has a `getString`
     * of its own that takes no format argument and wraps `runBlocking`, which has no business
     * running inside a coroutine that is already suspended here.
     */
    private suspend fun recapTitle(
        year: Int,
        month: Month,
        currentYear: Int,
    ): String {
        val monthName =
            org.jetbrains.compose.resources
                .getString(monthFullNameResource(month))
        return if (year == currentYear) {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month, monthName)
        } else {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month_year, monthName, year.toString())
        }
    }

    fun getChartPlaylists() {
        _chartPlaylists.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getChartPlaylist().collectLatest {
                when (it) {
                    is Resource.Success -> _chartPlaylists.value = LocalResource.Success(it.data ?: emptyList())
                    is Resource.Error -> _chartPlaylists.value = LocalResource.Error(it.message ?: "Unknown error")
                }
            }
        }
    }

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            val localPlaylistEntity = LocalPlaylistEntity(title = title)
            localPlaylistRepository
                .insertLocalPlaylist(
                    localPlaylistEntity,
                    getString(Res.string.added_local_playlist),
                ).lastOrNull()
                ?.let {
                    log("Created playlist with id: $it")
                }
            getLocalPlaylist()
        }
    }

    fun createSyncedPlaylist(
        title: String,
        privacyStatus: String,
        onCreated: (DualPlaylistCreationResult) -> Unit,
    ) {
        if (_isCreatingPlaylist.value) return
        viewModelScope.launch {
            _isCreatingPlaylist.value = true
            try {
                val isLogged = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE
                val resource =
                    localPlaylistRepository
                        .createPlaylistSyncedWithYouTube(
                            title = title,
                            description = null,
                            privacyStatus = privacyStatus,
                            isLoggedIn = isLogged,
                        ).firstOrNull { it !is LocalResource.Loading<*> }
                when (resource) {
                    is LocalResource.Success -> {
                        val created = resource.data
                        if (created != null) {
                            val ytId = created.youtubePlaylistId
                            if (ytId != null) {
                                val optimistic =
                                    PlaylistsResult(
                                        author = "",
                                        browseId = ytId,
                                        category = "",
                                        itemCount = "",
                                        resultType = "PLAYLIST",
                                        thumbnails = emptyList(),
                                        title = title,
                                    )
                                val updated =
                                    prependCreatedYouTubePlaylist(
                                        existing = _youTubePlaylist.value.data ?: emptyList(),
                                        created = optimistic,
                                    )
                                _youTubePlaylist.value = LocalResource.Success(updated)
                                val cookieHash = dataStoreManager.cookie.first().hashCode()
                                dataStoreManager.putString(
                                    YT_PLAYLISTS_CACHE_KEY,
                                    encodeYouTubePlaylistsCache(cookieHash, updated),
                                )
                                warmUpYouTubePlaylists(forceRefresh = true)
                                makeToast(getString(Res.string.playlist_created_synced))
                            } else if (isLogged) {
                                makeToast(getString(Res.string.playlist_created_local_sync_failed))
                            } else {
                                makeToast(getString(Res.string.added_local_playlist))
                            }
                            getLocalPlaylist()
                            onCreated(created)
                        } else {
                            makeToast(getString(Res.string.error))
                        }
                    }

                    is LocalResource.Error -> {
                        makeToast(resource.message ?: getString(Res.string.error))
                    }

                    else -> Unit
                }
            } finally {
                _isCreatingPlaylist.value = false
            }
        }
    }

    fun deleteSong(videoId: String) {
        _recentlyAdded.value = LocalResource.Loading()
        viewModelScope.launch {
            songRepository.setInLibrary(videoId, Config.REMOVED_SONG_DATE_TIME)
            songRepository.resetTotalPlayTime(videoId)
            delay(500) // Wait for the database to update
            getRecentlyAdded()
        }
    }

    companion object {
        /** How far back the Wrapped tab offers recaps, counting the current month as the first. */
        private const val MONTHS_OF_RECAP = 12
        private const val YT_PLAYLISTS_CACHE_KEY = "yt_library_playlists_cache_v1"
        private const val LIBRARY_LAYOUT_MODE_KEY = "library_yt_playlist_layout_v1"
    }
}