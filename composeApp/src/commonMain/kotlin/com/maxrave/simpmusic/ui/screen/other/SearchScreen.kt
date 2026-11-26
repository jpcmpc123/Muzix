package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.data.type.SearchResultType
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.LIVE_RESULT_TYPE
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DesktopMiniPlayerClearance
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.expect.ui.SongRecognitionSheet
import com.maxrave.simpmusic.expect.ui.VerticalScrollBar
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import com.maxrave.simpmusic.extension.ultraThinBackdropBarBlur
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.MoodCategoryCard
import com.maxrave.simpmusic.ui.component.sizedThumb
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.extension.toAppDeepLinkOrNull
import com.maxrave.simpmusic.ui.component.ArtistFullWidthItems
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistFullWidthItems
import com.maxrave.simpmusic.ui.component.ShimmerSearchItem
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.ArrowOutward
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SearchScreenUIState
import com.maxrave.simpmusic.viewModel.SearchType
import com.maxrave.simpmusic.viewModel.SearchViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.toStringRes
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import com.maxrave.simpmusic.extension.glassHazeSource
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.maxrave.domain.repository.SongRepository
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.clear_search_history
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.everything_you_need
import simpmusic.composeapp.generated.resources.in_search
import simpmusic.composeapp.generated.resources.no_results_found
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.recognize_song
import simpmusic.composeapp.generated.resources.remove_from_search_history
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.search_for
import simpmusic.composeapp.generated.resources.search_for_songs_artists_albums_playlists_and_more
import simpmusic.composeapp.generated.resources.see_all
import simpmusic.composeapp.generated.resources.song
import simpmusic.composeapp.generated.resources.songs
import simpmusic.composeapp.generated.resources.videos
import simpmusic.composeapp.generated.resources.what_do_you_want_to_listen_to

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    showNowPlayingSheet: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchScreenState by searchViewModel.searchScreenState.collectAsStateWithLifecycle()
    val songRepository: SongRepository = koinInject()
    val uiState by searchViewModel.searchScreenUIState.collectAsStateWithLifecycle()
    val searchHistory by searchViewModel.searchHistory.collectAsStateWithLifecycle()
    val moodAndGenres by searchViewModel.moodAndGenres.collectAsStateWithLifecycle()
    val moodError by searchViewModel.moodError.collectAsStateWithLifecycle()
    val moodArtwork by searchViewModel.moodArtwork.collectAsStateWithLifecycle()

    var searchUIType by rememberSaveable { mutableStateOf(SearchUIType.EMPTY) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var isSearchSubmitted by rememberSaveable { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    var isFocused by rememberSaveable { mutableStateOf(false) }

    val isSearchPanelVisible =
        searchUIType == SearchUIType.SEARCH_HISTORY || searchUIType == SearchUIType.SEARCH_SUGGESTIONS
    val isResultsVisible = searchUIType == SearchUIType.SEARCH_RESULTS
    val dismissSearchPanel: () -> Unit = {
        isExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    val backToSearchHome: () -> Unit = {
        isExpanded = false
        isSearchSubmitted = false
        searchText = ""
        searchViewModel.resetToEmpty()
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    PlatformBackHandler(
        enabled = isSearchPanelVisible || isResultsVisible,
        onBack = {
            if (isSearchPanelVisible) dismissSearchPanel() else backToSearchHome()
        },
    )

    // The bar floats OVER the content (a Box, not a Column) so there is something behind it to
    // blur — same arrangement HomeScreen uses. Each branch owns a scroll state, hoisted here so
    // the bar can tell whether the branch currently on screen is scrolled away from the top.
    // Two columns only on a phone held upright. Anywhere wider — tablet, landscape, desktop — two
    // columns stretch each tile to half the window, and since the tile keeps a 2:1 ratio it grows
    // absurdly tall with it.
    val screenInfo = getScreenSizeInfo()
    val isMobilePortrait = getPlatform() == Platform.Android && screenInfo.wDP < screenInfo.hDP
    val moodGridColumns = if (isMobilePortrait) 2 else 4

    // The floating chip row and frosted search bar both sample this single screen backdrop,
    // avoiding a separate full-screen Haze offscreen recording pass per frame.
    val chipBackdrop = rememberBackdrop(MaterialTheme.colorScheme.background)
    val suggestionsState = rememberLazyListState()
    val historyState = rememberLazyListState()
    val moodGridState = rememberLazyGridState()
    val resultsState = rememberLazyListState()
    var searchBarHeightPx by remember { mutableIntStateOf(0) }
    val searchBarHeight = with(LocalDensity.current) { searchBarHeightPx.toDp() }
    val isContentAtTop by remember {
        derivedStateOf {
            when (searchUIType) {
                SearchUIType.EMPTY ->
                    moodGridState.firstVisibleItemIndex == 0 && moodGridState.firstVisibleItemScrollOffset == 0
                SearchUIType.SEARCH_HISTORY ->
                    historyState.firstVisibleItemIndex == 0 && historyState.firstVisibleItemScrollOffset == 0
                SearchUIType.SEARCH_SUGGESTIONS ->
                    suggestionsState.firstVisibleItemIndex == 0 && suggestionsState.firstVisibleItemScrollOffset == 0
                SearchUIType.SEARCH_RESULTS ->
                    resultsState.firstVisibleItemIndex == 0 && resultsState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    val searchForString = stringResource(Res.string.search_for)
    val songString = stringResource(Res.string.song).lowercase()
    val artistString = stringResource(Res.string.artists).lowercase()
    val albumString = stringResource(Res.string.albums).lowercase()
    val playlistString = stringResource(Res.string.playlists).lowercase()
    val videoString = stringResource(Res.string.videos).lowercase()
    val podcastString = stringResource(Res.string.podcasts).lowercase()

    // Animated Placeholder
    val placeholderTexts =
        remember {
            listOf(
                "$searchForString $songString...",
                "$searchForString $artistString...",
                "$searchForString $albumString...",
                "$searchForString $playlistString...",
                "$searchForString $videoString...",
                "$searchForString $podcastString...",
            )
        }

    var currentPlaceholderIndex by remember { mutableIntStateOf(0) }

    // Animate placeholder - pause when focused
    LaunchedEffect(isFocused) {
        while (!isFocused) {
            delay(3000) // Change every 3 seconds
            currentPlaceholderIndex = (currentPlaceholderIndex + 1) % placeholderTexts.size
        }
    }

    var sheetSong by remember { mutableStateOf<SongEntity?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    var showRecognition by rememberSaveable { mutableStateOf(false) }
    // The sheet asked for by simpmusic://recognize (the Quick Settings tile), opened once and cleared.
    val songRecognitionRequest by sharedViewModel.songRecognitionRequest.collectAsStateWithLifecycle()
    LaunchedEffect(songRecognitionRequest) {
        if (songRecognitionRequest) {
            showRecognition = true
            sharedViewModel.requestSongRecognition(false)
        }
    }
    val currentVideoId by searchViewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val chipRowState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

    val onMoreClick: (SongEntity) -> Unit = { song ->
        sheetSong = song
        showBottomSheet = true
    }

    LaunchedEffect(searchText) {
        if (isFocused) {
            isSearchSubmitted = false
            isExpanded = true
        }
        if (searchText.isNotEmpty() && isFocused) {
            searchViewModel.suggestQuery(searchText)
        } else if (searchText.isEmpty()) {
            searchViewModel.clearSuggestions()
        }
    }

    LaunchedEffect(isSearchSubmitted) {
        if (isSearchSubmitted) {
            isExpanded = false
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            isExpanded = true
        }
    }

    LaunchedEffect(isExpanded, searchText, isFocused) {
        searchUIType =
            if (searchText.isNotEmpty() && isExpanded) {
                SearchUIType.SEARCH_SUGGESTIONS
            } else if (isFocused && isExpanded) {
                SearchUIType.SEARCH_HISTORY
            } else if (searchText.isEmpty()) {
                SearchUIType.EMPTY
            } else {
                SearchUIType.SEARCH_RESULTS
            }
    }

    LaunchedEffect(searchScreenState.searchType) {
        resultsState.scrollToItem(0)
    }

    val hasContinuation =
        searchScreenState.searchType != SearchType.ALL &&
            searchScreenState.continuationByType[searchScreenState.searchType] != null
    val shouldLoadMore by remember(resultsState, hasContinuation) {
        derivedStateOf {
            if (!hasContinuation) {
                false
            } else {
                val layoutInfo = resultsState.layoutInfo
                val totalCount = layoutInfo.totalItemsCount
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalCount > 0 && lastVisibleIndex >= totalCount - 4
            }
        }
    }
    LaunchedEffect(shouldLoadMore, searchScreenState.searchType, searchScreenState.activeQuery) {
        if (shouldLoadMore) {
            searchViewModel.loadMoreForCurrentType()
        }
    }

    // Rotation recreates the activity (MainActivity does not handle orientation changes), and
    // when the search state does not survive that recreation this screen comes back restored —
    // submitted query, results branch — but with the ViewModel UI state back at Empty and no
    // results, which the results branch can only render as "No Result Found". Nothing re-ran the
    // search, so the results stayed lost until the user searched again by hand. Re-run the
    // submitted search once in exactly that situation.
    LaunchedEffect(searchUIType, uiState, isSearchSubmitted, searchText) {
        if (searchUIType == SearchUIType.SEARCH_RESULTS &&
            isSearchSubmitted &&
            uiState is SearchScreenUIState.Empty
        ) {
            val query = searchText.trim()
            if (query.isNotEmpty()) {
                searchViewModel.searchByType(query, searchScreenState.searchType, forceRefresh = true)
            }
        }
    }

    // Same recreation, suggestions branch: the query comes back restored and the UI type is
    // SEARCH_SUGGESTIONS again, but suggestQuery is only ever called from the text-change effect
    // above, which requires the field to be focused — and focus is not restored across
    // recreation. Re-run the suggestion fetch once when unfocused with a restored query.
    var suggestionRestoreQuery by remember { mutableStateOf("") }
    LaunchedEffect(
        searchUIType,
        isFocused,
        isSearchSubmitted,
        searchText,
        searchScreenState.suggestYTItems,
        searchScreenState.suggestQueries,
    ) {
        if (searchUIType == SearchUIType.SEARCH_SUGGESTIONS &&
            !isFocused &&
            !isSearchSubmitted &&
            searchScreenState.suggestYTItems.isEmpty() &&
            searchScreenState.suggestQueries.isEmpty()
        ) {
            val query = searchText.trim()
            if (query.isNotEmpty() && query != suggestionRestoreQuery) {
                suggestionRestoreQuery = query
                searchViewModel.suggestQuery(query)
            }
        }
    }

    //On search icon click while on search screen, open keyboard. Android only feature
    if (getPlatform() == Platform.Android) {
        val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
        LaunchedEffect(reloadDestination) {
            if (reloadDestination == SearchDestination::class) {
                if (!selectionState.isActive && searchUIType == SearchUIType.EMPTY) {
                    isExpanded = true
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                sharedViewModel.reloadDestinationDone()
            }
        }
    }

    if (showSelectionSheet) {
        val selectedIds = selectionState.selected.toList()
        SelectedSongsBottomSheet(
            count = selectedIds.size,
            onDismiss = { showSelectionSheet = false },
            onPlayNext = {
                selectionViewModel.playNext(selectedIds)
                selectionState.exit()
            },
            onAddToQueue = {
                selectionViewModel.addToQueue(selectedIds)
                selectionState.exit()
            },
            onAddToPlaylist = { showSelectionAddToPlaylist = true },
            onDownload = {
                selectionViewModel.download(selectedIds)
                selectionState.exit()
            },
            onAddToFavorite = {
                selectionViewModel.addToFavorite(selectedIds)
                selectionState.exit()
            },
        )
    }
    if (showSelectionAddToPlaylist) {
        val selectedIds = selectionState.selected.toList()
        val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = localPlaylists,
            listYouTubePlaylist = emptyList(),
            onDismiss = { showSelectionAddToPlaylist = false },
            onClick = { playlist ->
                selectionViewModel.addToPlaylist(playlist.id, selectedIds)
                selectionState.exit()
            },
            onYTPlaylistClick = {},
        )
    }
    if (showBottomSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showBottomSheet = false
                sheetSong = null
            },
            navController = navController,
            song = sheetSong,
        )
    }
    if (showRecognition) {
        SongRecognitionSheet(
            onFind = { query ->
                searchText = query
                focusManager.clearFocus()
                keyboardController?.hide()
                isSearchSubmitted = true
                searchViewModel.insertSearchHistory(query)
                searchViewModel.searchSongs(query, forceRefresh = true)
            },
            onOpenPlayer = showNowPlayingSheet,
            onDismiss = { showRecognition = false },
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Transparent),
    ) {
        // Content scrolls under the bar (it is the haze source), so it needs top padding
        // equal to the bar's measured height to keep its first item clear of it.
        Crossfade(
            targetState = searchUIType,
            modifier =
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(
                        chipBackdrop,
                        enabled = com.maxrave.simpmusic.ui.component.LocalGlassEffectsEnabled.current,
                    ),
        ) {
            when (it) {
                SearchUIType.SEARCH_SUGGESTIONS -> {
                    val (suggestItems, suggestKeys) =
                        remember(searchScreenState.suggestYTItems) {
                            val snapshot = searchScreenState.suggestYTItems
                            val seen = HashMap<String, Int>(snapshot.size)
                            val keys =
                                snapshot.map { s ->
                                    val base =
                                        when (s) {
                                            is SongsResult -> "song_${s.videoId}"
                                            is VideosResult -> "video_${s.videoId}"
                                            is ArtistsResult -> "artist_${s.browseId}"
                                            is AlbumsResult -> "album_${s.browseId}"
                                            is PlaylistsResult -> "playlist_${s.browseId}"
                                            else -> "other_${s.hashCode()}"
                                        }
                                    val count = seen[base] ?: 0
                                    seen[base] = count + 1
                                    if (count == 0) base else "$base#$count"
                                }
                            snapshot to keys
                        }
                    val (suggestQueryItems, suggestQueryKeys) =
                        remember(searchScreenState.suggestQueries) {
                            val snapshot = searchScreenState.suggestQueries
                            val seen = HashMap<String, Int>(snapshot.size)
                            val keys =
                                snapshot.map { q ->
                                    val count = seen[q] ?: 0
                                    seen[q] = count + 1
                                    if (count == 0) "q_$q" else "q_$q#$count"
                                }
                            snapshot to keys
                        }
                    LazyColumn(
                        Modifier.padding(horizontal = 16.dp),
                        state = suggestionsState,
                        contentPadding = PaddingValues(top = searchBarHeight, bottom = 10.dp),
                    ) {
                        itemsIndexed(
                            items = suggestItems,
                            key = { index, item -> suggestKeys.getOrElse(index) { "suggest_${item.hashCode()}_$index" } },
                            contentType = { _, item -> item::class.simpleName ?: "suggest_item" },
                        ) { _, item ->
                            val platformContext = coil3.compose.LocalPlatformContext.current
                            SuggestItemRow(
                                searchResult = item,
                                onItemClick = { clickedItem ->
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    when (clickedItem) {
                                        is SongsResult, is VideosResult -> {
                                            val firstTrack: Track =
                                                (clickedItem as? SongsResult)?.toTrack()
                                                    ?: (clickedItem as VideosResult).toTrack()
                                            searchViewModel.setQueueData(
                                                QueueData.Data(
                                                    listTracks = arrayListOf(firstTrack),
                                                    firstPlayedTrack = firstTrack,
                                                    playlistId = "RDAMVM${firstTrack.videoId}",
                                                    playlistName = "\"${searchText}\" ${getStringBlocking(Res.string.in_search)}",
                                                    playlistType = PlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            searchViewModel.loadMediaItem(firstTrack, type = Config.SONG_CLICK)
                                        }

                                        is ArtistsResult -> {
                                            navController.navigate(
                                                ArtistDestination(clickedItem.browseId),
                                            )
                                        }

                                        is AlbumsResult -> {
                                            com.maxrave.simpmusic.utils.primeCollectionArtworkNavigation(
                                                platformContext = platformContext,
                                                collectionId = clickedItem.browseId,
                                                rawThumbUrl = clickedItem.thumbnails.lastOrNull()?.url,
                                            )
                                            navController.navigate(
                                                AlbumDestination(clickedItem.browseId),
                                            )
                                        }

                                        is PlaylistsResult -> {
                                            com.maxrave.simpmusic.utils.primeCollectionArtworkNavigation(
                                                platformContext = platformContext,
                                                collectionId = clickedItem.browseId,
                                                rawThumbUrl = clickedItem.thumbnails.lastOrNull()?.url,
                                            )
                                            navController.navigate(
                                                PlaylistDestination(
                                                    clickedItem.browseId,
                                                ),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                        itemsIndexed(
                            items = suggestQueryItems,
                            key = { index, q -> suggestQueryKeys.getOrElse(index) { "q_${q}_$index" } },
                            contentType = { _, _ -> "suggest_query" },
                        ) { _, suggestion ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(),
                                            onClick = {
                                                searchText = suggestion
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                isSearchSubmitted = true
                                                searchViewModel.insertSearchHistory(suggestion)
                                                searchViewModel.searchByType(
                                                    query = suggestion,
                                                    type = searchScreenState.searchType,
                                                    forceRefresh = true,
                                                )
                                            },
                                        ).padding(horizontal = 12.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = suggestion,
                                    style = typo().bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {
                                        searchText = suggestion
                                        focusRequester.requestFocus()
                                    },
                                ) {
                                    Icon(
                                        imageVector = SimpIcons.ArrowOutward,
                                        contentDescription = "Search suggestion",
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                        item {
                            EndOfPage(
                                withoutCredit = true,
                            )
                        }
                    }
                    VerticalScrollBar(
                        scrollState = suggestionsState,
                        modifier = Modifier.padding(top = searchBarHeight, bottom = DesktopMiniPlayerClearance),
                    )
                }

                SearchUIType.SEARCH_HISTORY -> {
                    // Search history state
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                    ) {
                        LazyColumn(
                            state = historyState,
                            contentPadding = PaddingValues(top = searchBarHeight, bottom = 10.dp),
                        ) {
                            stickyHeader {
                                Crossfade(
                                    targetState = searchHistory.isNotEmpty(),
                                ) {
                                    if (it) {
                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.background),
                                        ) {
                                            TextButton(
                                                onClick = { searchViewModel.deleteSearchHistory() },
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.clear_search_history),
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            items(
                                items = searchHistory,
                                key = { historyItem -> "hist_$historyItem" },
                                contentType = { "search_history" },
                            ) { historyItem ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchText = historyItem
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                isSearchSubmitted = true
                                                searchViewModel.insertSearchHistory(historyItem)
                                                searchViewModel.searchByType(
                                                    query = historyItem,
                                                    type = searchScreenState.searchType,
                                                    forceRefresh = true,
                                                )
                                            }.padding(start = 12.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = SimpIcons.History,
                                        contentDescription = "Search history",
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                                    Text(
                                        text = historyItem,
                                        style = typo().bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = {
                                            searchText = historyItem
                                            focusRequester.requestFocus()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.ArrowOutward,
                                            contentDescription = "Search suggestion",
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            searchViewModel.deleteSearchHistoryItem(historyItem)
                                        },
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.Close,
                                            contentDescription = stringResource(Res.string.remove_from_search_history),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                            item {
                                EndOfPage(
                                    withoutCredit = true,
                                )
                            }
                        }
                    }
                    VerticalScrollBar(
                        scrollState = historyState,
                        modifier = Modifier.padding(top = searchBarHeight, bottom = DesktopMiniPlayerClearance),
                    )
                }

                SearchUIType.EMPTY -> {
                    val mood = moodAndGenres
                    if (mood == null) {
                        if (moodError) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(Res.string.error_occurred),
                                        style = typo().titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(onClick = { searchViewModel.retryMoodAndGenres() }) {
                                        Text(text = stringResource(Res.string.retry))
                                    }
                                }
                            }
                        } else {
                            // First run only: the repository serves its cached copy before hitting the
                            // network, so this spinner is never seen again after the first fetch.
                            CenterLoadingBox(Modifier.fillMaxSize())
                        }
                    } else {
                        // Capped and centred: on a wide desktop window the grid would otherwise
                        // span the whole width, stretching four tiles into long bars. 1100.dp
                        // keeps a tile near 250.dp, which is its natural size.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(moodGridColumns),
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 1100.dp)
                                    .padding(horizontal = 16.dp),
                            state = moodGridState,
                            contentPadding = PaddingValues(top = searchBarHeight),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            // Breathing room on both sides of this block: above it
                                            // sits the floating search bar, below it the tile grid.
                                            .padding(top = 36.dp, bottom = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.everything_you_need),
                                        style = typo().titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(Res.string.search_for_songs_artists_albums_playlists_and_more),
                                        style = typo().bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            mood.sections.forEachIndexed { index, section ->
                                // First section runs straight on from the header block above it,
                                // so its own heading would just be a second title in a row.
                                if (index > 0) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Text(
                                            // Section titles come from YouTube already localised,
                                            // so there is no string resource to pick here.
                                            text = section.title,
                                            style = typo().titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                }
                                // Key must include the section: every section lives in this ONE
                                // grid, and "For you" repeats categories that also appear under
                                // Moods or Genres, so params alone collides.
                                items(section.items, key = { "${section.title}/${it.params}" }) { item ->
                                    // LazyVerticalGrid only composes tiles inside the viewport, so
                                    // putting the request here IS the laziness — a category the
                                    // user never scrolls to never costs a browse.
                                    LaunchedEffect(item.params) {
                                        searchViewModel.loadMoodArtwork(item.params)
                                    }
                                    MoodCategoryCard(
                                        title = item.title,
                                        artworkUrl = moodArtwork[item.params],
                                    ) {
                                        navController.navigate(MoodDestination(item.params))
                                    }
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EndOfPage()
                            }
                        }
                        VerticalScrollBar(
                            scrollState = moodGridState,
                            modifier = Modifier.padding(top = searchBarHeight, bottom = DesktopMiniPlayerClearance),
                        )
                        }
                    }
                }

                SearchUIType.SEARCH_RESULTS -> {
                    // Content area — chips now live in the blurred bar block above.
                    Column(modifier = Modifier.fillMaxSize()) {
                        PullToRefreshBox(
                            modifier = Modifier.fillMaxSize(),
                            state = pullToRefreshState,
                            onRefresh = {
                                val query = searchText.trim()
                                if (query.isNotEmpty()) {
                                    isSearchSubmitted = true
                                    searchViewModel.insertSearchHistory(query)
                                    searchViewModel.searchByType(
                                        query = query,
                                        type = searchScreenState.searchType,
                                        forceRefresh = true,
                                    )
                                }
                            },
                            isRefreshing = uiState is SearchScreenUIState.Loading,
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullToRefreshState,
                                    isRefreshing = uiState is SearchScreenUIState.Loading,
                                    // Anchored to the top of the box, which now starts under the
                                    // bar — without this offset the spinner sits behind the bar
                                    // and only its top sliver shows.
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = searchBarHeight),
                                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                                    color = PullToRefreshDefaults.indicatorColor,
                                    maxDistance = PullToRefreshDefaults.PositionalThreshold - 5.dp,
                                )
                            },
                        ) {
                            Crossfade(targetState = uiState) { uiState ->
                                when (uiState) {
                                    is SearchScreenUIState.Loading -> {
                                        // Loading state — same top inset as the results list, or
                                        // the first shimmer row hides behind the bar and chips.
                                        LazyColumn(
                                            contentPadding =
                                                PaddingValues(
                                                    top = searchBarHeight,
                                                    bottom = 10.dp,
                                                ),
                                        ) {
                                            items(10) {
                                                ShimmerSearchItem()
                                            }
                                        }
                                    }

                                    is SearchScreenUIState.Success -> {
                                        // Success state with results
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            val currentResults =
                                                when (searchScreenState.searchType) {
                                                    SearchType.ALL -> searchScreenState.searchAllResult
                                                    SearchType.SONGS -> searchScreenState.searchSongsResult
                                                    SearchType.VIDEOS -> searchScreenState.searchVideosResult
                                                    SearchType.ALBUMS -> searchScreenState.searchAlbumsResult
                                                    SearchType.ARTISTS -> searchScreenState.searchArtistsResult
                                                    SearchType.PLAYLISTS -> searchScreenState.searchPlaylistsResult
                                                    SearchType.FEATURED_PLAYLISTS -> searchScreenState.searchFeaturedPlaylistsResult
                                                    SearchType.PODCASTS -> searchScreenState.searchPodcastsResult
                                                }

                                            // One batch query for the stored rows of every song/video result below.
                                            val resultSongIds =
                                                remember(currentResults) {
                                                    currentResults.mapNotNull { result ->
                                                        when (result) {
                                                            is SongsResult -> result.videoId
                                                            is VideosResult -> result.videoId
                                                            else -> null
                                                        }
                                                    }.distinct()
                                                }
                                            val localSongs by remember(resultSongIds) {
                                                if (resultSongIds.isEmpty()) flowOf(emptyList()) else songRepository.getSongsByListVideoId(resultSongIds)
                                            }.collectAsStateWithLifecycle(initialValue = emptyList())
                                            val localSongById = remember(localSongs) { localSongs.associateBy { it.videoId } }
                                            Crossfade(targetState = currentResults.isNotEmpty()) { hasResults ->
                                                if (hasResults) {
                                                    val platformContext = LocalPlatformContext.current
                                                    val renderEntry: @Composable (SearchResultType) -> Unit = { result ->
                                                        SearchResultItemRow(
                                                            result = result,
                                                            searchText = searchText,
                                                            currentVideoId = currentVideoId,
                                                            localSongById = localSongById,
                                                            selectionState = selectionState,
                                                            searchViewModel = searchViewModel,
                                                            sharedViewModel = sharedViewModel,
                                                            navController = navController,
                                                            platformContext = platformContext,
                                                            onMoreClick = onMoreClick,
                                                        )
                                                    }
                                                    if (searchScreenState.searchType == SearchType.ALL) {
                                                        val (liveVideos, regularVideos) =
                                                            remember(searchScreenState.searchVideosResult) {
                                                                searchScreenState.searchVideosResult.partition {
                                                                    it.resultType == LIVE_RESULT_TYPE
                                                                }
                                                            }
                                                        val topArtists =
                                                            remember(searchScreenState.searchArtistsResult) {
                                                                searchScreenState.searchArtistsResult.take(3)
                                                            }
                                                        val topSongs =
                                                            remember(liveVideos, searchScreenState.searchSongsResult) {
                                                                liveVideos + searchScreenState.searchSongsResult.take(6)
                                                            }
                                                        val topAlbums =
                                                            remember(searchScreenState.searchAlbumsResult) {
                                                                searchScreenState.searchAlbumsResult.take(4)
                                                            }
                                                        val topPlaylists =
                                                            remember(searchScreenState.searchPlaylistsResult) {
                                                                searchScreenState.searchPlaylistsResult.take(4)
                                                            }
                                                        val topVideos =
                                                            remember(regularVideos) {
                                                                regularVideos.take(4)
                                                            }
                                                        LazyColumn(
                                                            contentPadding =
                                                                PaddingValues(
                                                                    start = 4.dp,
                                                                    end = 4.dp,
                                                                    top = searchBarHeight,
                                                                    bottom = 10.dp,
                                                                ),
                                                            state = resultsState,
                                                        ) {
                                                            if (topArtists.isNotEmpty()) {
                                                                item(key = "sec_artists", contentType = "section_header") {
                                                                    SearchSectionHeader(
                                                                        title = stringResource(Res.string.artists),
                                                                        showSeeAll =
                                                                            searchScreenState.searchArtistsResult.size > topArtists.size ||
                                                                                searchScreenState.continuationByType[SearchType.ARTISTS] != null,
                                                                        onSeeAll = { searchViewModel.setSearchType(SearchType.ARTISTS) },
                                                                    )
                                                                }
                                                                items(
                                                                    items = topArtists,
                                                                    key = { "all_artist_${it.browseId}" },
                                                                    contentType = { "ArtistsResult" },
                                                                ) { item -> renderEntry(item) }
                                                            }
                                                            if (topSongs.isNotEmpty()) {
                                                                item(key = "sec_songs", contentType = "section_header") {
                                                                    SearchSectionHeader(
                                                                        title = stringResource(Res.string.songs),
                                                                        showSeeAll =
                                                                            searchScreenState.searchSongsResult.size > 6 ||
                                                                                searchScreenState.continuationByType[SearchType.SONGS] != null,
                                                                        onSeeAll = { searchViewModel.setSearchType(SearchType.SONGS) },
                                                                    )
                                                                }
                                                                items(
                                                                    items = topSongs,
                                                                    key = { entry ->
                                                                        when (entry) {
                                                                            is SongsResult -> "all_song_${entry.videoId}"
                                                                            is VideosResult -> "all_live_${entry.videoId}"
                                                                            else -> "all_s_${entry.hashCode()}"
                                                                        }
                                                                    },
                                                                    contentType = { it::class.simpleName ?: "song" },
                                                                ) { item -> renderEntry(item) }
                                                            }
                                                            if (topAlbums.isNotEmpty()) {
                                                                item(key = "sec_albums", contentType = "section_header") {
                                                                    SearchSectionHeader(
                                                                        title = stringResource(Res.string.albums),
                                                                        showSeeAll =
                                                                            searchScreenState.searchAlbumsResult.size > topAlbums.size ||
                                                                                searchScreenState.continuationByType[SearchType.ALBUMS] != null,
                                                                        onSeeAll = { searchViewModel.setSearchType(SearchType.ALBUMS) },
                                                                    )
                                                                }
                                                                items(
                                                                    items = topAlbums,
                                                                    key = { "all_album_${it.browseId}" },
                                                                    contentType = { "AlbumsResult" },
                                                                ) { item -> renderEntry(item) }
                                                            }
                                                            if (topPlaylists.isNotEmpty()) {
                                                                item(key = "sec_playlists", contentType = "section_header") {
                                                                    SearchSectionHeader(
                                                                        title = stringResource(Res.string.playlists),
                                                                        showSeeAll =
                                                                            searchScreenState.searchPlaylistsResult.size > topPlaylists.size ||
                                                                                searchScreenState.continuationByType[SearchType.PLAYLISTS] != null,
                                                                        onSeeAll = { searchViewModel.setSearchType(SearchType.PLAYLISTS) },
                                                                    )
                                                                }
                                                                items(
                                                                    items = topPlaylists,
                                                                    key = { "all_playlist_${it.browseId}" },
                                                                    contentType = { "PlaylistsResult" },
                                                                ) { item -> renderEntry(item) }
                                                            }
                                                            if (topVideos.isNotEmpty()) {
                                                                item(key = "sec_videos", contentType = "section_header") {
                                                                    SearchSectionHeader(
                                                                        title = stringResource(Res.string.videos),
                                                                        showSeeAll =
                                                                            regularVideos.size > topVideos.size ||
                                                                                searchScreenState.continuationByType[SearchType.VIDEOS] != null,
                                                                        onSeeAll = { searchViewModel.setSearchType(SearchType.VIDEOS) },
                                                                    )
                                                                }
                                                                items(
                                                                    items = topVideos,
                                                                    key = { "all_video_${it.videoId}" },
                                                                    contentType = { "VideosResult" },
                                                                ) { item -> renderEntry(item) }
                                                            }
                                                            item { EndOfPage(withoutCredit = true) }
                                                        }
                                                    } else {
                                                        val resultKeys =
                                                            remember(currentResults) {
                                                                val seen = HashMap<String, Int>(currentResults.size)
                                                                currentResults.map { entry ->
                                                                    val base =
                                                                        when (entry) {
                                                                            is SongsResult -> "song_${entry.videoId}"
                                                                            is VideosResult -> "video_${entry.videoId}"
                                                                            is AlbumsResult -> "album_${entry.browseId}"
                                                                            is ArtistsResult -> "artist_${entry.browseId}"
                                                                            is PlaylistsResult -> "playlist_${entry.browseId}"
                                                                            else -> "res_${entry.hashCode()}"
                                                                        }
                                                                    val count = seen[base] ?: 0
                                                                    seen[base] = count + 1
                                                                    if (count == 0) base else "$base#$count"
                                                                }
                                                            }
                                                        LazyColumn(
                                                            contentPadding =
                                                                PaddingValues(
                                                                    start = 4.dp,
                                                                    end = 4.dp,
                                                                    top = searchBarHeight,
                                                                    bottom = 10.dp,
                                                                ),
                                                            state = resultsState,
                                                        ) {
                                                            itemsIndexed(
                                                                items = currentResults,
                                                                key = { index, _ -> resultKeys[index] },
                                                                contentType = { _, entry -> entry::class.simpleName ?: "search_result" },
                                                            ) { _, result ->
                                                                renderEntry(result)
                                                            }
                                                            if (searchScreenState.isLoadingMore) {
                                                                item(key = "load_more_indicator") {
                                                                    Box(
                                                                        modifier =
                                                                            Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(vertical = 16.dp),
                                                                        contentAlignment = Alignment.Center,
                                                                    ) {
                                                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                                                    }
                                                                }
                                                            }
                                                            item { EndOfPage(withoutCredit = true) }
                                                        }
                                                    }
                                                    VerticalScrollBar(
                                                        scrollState = resultsState,
                                                        modifier = Modifier.padding(top = searchBarHeight, bottom = DesktopMiniPlayerClearance),
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.no_results_found),
                                                            style = typo().titleMedium,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.fillMaxWidth(),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    is SearchScreenUIState.Error -> {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            // Error state
                                            Column(
                                                modifier = Modifier.align(Alignment.Center),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.error_occurred),
                                                    style = typo().titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(onClick = {
                                                    searchViewModel.retryCurrentSearch(searchText)
                                                }) {
                                                    Text(text = stringResource(Res.string.retry))
                                                }
                                            }
                                        }
                                    }

                                    SearchScreenUIState.Empty -> {
                                        // Empty state
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.no_results_found),
                                                style = typo().titleMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { searchBarHeightPx = it.size.height },
        ) {
            // Animate only the background so scrolling keeps the search input and its focus.
            AnimatedVisibility(
                visible = !isContentAtTop,
                modifier = Modifier.matchParentSize(),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                label = "search_bar_scrim",
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .ultraThinBackdropBarBlur(chipBackdrop),
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(vertical = 10.dp),
            ) {
        AnimatedVisibility(visible = selectionState.isActive) {
            SongSelectionTopAppBar(
                state = selectionState,
                onSelectAll = {
                    val visible =
                        when (searchScreenState.searchType) {
                            SearchType.SONGS -> searchScreenState.searchSongsResult.map { it.videoId }
                            SearchType.VIDEOS -> searchScreenState.searchVideosResult.map { it.videoId }
                            SearchType.ALL ->
                                searchScreenState.searchAllResult.mapNotNull {
                                    (it as? SongsResult)?.videoId ?: (it as? VideosResult)?.videoId
                                }
                            else -> emptyList()
                        }
                    selectionState.toggleSelectAll(visible)
                },
                onOpenActions = { showSelectionSheet = true },
                containerColor = Color.Transparent,
                // Zero here AND on the SearchBar below: the Column that holds them both consumes
                // the status bar once, for the whole stack. Leaving it on either child reserves it
                // a second time — which is the slab of padding this screen used to show.
                windowInsets = WindowInsets(0),
            )
        }
        // Search Bar with Animated Placeholder
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchText,
                    onQueryChange = { newText ->
                        searchText = newText
                    },
                    onSearch = { query ->
                        // A pasted YouTube link is a destination, not a query. Translating it into
                        // the app's own deep link hands it to the same intent flow that handles
                        // shared links, so it plays or opens straight away instead of being
                        // searched for as text. Anything else falls through to a normal search.
                        val deepLink = query.toAppDeepLinkOrNull()
                        if (deepLink != null) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            sharedViewModel.setIntent(GenericIntent(data = deepLink))
                        } else if (query.trim().isNotEmpty()) {
                            val clean = query.trim()
                            isSearchSubmitted = true
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            searchViewModel.insertSearchHistory(clean)
                            searchViewModel.searchByType(
                                query = clean,
                                type = searchScreenState.searchType,
                                forceRefresh = true,
                            )
                        }
                    },
                    expanded = false,
                    onExpandedChange = {},
                    enabled = true,
                    placeholder = {
                        // Animated placeholder text
                        AnimatedContent(
                            targetState = currentPlaceholderIndex,
                            transitionSpec = {
                                (
                                    fadeIn(animationSpec = tween(500)) +
                                        slideInVertically { height -> height }
                                ).togetherWith(
                                    fadeOut(animationSpec = tween(500)) +
                                        slideOutVertically { height -> -height },
                                )
                            },
                            label = "placeholder_animation",
                        ) { index ->
                            Text(
                                text = placeholderTexts[index],
                                style = typo().labelMedium,
                            )
                        }
                    },
                    leadingIcon = {
                        Crossfade(
                            targetState = isSearchPanelVisible || isResultsVisible,
                            modifier = Modifier.size(48.dp),
                            animationSpec = tween(200),
                            label = "search_back_icon",
                        ) { showBack ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showBack) {
                                    IconButton(
                                        onClick = {
                                            if (isSearchPanelVisible) dismissSearchPanel() else backToSearchHome()
                                        },
                                        enabled = isSearchPanelVisible || isResultsVisible,
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.ArrowBackIosNew,
                                            contentDescription = "Back",
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = SimpIcons.Search,
                                        contentDescription = "Search",
                                    )
                                }
                            }
                        }
                    },
                    trailingIcon = {
                        // X button only shows when there's text
                        if (searchText.isNotEmpty()) {
                            IconButton(
                                modifier = Modifier.clip(CircleShape),
                                onClick = {
                                    searchText = ""
                                    isSearchSubmitted = false
                                    searchViewModel.clearSuggestions()
                                    if (isResultsVisible) {
                                        searchViewModel.resetToEmpty()
                                        isExpanded = true
                                        focusRequester.requestFocus()
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = SimpIcons.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        } else if (getPlatform() == Platform.Android) {
                            // An empty field offers song recognition instead (Android only).
                            IconButton(
                                modifier = Modifier.clip(CircleShape),
                                onClick = { showRecognition = true },
                            ) {
                                Icon(
                                    imageVector = SimpIcons.GraphicEq,
                                    contentDescription = stringResource(Res.string.recognize_song),
                                )
                            }
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }.padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            // See the note on SongSelectionTopAppBar above — the Column owns the status-bar inset.
            windowInsets = WindowInsets(0),
            content = {},
        )
                // Filter chips ride along inside the blurred block instead of sitting in the
                // results branch. That way searchBarHeight covers them too, results scroll
                // underneath the whole thing, and the glass has something to blur.
                AnimatedVisibility(visible = searchUIType == SearchUIType.SEARCH_RESULTS) {
                    LazyRow(
                        state = chipRowState,
                        modifier = Modifier.padding(top = 10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = SearchType.entries,
                            key = { it.name },
                        ) { id ->
                            val isSelected = id == searchScreenState.searchType
                            Chip(
                                isAnimated = uiState is SearchScreenUIState.Loading,
                                isSelected = isSelected,
                                text = stringResource(id.toStringRes()),
                                backdrop = chipBackdrop,
                            ) {
                                searchViewModel.setSearchType(id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestItemRow(
    searchResult: SearchResultType,
    onItemClick: (SearchResultType) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onItemClick(searchResult) }
                .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val url =
            when (searchResult) {
                is SongsResult -> {
                    searchResult.thumbnails?.lastOrNull()?.url
                }

                is AlbumsResult -> {
                    searchResult.thumbnails.lastOrNull()?.url
                }

                is ArtistsResult -> {
                    searchResult.thumbnails.lastOrNull()?.url
                }

                is PlaylistsResult -> {
                    searchResult.thumbnails.lastOrNull()?.url
                }

                is VideosResult -> {
                    searchResult.thumbnails?.lastOrNull()?.url
                }

                else -> {
                    null
                }
            }

        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp)),
        ) {
            val searchThumbRequest =
                com.maxrave.simpmusic.utils.rememberOptimizedImageRequest(
                    rawUrl = url,
                    targetPx = com.maxrave.simpmusic.utils.THUMB_SMALL_PX,
                    crossfadeMs = com.maxrave.simpmusic.utils.IMAGE_CROSSFADE_FAST_MS,
                )
            AsyncImage(
                model = searchThumbRequest,
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(
                            if (searchResult is ArtistsResult) {
                                CircleShape
                            } else {
                                ArtworkShape
                            },
                        ),
            )
        }

        Spacer(modifier = Modifier.padding(horizontal = 12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val title =
                when (searchResult) {
                    is SongsResult -> {
                        searchResult.title
                    }

                    is AlbumsResult -> {
                        searchResult.title
                    }

                    is ArtistsResult -> {
                        searchResult.artist
                    }

                    is PlaylistsResult -> {
                        searchResult.title
                    }

                    is VideosResult -> {
                        searchResult.title
                    }

                    else -> {
                        null
                    }
                } ?: "Unknown"

            Text(
                text = title,
                style = typo().labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))

            val subtitle =
                when (searchResult) {
                    is SongsResult -> searchResult.artists?.map { it.name }?.connectArtists()
                    is AlbumsResult -> searchResult.artists.map { it.name }.connectArtists()
                    is PlaylistsResult -> searchResult.author.ifEmpty { "YouTube Music" }
                    is ArtistsResult -> stringResource(Res.string.artists)
                    is VideosResult -> searchResult.artists?.map { it.name }?.connectArtists()
                    else -> null
                } ?: "Unknown"

            val isLiveSuggestion =
                (searchResult as? SongsResult)?.resultType == com.maxrave.domain.utils.LIVE_RESULT_TYPE ||
                    (searchResult as? VideosResult)?.resultType == com.maxrave.domain.utils.LIVE_RESULT_TYPE
            if (isLiveSuggestion || subtitle.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLiveSuggestion) {
                        com.maxrave.simpmusic.ui.component.LiveBadge(modifier = Modifier.padding(end = 6.dp))
                    }
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    showSeeAll: Boolean,
    onSeeAll: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = typo().titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (showSeeAll) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = stringResource(Res.string.see_all),
                    style = typo().labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SearchResultItemRow(
    result: SearchResultType,
    searchText: String,
    currentVideoId: String?,
    localSongById: Map<String, SongEntity>,
    selectionState: com.maxrave.simpmusic.ui.component.selection.SongSelectionState,
    searchViewModel: SearchViewModel,
    sharedViewModel: SharedViewModel,
    navController: NavController,
    platformContext: coil3.PlatformContext,
    onMoreClick: (SongEntity) -> Unit,
) {
    when (result) {
        is SongsResult -> {
            SongFullWidthItems(
                track = result.toTrack(),
                localSongOverride = localSongById[result.videoId],
                hasLocalSongOverride = true,
                isPlaying = result.videoId == currentVideoId,
                modifier = Modifier,
                onMoreClickListener = {
                    onMoreClick(result.toTrack().toSongEntity())
                },
                onClickListener = {
                    val firstTrack = result.toTrack()
                    searchViewModel.setQueueData(
                        QueueData.Data(
                            listTracks = arrayListOf(firstTrack),
                            firstPlayedTrack = firstTrack,
                            playlistId = "RDAMVM${result.videoId}",
                            playlistName = "\"${searchText}\" ${getStringBlocking(Res.string.in_search)}",
                            playlistType = PlaylistType.RADIO,
                            continuation = null,
                        ),
                    )
                    searchViewModel.loadMediaItem(firstTrack, Config.SONG_CLICK)
                },
                onAddToQueue = {
                    sharedViewModel.addListToQueue(
                        arrayListOf(result.toTrack()),
                    )
                },
                selectionMode = selectionState.isActive,
                isSelected = selectionState.isSelected(result.videoId),
                onLongClick = { selectionState.start(it) },
                onSelectToggle = { selectionState.toggle(it) },
            )
        }

        is VideosResult -> {
            SongFullWidthItems(
                track = result.toTrack(),
                localSongOverride = localSongById[result.videoId],
                hasLocalSongOverride = true,
                isPlaying = result.videoId == currentVideoId,
                modifier = Modifier,
                onMoreClickListener = {
                    onMoreClick(result.toTrack().toSongEntity())
                },
                onClickListener = {
                    val firstTrack = result.toTrack()
                    searchViewModel.setQueueData(
                        QueueData.Data(
                            listTracks = arrayListOf(firstTrack),
                            firstPlayedTrack = firstTrack,
                            playlistId = "RDAMVM${result.videoId}",
                            playlistName = "\"${searchText}\" ${getStringBlocking(Res.string.in_search)}",
                            playlistType = PlaylistType.RADIO,
                            continuation = null,
                        ),
                    )
                    searchViewModel.loadMediaItem(firstTrack, Config.VIDEO_CLICK)
                },
                onAddToQueue = {
                    sharedViewModel.addListToQueue(
                        arrayListOf(result.toTrack()),
                    )
                },
                selectionMode = selectionState.isActive,
                isSelected = selectionState.isSelected(result.videoId),
                onLongClick = { selectionState.start(it) },
                onSelectToggle = { selectionState.toggle(it) },
            )
        }

        is AlbumsResult -> {
            PlaylistFullWidthItems(
                data = result,
                onClickListener = {
                    com.maxrave.simpmusic.utils.primeCollectionArtworkNavigation(
                        platformContext = platformContext,
                        collectionId = result.browseId,
                        rawThumbUrl = result.thumbnails.lastOrNull()?.url,
                    )
                    navController.navigate(
                        AlbumDestination(
                            result.browseId,
                        ),
                    )
                },
            )
        }

        is ArtistsResult -> {
            ArtistFullWidthItems(
                data = result,
                onClickListener = {
                    navController.navigate(
                        ArtistDestination(
                            result.browseId,
                        ),
                    )
                },
            )
        }

        is PlaylistsResult -> {
            PlaylistFullWidthItems(
                data = result,
                onClickListener = {
                    if (result.resultType == "Podcast") {
                        navController.navigate(
                            PodcastDestination(
                                result.browseId,
                            ),
                        )
                    } else {
                        com.maxrave.simpmusic.utils.primeCollectionArtworkNavigation(
                            platformContext = platformContext,
                            collectionId = result.browseId,
                            rawThumbUrl = result.thumbnails.lastOrNull()?.url,
                        )
                        navController.navigate(
                            PlaylistDestination(
                                result.browseId,
                            ),
                        )
                    }
                },
            )
        }
    }
}

enum class SearchUIType {
    EMPTY,
    SEARCH_HISTORY,
    SEARCH_SUGGESTIONS,
    SEARCH_RESULTS,
}
