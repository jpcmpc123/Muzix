package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.domain.data.entities.SearchHistory
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.data.type.SearchResultType
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.utils.LIVE_RESULT_TYPE
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toQueryList
import com.maxrave.logger.LogLevel
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.featured_playlists
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.songs
import simpmusic.composeapp.generated.resources.videos

// State cho tìm kiếm
data class SearchScreenState(
    val activeQuery: String = "",
    val searchType: SearchType = SearchType.ALL,
    val searchAllResult: List<SearchResultType> = emptyList(),
    val searchSongsResult: List<SongsResult> = emptyList(),
    val searchVideosResult: List<VideosResult> = emptyList(),
    val searchAlbumsResult: List<AlbumsResult> = emptyList(),
    val searchArtistsResult: List<ArtistsResult> = emptyList(),
    val searchPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchFeaturedPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchPodcastsResult: List<PlaylistsResult> = emptyList(),
    val suggestQueries: List<String> = emptyList(),
    val suggestYTItems: List<SearchResultType> = emptyList(),
    val continuationByType: Map<SearchType, String?> = emptyMap(),
    val isLoadingMore: Boolean = false,
)

// Loại tìm kiếm
enum class SearchType {
    ALL,
    SONGS,
    VIDEOS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    FEATURED_PLAYLISTS,
    PODCASTS,
}

fun SearchType.toStringRes(): StringResource =
    when (this) {
        SearchType.ALL -> Res.string.all
        SearchType.SONGS -> Res.string.songs
        SearchType.VIDEOS -> Res.string.videos
        SearchType.ALBUMS -> Res.string.albums
        SearchType.ARTISTS -> Res.string.artists
        SearchType.PLAYLISTS -> Res.string.playlists
        SearchType.FEATURED_PLAYLISTS -> Res.string.featured_playlists
        SearchType.PODCASTS -> Res.string.podcasts
    }

// UI state cho tìm kiếm
sealed class SearchScreenUIState {
    object Empty : SearchScreenUIState()

    object Loading : SearchScreenUIState()

    object Success : SearchScreenUIState()

    object Error : SearchScreenUIState()
}

internal fun buildSearchAllList(
    artists: List<ArtistsResult>,
    songs: List<SongsResult>,
    albums: List<AlbumsResult>,
    playlists: List<PlaylistsResult>,
    videos: List<VideosResult>,
    featuredPlaylists: List<PlaylistsResult> = emptyList(),
    podcasts: List<PlaylistsResult> = emptyList(),
): List<SearchResultType> {
    val (liveVideos, regularVideos) = videos.partition { it.resultType == LIVE_RESULT_TYPE }
    return buildList {
        addAll(artists.take(3))
        addAll(liveVideos)
        addAll(songs.take(8))
        addAll(albums.take(6))
        addAll(playlists.take(6))
        addAll(regularVideos.take(6))
        addAll(featuredPlaylists.take(4))
        addAll(podcasts.take(4))
    }
}

internal inline fun <T> mergePagedResults(
    existing: List<T>,
    incoming: List<T>,
    crossinline keySelector: (T) -> String,
): List<T> {
    if (incoming.isEmpty()) return existing
    if (existing.isEmpty()) return incoming.distinctBy(keySelector)
    val seen = HashSet<String>(existing.size + incoming.size)
    val merged = ArrayList<T>(existing.size + incoming.size)
    existing.forEach { item ->
        if (seen.add(keySelector(item))) {
            merged.add(item)
        }
    }
    incoming.forEach { item ->
        if (seen.add(keySelector(item))) {
            merged.add(item)
        }
    }
    return merged
}

internal fun shouldFetchTabForQuery(
    activeQuery: String,
    targetType: SearchType,
    loadedQueryByType: Map<SearchType, String>,
): Boolean {
    val clean = activeQuery.trim()
    if (clean.isEmpty()) return false
    return loadedQueryByType[targetType] != clean
}

class SearchViewModel(
    private val dataStoreManager: DataStoreManager,
    private val searchRepository: SearchRepository,
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _searchScreenUIState = MutableStateFlow<SearchScreenUIState>(SearchScreenUIState.Empty)
    val searchScreenUIState: StateFlow<SearchScreenUIState> get() = _searchScreenUIState.asStateFlow()

    private val _searchScreenState = MutableStateFlow(SearchScreenState())
    val searchScreenState: StateFlow<SearchScreenState> get() = _searchScreenState.asStateFlow()

    private val _searchHistory: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val searchHistory: StateFlow<List<String>> get() = _searchHistory.asStateFlow()

    /** Browse categories shown on the empty search screen, the way YouTube Music itself does. */
    private val _moodAndGenres: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val moodAndGenres: StateFlow<Mood?> get() = _moodAndGenres.asStateFlow()

    private val _moodError: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val moodError: StateFlow<Boolean> get() = _moodError.asStateFlow()

    /** Cover art per category params, filled in as tiles scroll into view. */
    private val _moodArtwork: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())
    val moodArtwork: StateFlow<Map<String, String>> get() = _moodArtwork.asStateFlow()

    private val requestedArtwork = mutableSetOf<String>()
    private val loadedQueryByType = mutableMapOf<SearchType, String>()
    private var suggestJob: Job? = null
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var moodJob: Job? = null

    var regionCode: String? = null
    var language: String? = null

    init {
        viewModelScope.launch {
            regionCode = dataStoreManager.location.first()
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
        }
        getSearchHistory()
        getMoodAndGenres()
    }

    /**
     * Fetched once when available; if offline on first launch, [moodError] lets the UI offer retry
     * rather than spinning forever.
     */
    private fun getMoodAndGenres(forceRetry: Boolean = false) {
        if (_moodAndGenres.value != null && !forceRetry) return
        moodJob?.cancel()
        _moodError.value = false
        moodJob =
            viewModelScope.launch {
                homeRepository.getMoodAndMomentsData().collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _moodAndGenres.value = resource.data
                            _moodError.value = false
                        }

                        is Resource.Error -> {
                            if (_moodAndGenres.value == null) {
                                _moodError.value = true
                            }
                        }
                    }
                }
            }
    }

    fun retryMoodAndGenres() {
        getMoodAndGenres(forceRetry = true)
    }

    /**
     * Resolve the cover for one category. Called from the tile itself, so only categories the user
     * actually scrolls to cost a request — [requestedArtwork] keeps a tile scrolling in and out of
     * view from firing it again.
     */
    fun loadMoodArtwork(params: String) {
        if (!requestedArtwork.add(params)) return
        viewModelScope.launch {
            homeRepository.getMoodCategoryArtwork(params).collect { url ->
                if (url != null) {
                    _moodArtwork.update { it + (params to url) }
                }
            }
        }
    }

    private fun getSearchHistory() {
        viewModelScope.launch {
            searchRepository.getSearchHistory().collect { values ->
                if (values.isNotEmpty()) {
                    _searchHistory.value = values.toQueryList().reversed().distinct()
                } else {
                    _searchHistory.value = emptyList()
                }
            }
        }
    }

    fun insertSearchHistory(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            searchRepository.insertSearchHistory(SearchHistory(query = clean)).collectLatest {
                Logger.d(tag, "Inserted search history: $clean, $it")
                getSearchHistory()
            }
        }
    }

    fun deleteSearchHistory() {
        viewModelScope.launch {
            searchRepository.deleteSearchHistory()
            getSearchHistory()
        }
    }

    fun deleteSearchHistoryItem(query: String) {
        viewModelScope.launch {
            searchRepository.deleteSearchHistoryByQuery(query)
            getSearchHistory()
        }
    }

    fun clearSuggestions() {
        suggestJob?.cancel()
        _searchScreenState.update { state ->
            state.copy(
                suggestQueries = emptyList(),
                suggestYTItems = emptyList(),
            )
        }
    }

    fun resetToEmpty() {
        suggestJob?.cancel()
        searchJob?.cancel()
        loadMoreJob?.cancel()
        loadedQueryByType.clear()
        _searchScreenState.update { state ->
            state.copy(
                activeQuery = "",
                searchAllResult = emptyList(),
                searchSongsResult = emptyList(),
                searchVideosResult = emptyList(),
                searchAlbumsResult = emptyList(),
                searchArtistsResult = emptyList(),
                searchPlaylistsResult = emptyList(),
                searchFeaturedPlaylistsResult = emptyList(),
                searchPodcastsResult = emptyList(),
                suggestQueries = emptyList(),
                suggestYTItems = emptyList(),
                continuationByType = emptyMap(),
                isLoadingMore = false,
            )
        }
        _searchScreenUIState.value = SearchScreenUIState.Empty
    }

    private fun prepareQueryContext(
        cleanQuery: String,
        targetType: SearchType,
        forceRefresh: Boolean,
    ) {
        val queryChanged = _searchScreenState.value.activeQuery != cleanQuery
        if (queryChanged) {
            loadedQueryByType.clear()
            _searchScreenState.update { state ->
                state.copy(
                    activeQuery = cleanQuery,
                    searchType = targetType,
                    searchAllResult = emptyList(),
                    searchSongsResult = emptyList(),
                    searchVideosResult = emptyList(),
                    searchAlbumsResult = emptyList(),
                    searchArtistsResult = emptyList(),
                    searchPlaylistsResult = emptyList(),
                    searchFeaturedPlaylistsResult = emptyList(),
                    searchPodcastsResult = emptyList(),
                    continuationByType = emptyMap(),
                    isLoadingMore = false,
                )
            }
        } else {
            if (forceRefresh) {
                loadedQueryByType.remove(targetType)
                if (targetType == SearchType.ALL) {
                    loadedQueryByType.clear()
                }
            }
            _searchScreenState.update { state ->
                state.copy(
                    searchType = targetType,
                    isLoadingMore = false,
                )
            }
        }
    }

    fun searchByType(
        query: String,
        type: SearchType = _searchScreenState.value.searchType,
        forceRefresh: Boolean = true,
    ) {
        when (type) {
            SearchType.ALL -> searchAll(query, forceRefresh = forceRefresh)
            SearchType.SONGS -> searchSongs(query, forceRefresh = forceRefresh)
            SearchType.VIDEOS -> searchVideos(query, forceRefresh = forceRefresh)
            SearchType.ALBUMS -> searchAlbums(query, forceRefresh = forceRefresh)
            SearchType.ARTISTS -> searchArtists(query, forceRefresh = forceRefresh)
            SearchType.PLAYLISTS -> searchPlaylists(query, forceRefresh = forceRefresh)
            SearchType.FEATURED_PLAYLISTS -> searchFeaturedPlaylist(query, forceRefresh = forceRefresh)
            SearchType.PODCASTS -> searchPodcast(query, forceRefresh = forceRefresh)
        }
    }

    fun retryCurrentSearch(fallbackQuery: String = "") {
        val currentState = _searchScreenState.value
        val targetQuery = currentState.activeQuery.ifBlank { fallbackQuery.trim() }
        if (targetQuery.isNotEmpty()) {
            searchByType(targetQuery, currentState.searchType, forceRefresh = true)
        }
    }

    fun searchSongs(
        query: String,
        forceRefresh: Boolean = true,
    ) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        prepareQueryContext(clean, SearchType.SONGS, forceRefresh)
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob =
            viewModelScope.launch {
                searchRepository.searchSongsPaged(clean).collect { values ->
                    if (_searchScreenState.value.activeQuery != clean) return@collect
                    when (values) {
                        is Resource.Success -> {
                            val page = values.data
                            loadedQueryByType[SearchType.SONGS] = clean
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.SONGS,
                                    searchSongsResult = page?.items.orEmpty(),
                                    continuationByType = state.continuationByType + (SearchType.SONGS to page?.continuation),
                                )
                            }
                            _searchScreenUIState.value = SearchScreenUIState.Success
                        }

                        is Resource.Error -> {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
    }

    fun searchAll(
        query: String,
        forceRefresh: Boolean = true,
    ) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        prepareQueryContext(clean, SearchType.ALL, forceRefresh)
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob =
            viewModelScope.launch {
                var song = emptyList<SongsResult>()
                var songCont: String? = null
                var video = emptyList<VideosResult>()
                var videoCont: String? = null
                var album = emptyList<AlbumsResult>()
                var albumCont: String? = null
                var artist = emptyList<ArtistsResult>()
                var artistCont: String? = null
                var playlist = emptyList<PlaylistsResult>()
                var playlistCont: String? = null
                var successCount = 0

                val job1 =
                    launch {
                        searchRepository.searchSongsPaged(clean).collect { values ->
                            if (values is Resource.Success) {
                                successCount++
                                song = values.data?.items.orEmpty()
                                songCont = values.data?.continuation
                            }
                        }
                    }
                val job2 =
                    launch {
                        searchRepository.searchArtistsPaged(clean).collect { values ->
                            if (values is Resource.Success) {
                                successCount++
                                artist = values.data?.items.orEmpty()
                                artistCont = values.data?.continuation
                            }
                        }
                    }
                val job3 =
                    launch {
                        searchRepository.searchAlbumsPaged(clean).collect { values ->
                            if (values is Resource.Success) {
                                successCount++
                                album = values.data?.items.orEmpty()
                                albumCont = values.data?.continuation
                            }
                        }
                    }
                val job4 =
                    launch {
                        searchRepository.searchPlaylistsPaged(clean).collect { values ->
                            if (values is Resource.Success) {
                                successCount++
                                playlist = values.data?.items.orEmpty()
                                playlistCont = values.data?.continuation
                            }
                        }
                    }
                val job5 =
                    launch {
                        searchRepository.searchVideosPaged(clean).collect { values ->
                            if (values is Resource.Success) {
                                successCount++
                                video = values.data?.items.orEmpty()
                                videoCont = values.data?.continuation
                            }
                        }
                    }

                job1.join()
                job2.join()
                job3.join()
                job4.join()
                job5.join()

                if (_searchScreenState.value.activeQuery != clean) return@launch

                if (successCount == 0) {
                    _searchScreenUIState.value = SearchScreenUIState.Error
                    return@launch
                }

                loadedQueryByType[SearchType.ALL] = clean
                loadedQueryByType[SearchType.SONGS] = clean
                loadedQueryByType[SearchType.ARTISTS] = clean
                loadedQueryByType[SearchType.ALBUMS] = clean
                loadedQueryByType[SearchType.PLAYLISTS] = clean
                loadedQueryByType[SearchType.VIDEOS] = clean

                val allItems =
                    buildSearchAllList(
                        artists = artist,
                        songs = song,
                        albums = album,
                        playlists = playlist,
                        videos = video,
                    )

                _searchScreenState.update { state ->
                    state.copy(
                        searchType = SearchType.ALL,
                        searchAllResult = allItems,
                        searchSongsResult = song,
                        searchArtistsResult = artist,
                        searchAlbumsResult = album,
                        searchPlaylistsResult = playlist,
                        searchVideosResult = video,
                        continuationByType =
                            state.continuationByType +
                                mapOf(
                                    SearchType.SONGS to songCont,
                                    SearchType.ARTISTS to artistCont,
                                    SearchType.ALBUMS to albumCont,
                                    SearchType.PLAYLISTS to playlistCont,
                                    SearchType.VIDEOS to videoCont,
                                ),
                    )
                }
                _searchScreenUIState.value = SearchScreenUIState.Success
            }
    }

    fun suggestQuery(query: String) {
        suggestJob?.cancel()
        val clean = query.trim()
        if (clean.isEmpty()) {
            _searchScreenState.update { state ->
                state.copy(
                    suggestQueries = emptyList(),
                    suggestYTItems = emptyList(),
                )
            }
            return
        }
        suggestJob =
            viewModelScope.launch {
                delay(250)
                searchRepository.getSuggestQuery(clean).collect { values ->
                    when (values) {
                        is Resource.Success -> {
                            values.data?.let { suggestData ->
                                _searchScreenState.update { state ->
                                    state.copy(
                                        suggestQueries = suggestData.queries,
                                        suggestYTItems = suggestData.recommendedItems,
                                    )
                                }
                            }
                        }

                        is Resource.Error -> {
                            log("Error fetching suggest queries: ${values.message}", LogLevel.ERROR)
                        }
                    }
                }
            }
    }

    fun searchAlbums(
        query: String,
        forceRefresh: Boolean = true,
    ) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        prepareQueryContext(clean, SearchType.ALBUMS, forceRefresh)
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob =
            viewModelScope.launch {
                searchRepository.searchAlbumsPaged(clean).collect { values ->
                    if (_searchScreenState.value.activeQuery != clean) return@collect
                    when (values) {
                        is Resource.Success -> {
                            val page = values.data
                            loadedQueryByType[SearchType.ALBUMS] = clean
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.ALBUMS,
                                    searchAlbumsResult = page?.items.orEmpty(),
                                    continuationByType = state.continuationByType + (SearchType.ALBUMS to page?.continuation),
                                )
                            }
                            _searchScreenUIState.value = SearchScreenUIState.Success
                        }

                        is Resource.Error -> {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
    }

    fun searchFeaturedPlaylist(
        query: String,
        forceRefresh: Boolean = true,
    ) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        prepareQueryContext(clean, SearchType.FEATURED_PLAYLISTS, forceRefresh)
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob =
            viewModelScope.launch {
                searchRepository.searchFeaturedPlaylistsPaged(clean).collect { values ->
                    if (_searchScreenState.value.activeQuery != clean) return@collect
                    when (values) {
                        is Resource.Success -> {
                            val page = values.data
                            loadedQueryByType[SearchType.FEATURED_PLAYLISTS] = clean
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.FEATURED_PLAYLISTS,
                                    searchFeaturedPlaylistsResult = page?.items.orEmpty(),
                                    continuationByType =
                                        state.continuationByType + (SearchType.FEATURED_PLAYLISTS to page?.continuation),
                                )
                            }
                            _searchScreenUIState.value = SearchScreenUIState.Success
                        }

                        is Resource.Error -> {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
    }

    fun searchPodcast(
        query: String,
        forceRefresh: Boolean = true,
    ) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        prepareQueryContext(clean, SearchType.PODCASTS, forceRefresh)
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob =
            viewModelScope.launch {
                searchRepository.searchPodcastsPaged(clean).collect { values ->
                    if (_searchScreenState.value.activeQuery != clean) return@collect
                    when (values) {
                        is Resource.Success -> {
                            val page = values.data
                            loadedQueryByType[SearchType.PODCASTS] = clean
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.PODCASTS,
                                    searchPodcastsResult = page?.items.orEmpty(),
                                    continuationByType = state.continuationByType + (SearchType.PODCASTS to page?.continuation),
                                )
                            }
                            _searchScreenUIState.value = SearchScreenUIState.Success
                        }

                        is Resource.Error -> {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
    }

    fun searchArtists(
        query: String,
        forceRefresh: Boolean = true,
    ) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        prepareQueryContext(clean, SearchType.ARTISTS, forceRefresh)
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob =
            viewModelScope.launch {
                searchRepository.searchArtistsPaged(clean).collect { values ->
                    if (_searchScreenState.value.activeQuery != clean) return@collect
                    when (values) {
                        is Resource.Success -> {
                            val page = values.data
                            loadedQueryByType[SearchType.ARTISTS] = clean
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.ARTISTS,
                                    searchArtistsResult = page?.items.orEmpty(),
                                    continuationByType = state.continuationByType + (SearchType.ARTISTS to page?.continuation),
                                )
                            }
                            _searchScreenUIState.value = SearchScreenUIState.Success
                        }

                        is Resource.Error -> {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
    }

    fun searchPlaylists(
        query: String,
        forceRefresh: Boolean = true,
    ) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        prepareQueryContext(clean, SearchType.PLAYLISTS, forceRefresh)
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob =
            viewModelScope.launch {
                searchRepository.searchPlaylistsPaged(clean).collect { values ->
                    if (_searchScreenState.value.activeQuery != clean) return@collect
                    when (values) {
                        is Resource.Success -> {
                            val page = values.data
                            loadedQueryByType[SearchType.PLAYLISTS] = clean
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.PLAYLISTS,
                                    searchPlaylistsResult = page?.items.orEmpty(),
                                    continuationByType = state.continuationByType + (SearchType.PLAYLISTS to page?.continuation),
                                )
                            }
                            _searchScreenUIState.value = SearchScreenUIState.Success
                        }

                        is Resource.Error -> {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
    }

    fun searchVideos(
        query: String,
        forceRefresh: Boolean = true,
    ) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        prepareQueryContext(clean, SearchType.VIDEOS, forceRefresh)
        _searchScreenUIState.value = SearchScreenUIState.Loading
        searchJob =
            viewModelScope.launch {
                searchRepository.searchVideosPaged(clean).collect { values ->
                    if (_searchScreenState.value.activeQuery != clean) return@collect
                    when (values) {
                        is Resource.Success -> {
                            val page = values.data
                            loadedQueryByType[SearchType.VIDEOS] = clean
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.VIDEOS,
                                    searchVideosResult = page?.items.orEmpty(),
                                    continuationByType = state.continuationByType + (SearchType.VIDEOS to page?.continuation),
                                )
                            }
                            _searchScreenUIState.value = SearchScreenUIState.Success
                        }

                        is Resource.Error -> {
                            _searchScreenUIState.value = SearchScreenUIState.Error
                        }
                    }
                }
            }
    }

    fun loadMoreForCurrentType() {
        val snapshot = _searchScreenState.value
        val type = snapshot.searchType
        val activeQuery = snapshot.activeQuery
        if (snapshot.isLoadingMore || activeQuery.isBlank() || type == SearchType.ALL) return
        val token = snapshot.continuationByType[type] ?: return

        loadMoreJob?.cancel()
        _searchScreenState.update { it.copy(isLoadingMore = true) }
        loadMoreJob =
            viewModelScope.launch {
                try {
                    when (type) {
                        SearchType.SONGS -> {
                            searchRepository.searchSongsPaged(activeQuery, token).collect { res ->
                                if (_searchScreenState.value.activeQuery != activeQuery) return@collect
                                if (res is Resource.Success) {
                                    val page = res.data
                                    _searchScreenState.update { state ->
                                        state.copy(
                                            searchSongsResult =
                                                mergePagedResults(state.searchSongsResult, page?.items.orEmpty()) { it.videoId },
                                            continuationByType =
                                                state.continuationByType + (SearchType.SONGS to page?.continuation),
                                        )
                                    }
                                }
                            }
                        }

                        SearchType.VIDEOS -> {
                            searchRepository.searchVideosPaged(activeQuery, token).collect { res ->
                                if (_searchScreenState.value.activeQuery != activeQuery) return@collect
                                if (res is Resource.Success) {
                                    val page = res.data
                                    _searchScreenState.update { state ->
                                        state.copy(
                                            searchVideosResult =
                                                mergePagedResults(state.searchVideosResult, page?.items.orEmpty()) { it.videoId },
                                            continuationByType =
                                                state.continuationByType + (SearchType.VIDEOS to page?.continuation),
                                        )
                                    }
                                }
                            }
                        }

                        SearchType.ALBUMS -> {
                            searchRepository.searchAlbumsPaged(activeQuery, token).collect { res ->
                                if (_searchScreenState.value.activeQuery != activeQuery) return@collect
                                if (res is Resource.Success) {
                                    val page = res.data
                                    _searchScreenState.update { state ->
                                        state.copy(
                                            searchAlbumsResult =
                                                mergePagedResults(state.searchAlbumsResult, page?.items.orEmpty()) { it.browseId },
                                            continuationByType =
                                                state.continuationByType + (SearchType.ALBUMS to page?.continuation),
                                        )
                                    }
                                }
                            }
                        }

                        SearchType.ARTISTS -> {
                            searchRepository.searchArtistsPaged(activeQuery, token).collect { res ->
                                if (_searchScreenState.value.activeQuery != activeQuery) return@collect
                                if (res is Resource.Success) {
                                    val page = res.data
                                    _searchScreenState.update { state ->
                                        state.copy(
                                            searchArtistsResult =
                                                mergePagedResults(state.searchArtistsResult, page?.items.orEmpty()) { it.browseId },
                                            continuationByType =
                                                state.continuationByType + (SearchType.ARTISTS to page?.continuation),
                                        )
                                    }
                                }
                            }
                        }

                        SearchType.PLAYLISTS -> {
                            searchRepository.searchPlaylistsPaged(activeQuery, token).collect { res ->
                                if (_searchScreenState.value.activeQuery != activeQuery) return@collect
                                if (res is Resource.Success) {
                                    val page = res.data
                                    _searchScreenState.update { state ->
                                        state.copy(
                                            searchPlaylistsResult =
                                                mergePagedResults(state.searchPlaylistsResult, page?.items.orEmpty()) { it.browseId },
                                            continuationByType =
                                                state.continuationByType + (SearchType.PLAYLISTS to page?.continuation),
                                        )
                                    }
                                }
                            }
                        }

                        SearchType.FEATURED_PLAYLISTS -> {
                            searchRepository.searchFeaturedPlaylistsPaged(activeQuery, token).collect { res ->
                                if (_searchScreenState.value.activeQuery != activeQuery) return@collect
                                if (res is Resource.Success) {
                                    val page = res.data
                                    _searchScreenState.update { state ->
                                        state.copy(
                                            searchFeaturedPlaylistsResult =
                                                mergePagedResults(
                                                    state.searchFeaturedPlaylistsResult,
                                                    page?.items.orEmpty(),
                                                ) { it.browseId },
                                            continuationByType =
                                                state.continuationByType +
                                                    (SearchType.FEATURED_PLAYLISTS to page?.continuation),
                                        )
                                    }
                                }
                            }
                        }

                        SearchType.PODCASTS -> {
                            searchRepository.searchPodcastsPaged(activeQuery, token).collect { res ->
                                if (_searchScreenState.value.activeQuery != activeQuery) return@collect
                                if (res is Resource.Success) {
                                    val page = res.data
                                    _searchScreenState.update { state ->
                                        state.copy(
                                            searchPodcastsResult =
                                                mergePagedResults(state.searchPodcastsResult, page?.items.orEmpty()) { it.browseId },
                                            continuationByType =
                                                state.continuationByType + (SearchType.PODCASTS to page?.continuation),
                                        )
                                    }
                                }
                            }
                        }

                        SearchType.ALL -> Unit
                    }
                } finally {
                    _searchScreenState.update { it.copy(isLoadingMore = false) }
                }
            }
    }

    fun setSearchType(searchType: SearchType) {
        val activeQuery = _searchScreenState.value.activeQuery
        _searchScreenState.update { state ->
            state.copy(searchType = searchType)
        }
        if (shouldFetchTabForQuery(activeQuery, searchType, loadedQueryByType)) {
            searchByType(activeQuery, searchType, forceRefresh = false)
        } else if (activeQuery.isNotBlank() && _searchScreenUIState.value is SearchScreenUIState.Error) {
            _searchScreenUIState.value = SearchScreenUIState.Success
        }
    }
}