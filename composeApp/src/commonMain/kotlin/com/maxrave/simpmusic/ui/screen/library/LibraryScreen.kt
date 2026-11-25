package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.model.taste.TasteProfile
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DesktopMiniPlayerClearance
import com.maxrave.simpmusic.expect.ui.VerticalScrollBar
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.extension.glassHazeSource
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.extension.ultraThinBarBlur
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.CreateLocalPlaylistSheet
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.LibraryTilingBox
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.component.PlaylistFullWidthItems
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.taste.ShareTasteSheet
import com.maxrave.simpmusic.ui.component.taste.TasteCard
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.icon.PeopleAlt
import com.maxrave.simpmusic.ui.icon.Refresh
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryHistoryDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.LocalLiquidGlassEnabled
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.primeCollectionArtworkNavigation
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.TasteUiState
import com.maxrave.simpmusic.viewModel.TasteViewModel
import com.maxrave.simpmusic.viewModel.deduplicateYouTubePlaylistsWithoutLiked
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.downloaded_playlists
import simpmusic.composeapp.generated.resources.favorite_playlists
import simpmusic.composeapp.generated.resources.favorite_podcasts
import simpmusic.composeapp.generated.resources.library
import simpmusic.composeapp.generated.resources.mix_for_you
import simpmusic.composeapp.generated.resources.no_charts_found
import simpmusic.composeapp.generated.resources.no_favorite_playlists
import simpmusic.composeapp.generated.resources.no_favorite_podcasts
import simpmusic.composeapp.generated.resources.no_playlists_added
import simpmusic.composeapp.generated.resources.no_playlists_downloaded
import simpmusic.composeapp.generated.resources.simpmusic_charts
import simpmusic.composeapp.generated.resources.wrapped
import simpmusic.composeapp.generated.resources.your_library
import simpmusic.composeapp.generated.resources.your_playlists
import simpmusic.composeapp.generated.resources.your_youtube_playlists
import simpmusic.composeapp.generated.resources.youtube_liked_music

private const val LIKED_MUSIC_THUMBNAIL_URL =
    "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-songs-delhi-1200.png"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val density = LocalDensity.current

    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    // Wrapped and its recaps are built entirely from playback_event, so the chip follows the same
    // setting the Analytics tab does.
    val localTrackingEnabled by viewModel.localTrackingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val monthlyRecaps by viewModel.monthlyRecaps.collectAsStateWithLifecycle()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsStateWithLifecycle()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val chartPlaylists by viewModel.chartPlaylists.collectAsStateWithLifecycle()
    val libraryOverview by viewModel.libraryOverview.collectAsStateWithLifecycle()
    val cachedPreview by viewModel.cachedPreview.collectAsStateWithLifecycle()
    // The player cache is no table to observe, so the Cached card is read again each time the
    // screen is shown — coming back from the list after removing a song included.
    if (getPlatform() == Platform.Android) {
        LaunchedEffect(Unit) { viewModel.refreshCachedPreview() }
    }

    val tasteViewModel: TasteViewModel = koinViewModel()
    // Held here rather than inside the list item, so the sheet does not close if its card scrolls
    // out of composition behind it.
    var sharingTaste by remember { mutableStateOf<TasteProfile?>(null) }
    val accountThumbnail by viewModel.accountThumbnail.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()

    var topAppBarHeight by remember {
        mutableStateOf(0.dp)
    }
    var showAddSheet by remember { mutableStateOf(false) }

    val chipRowState = rememberScrollState()
    val currentFilter by viewModel.currentScreen.collectAsStateWithLifecycle()
    // A chip asked for by a deep link (simpmusic://library?tab=…), applied once and cleared.
    val libraryTabRequest by sharedViewModel.libraryTabRequest.collectAsStateWithLifecycle()
    LaunchedEffect(libraryTabRequest) {
        libraryTabRequest?.let {
            val target =
                if (it == LibraryChipType.YOUTUBE_MUSIC_PLAYLIST || it == LibraryChipType.YOUTUBE_MIX_FOR_YOU) {
                    LibraryChipType.YOUR_LIBRARY
                } else {
                    it
                }
            viewModel.setCurrentScreen(target)
            sharedViewModel.requestLibraryTab(null)
        }
    }

    LaunchedEffect(currentFilter, loggedIn) {
        when (currentFilter) {
            // Your YouTube Playlists now lives directly inside Your library, and Mix for you has
            // its own nav tab. Redirect persisted or deep-linked selections back to YOUR_LIBRARY.
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST,
            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> {
                viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY)
            }

            LibraryChipType.YOUR_LIBRARY -> {
                if (loggedIn) {
                    if (youTubePlaylist.data.isNullOrEmpty()) {
                        viewModel.getYouTubePlaylist()
                    }
                } else {
                    viewModel.clearYouTubePlaylist()
                }
            }

            LibraryChipType.LOCAL_PLAYLIST -> {
                viewModel.getLocalPlaylist()
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                viewModel.getPlaylistFavorite()
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                viewModel.getDownloadedPlaylist()
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                viewModel.getFavoritePodcasts()
            }

            LibraryChipType.CHART -> {
                if (chartPlaylists.data.isNullOrEmpty()) {
                    viewModel.getChartPlaylists()
                }
            }

            LibraryChipType.WRAPPED -> {
                viewModel.getMonthlyRecaps()
            }
        }
    }

    // The floating chip row refracts the page scrolling under it, so the page is the glass source and
    // the row its sibling (nesting the row inside the source is the RuntimeShader feedback loop).
    val chipBackdrop = rememberBackdrop(MaterialTheme.colorScheme.background)
    Crossfade(
        modifier = Modifier.then(if (LocalLiquidGlassEnabled.current) Modifier.layerBackdrop(chipBackdrop) else Modifier).glassHazeSource(hazeState),
        targetState = currentFilter,
    ) { filter ->
        when (filter) {
            LibraryChipType.YOUR_LIBRARY -> {
                val state = rememberLazyListState()
                val isScrollingUpState = state.isScrollingUp()
                LaunchedEffect(state) {
                    snapshotFlow { state.firstVisibleItemIndex <= 1 || isScrollingUpState.value }
                        .distinctUntilChanged()
                        .collect { onScrolling.invoke(it) }
                }

                val pullToRefreshState = rememberPullToRefreshState()
                var userRefreshing by remember { mutableStateOf(false) }
                val rawPlaylists = youTubePlaylist.data ?: emptyList()
                var lastLoadedPlaylists by remember { mutableStateOf(rawPlaylists) }
                LaunchedEffect(youTubePlaylist) {
                    when (youTubePlaylist) {
                        is LocalResource.Loading -> Unit
                        is LocalResource.Success -> {
                            userRefreshing = false
                            lastLoadedPlaylists = youTubePlaylist.data ?: emptyList()
                        }
                        is LocalResource.Error -> {
                            userRefreshing = false
                        }
                    }
                }
                val visibleYouTubePlaylists =
                    remember(youTubePlaylist, userRefreshing, lastLoadedPlaylists) {
                        val base =
                            if (youTubePlaylist is LocalResource.Loading && userRefreshing) {
                                lastLoadedPlaylists
                            } else {
                                youTubePlaylist.data ?: emptyList()
                            }
                        deduplicateYouTubePlaylistsWithoutLiked(base)
                    }
                val showTopIndicator = userRefreshing && youTubePlaylist is LocalResource.Loading

                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    state = pullToRefreshState,
                    onRefresh = {
                        userRefreshing = true
                        if (getPlatform() == Platform.Android) {
                            viewModel.refreshCachedPreview()
                        }
                        if (loggedIn) {
                            viewModel.getYouTubePlaylist()
                        } else {
                            userRefreshing = false
                        }
                    },
                    isRefreshing = showTopIndicator,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = showTopIndicator,
                            modifier =
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = topAppBarHeight),
                            containerColor = PullToRefreshDefaults.indicatorContainerColor,
                            color = PullToRefreshDefaults.indicatorColor,
                            maxDistance = PullToRefreshDefaults.PositionalThreshold,
                        )
                    },
                ) {
                    // Desktop windows run wide; this tab keeps to the floating capsule player's width
                    // so its cards line up with the capsule instead of stretching edge to edge. Done
                    // as content padding rather than by narrowing the list, so the side gutters stay
                    // part of it and the wheel still scrolls there.
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val layoutDirection = LocalLayoutDirection.current
                        val innerStart = innerPadding.calculateStartPadding(layoutDirection)
                        val innerEnd = innerPadding.calculateEndPadding(layoutDirection)
                        val sideInset =
                            if (getPlatform() == Platform.Desktop) {
                                ((maxWidth - innerStart - innerEnd - YOUR_LIBRARY_MAX_WIDTH) / 2).coerceAtLeast(0.dp)
                            } else {
                                0.dp
                            }
                        val platformContext = LocalPlatformContext.current
                        val likedMusicTitle = stringResource(Res.string.youtube_liked_music)
                        val likedPlaylist =
                            remember(likedMusicTitle) {
                                PlaylistEntity(
                                    title = likedMusicTitle,
                                    author = "YouTube Music",
                                    id = "LM",
                                    description = "PIN",
                                    thumbnails = LIKED_MUSIC_THUMBNAIL_URL,
                                )
                            }

                        LazyColumn(
                            contentPadding =
                                innerPadding.copy(
                                    start = innerStart + sideInset,
                                    top = topAppBarHeight,
                                    end = innerEnd + sideInset,
                                ),
                            state = state,
                        ) {
                            item {
                                LibraryTilingBox(navController, libraryOverview, cachedPreview)
                            }

                            // The reading is drawn from playback_event, so the card follows the same
                            // setting the Wrapped chip and the Analytics tab do.
                            if (localTrackingEnabled) {
                                item(key = "taste") {
                                    val tasteState by tasteViewModel.uiState.collectAsStateWithLifecycle()
                                    if (tasteState != TasteUiState.Hidden) {
                                        TasteCard(
                                            state = tasteState,
                                            onGenerate = tasteViewModel::generate,
                                            onOpenSettings = { navController.navigate(SettingsDestination) },
                                            onShare = { sharingTaste = it },
                                            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                                        )
                                    }
                                }
                            }

                            if (loggedIn) {
                                item(key = "youtube_playlists_header", contentType = "library_section_header") {
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp, bottom = 4.dp, start = 16.dp, end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.your_youtube_playlists),
                                            style = typo().headlineMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f),
                                        )
                                        RippleIconButton(
                                            imageVector = SimpIcons.Refresh,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            viewModel.getYouTubePlaylist()
                                        }
                                    }
                                }

                                item(key = "yt_liked_music_pinned", contentType = "youtube_playlist_row") {
                                    PlaylistFullWidthItems(
                                        data = likedPlaylist,
                                        onClickListener = {
                                            primeCollectionArtworkNavigation(
                                                platformContext = platformContext,
                                                collectionId = likedPlaylist.id,
                                                rawThumbUrl = likedPlaylist.thumbnails,
                                            )
                                            navController.navigate(PlaylistDestination(likedPlaylist.id))
                                        },
                                    )
                                }

                                if (youTubePlaylist is LocalResource.Loading && visibleYouTubePlaylists.isEmpty()) {
                                    item(key = "yt_playlists_loading") {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(96.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CenterLoadingBox(Modifier.size(32.dp))
                                        }
                                    }
                                } else {
                                    itemsIndexed(
                                        items = visibleYouTubePlaylists,
                                        key = { index, item -> "ytpl_${item.browseId}_$index" },
                                        contentType = { _, _ -> "youtube_playlist_row" },
                                    ) { _, playlist ->
                                        PlaylistFullWidthItems(
                                            data = playlist,
                                            onClickListener = {
                                                primeCollectionArtworkNavigation(
                                                    platformContext = platformContext,
                                                    collectionId = playlist.browseId,
                                                    rawThumbUrl = playlist.thumbnails.lastOrNull()?.url,
                                                )
                                                navController.navigate(
                                                    PlaylistDestination(
                                                        playlistId = playlist.browseId,
                                                        isYourYouTubePlaylist = true,
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                }
                            }

                            item {
                                EndOfPage()
                            }
                        }
                        VerticalScrollBar(
                            scrollState = state,
                            modifier = Modifier.padding(top = topAppBarHeight, bottom = DesktopMiniPlayerClearance),
                        )
                    }
                }
            }

            // Nothing to draw: YouTube playlists live in YOUR_LIBRARY and MixForYouScreen owns Mix for you.
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST,
            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> Unit

            LibraryChipType.LOCAL_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    yourLocalPlaylist,
                    onScrolling = onScrolling,
                    emptyText = Res.string.no_playlists_added,
                    createNewPlaylist = {
                        showAddSheet = true
                    },
                ) {
                    viewModel.getLocalPlaylist()
                }
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePlaylist,
                    emptyText = Res.string.no_favorite_playlists,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getPlaylistFavorite()
                }
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    downloadedPlaylist,
                    emptyText = Res.string.no_playlists_downloaded,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getDownloadedPlaylist()
                }
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePodcasts,
                    emptyText = Res.string.no_favorite_podcasts,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getFavoritePodcasts()
                }
            }

            LibraryChipType.CHART -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    chartPlaylists,
                    emptyText = Res.string.no_charts_found,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getChartPlaylists()
                }
            }

            LibraryChipType.WRAPPED -> {
                LibraryWrappedTab(
                    navController = navController,
                    contentPadding = innerPadding.copy(top = topAppBarHeight),
                    recaps = monthlyRecaps,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getMonthlyRecaps()
                }
            }
        }
    }
    sharingTaste?.let { profile ->
        ShareTasteSheet(profile = profile, onDismiss = { sharingTaste = null })
    }
    if (showAddSheet) {
        CreateLocalPlaylistSheet(
            onDismiss = { showAddSheet = false },
            onCreate = viewModel::createPlaylist,
        )
    }
    Column(
        Modifier
            .background(Color.Transparent)
            .ultraThinBarBlur(hazeState).onGloballyPositioned { coordinates ->
                topAppBarHeight = with(density) { coordinates.size.height.toDp() }
            },
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.library),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            navigationIcon = {
                AnimatedVisibility(
                    !accountThumbnail.isNullOrEmpty(),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    val accountRequest =
                        com.maxrave.simpmusic.utils.rememberOptimizedImageRequest(
                            rawUrl = accountThumbnail,
                            targetPx = com.maxrave.simpmusic.utils.THUMB_SMALL_PX,
                            crossfadeMs = com.maxrave.simpmusic.utils.IMAGE_CROSSFADE_FAST_MS,
                        )
                    AsyncImage(
                        model = accountRequest,
                        placeholder = rememberVectorPainter(SimpIcons.PeopleAlt),
                        error = rememberVectorPainter(SimpIcons.PeopleAlt),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape),
                    )
                }
            },
            actions = {
                RippleIconButton(
                    imageVector = SimpIcons.History,
                    tint = MaterialTheme.colorScheme.onBackground,
                    onClick = { navController.navigate(LibraryHistoryDestination) },
                )
                ListenTogetherIconButton { navController.navigate(ListenTogetherDestination) }
            },
        )
        Row(
            modifier =
                Modifier
                    .horizontalScroll(chipRowState)
                    .padding(horizontal = 15.dp)
                    .padding(bottom = 8.dp)
                    .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LibraryChipType.entries.forEach { type ->
                // Your YouTube Playlists now lives in YOUR_LIBRARY, and Mix for you has its own tab.
                if (type == LibraryChipType.YOUTUBE_MIX_FOR_YOU ||
                    type == LibraryChipType.YOUTUBE_MUSIC_PLAYLIST
                ) {
                    return@forEach
                }
                // Nothing to recap without the plays — gated consistently with Analytics.
                if (type == LibraryChipType.WRAPPED && !localTrackingEnabled) {
                    return@forEach
                }
                Chip(
                    isAnimated = false,
                    isSelected = type == currentFilter,
                    text =
                        when (type) {
                            LibraryChipType.YOUR_LIBRARY -> stringResource(Res.string.your_library)
                            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> stringResource(Res.string.your_youtube_playlists)
                            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> stringResource(Res.string.mix_for_you)
                            LibraryChipType.LOCAL_PLAYLIST -> stringResource(Res.string.your_playlists)
                            LibraryChipType.FAVORITE_PLAYLIST -> stringResource(Res.string.favorite_playlists)
                            LibraryChipType.DOWNLOADED_PLAYLIST -> stringResource(Res.string.downloaded_playlists)
                            LibraryChipType.FAVORITE_PODCAST -> stringResource(Res.string.favorite_podcasts)
                            LibraryChipType.CHART -> stringResource(Res.string.simpmusic_charts)
                            LibraryChipType.WRAPPED -> stringResource(Res.string.wrapped)
                        },
                    backdrop = chipBackdrop,
                ) {
                    viewModel.setCurrentScreen(type)
                }
            }
        }
    }
}

/**
 * How wide the Your Library tab may get on Desktop: the floating capsule player's own width plus
 * the 10dp gutter every row of this list already carries, so the cards' edges land on the
 * capsule's. The capsule has no width constant of its own — `MiniPlayer`'s Desktop row sums to
 * 16 + 200 (controls) + 29 (divider) + 300 (track) + 29 (divider) + 232 (heart cell 40 + four
 * 48dp icon buttons) + 16 = 822dp — so this must follow if that row changes.
 */
internal val YOUR_LIBRARY_MAX_WIDTH = 842.dp
