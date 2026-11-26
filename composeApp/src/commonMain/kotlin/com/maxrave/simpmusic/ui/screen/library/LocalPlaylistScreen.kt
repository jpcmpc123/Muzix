package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import com.maxrave.simpmusic.expect.ui.DesktopMiniPlayerClearance
import com.maxrave.simpmusic.expect.ui.VerticalScrollBar
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.ui.component.DownloadingIndicator
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Close
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.kyant.backdrop.highlight.Highlight
import com.kmpalette.rememberPaletteState
import com.maxrave.common.LOCAL_PLAYLIST_ID
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PairSongLocalPlaylist
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.FilterState
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.displayNameRes
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.rememberPaletteColorState
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.AddSongToPlaylistRow
import com.maxrave.simpmusic.ui.component.AddSongToPlaylistSheet
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.component.DraggableItem
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.LoadingDialog
import com.maxrave.simpmusic.ui.component.LocalPlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistSuggestionsSection
import com.maxrave.simpmusic.ui.component.RelatedPlaylistsSection
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionAction
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.component.SortPlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.SuggestItems
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.component.painterPlaylistThumbnail
import com.maxrave.simpmusic.ui.component.playlistTitleGradient
import com.maxrave.simpmusic.ui.component.rememberDragDropState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Sort
import com.maxrave.simpmusic.ui.icon.TipsAndUpdates
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.primeCollectionArtworkNavigation
import com.maxrave.simpmusic.viewModel.LocalPlaylistState
import com.maxrave.simpmusic.viewModel.LocalPlaylistUIEvent
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import com.maxrave.simpmusic.extension.glassHazeSource
import com.maxrave.simpmusic.extension.tintedBarBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album_length
import simpmusic.composeapp.generated.resources.baseline_downloaded
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.change_order
import simpmusic.composeapp.generated.resources.created_at
import simpmusic.composeapp.generated.resources.done
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.playlist_no_tracks_yet
import simpmusic.composeapp.generated.resources.reload
import simpmusic.composeapp.generated.resources.remove_from_playlist
import simpmusic.composeapp.generated.resources.sort_by
import simpmusic.composeapp.generated.resources.suggest
import simpmusic.composeapp.generated.resources.sync_playlist_warning
import simpmusic.composeapp.generated.resources.synced_playlist_cannot_change_order
import simpmusic.composeapp.generated.resources.unsync_playlist_warning
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.yes
import simpmusic.composeapp.generated.resources.your_playlist
import simpmusic.composeapp.generated.resources.search

private const val TAG = "LocalPlaylistScreen"

@ExperimentalFoundationApi
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalCoroutinesApi::class,
)
@Composable
fun LocalPlaylistScreen(
    id: Long,
    sharedViewModel: SharedViewModel = koinInject(),
    viewModel: LocalPlaylistViewModel = koinViewModel(),
    navController: NavController,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    val downloadState by viewModel.uiState.map { it.downloadState }.collectAsStateWithLifecycle(
        initialValue = DownloadState.STATE_NOT_DOWNLOADED,
    )
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestions by rememberSaveable { mutableStateOf(true) }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestButton by rememberSaveable { mutableStateOf(false) }
    var showAddSongSheet by rememberSaveable { mutableStateOf(false) }

    val playingTrack by sharedViewModel.nowPlayingState
        .mapLatest {
            it?.songEntity
        }.collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsStateWithLifecycle(initialValue = false)

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val suggestedTracks by viewModel.uiState.map { it.suggestions?.songs ?: emptyList() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val suggestionsReloadParams by viewModel.uiState.map { it.suggestions?.reloadParams }.collectAsStateWithLifecycle(initialValue = null)
    val suggestionsLoading by viewModel.loading.collectAsStateWithLifecycle()
    val relatedPlaylists by viewModel.relatedPlaylists.collectAsStateWithLifecycle()
    val addingSuggestionIds by viewModel.addingSuggestionIds.collectAsStateWithLifecycle()
    var showSyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showUnsyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var firstTimeGetLocalPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var currentItem by remember {
        mutableStateOf<SongEntity?>(null)
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }

    var sortBottomSheetShow by remember {
        mutableStateOf(false)
    }

    var changingOrder by remember {
        mutableStateOf(false)
    }

    val trackPagingItems: LazyPagingItems<Pair<SongEntity, PairSongLocalPlaylist>> = viewModel.tracksPagingState.collectAsLazyPagingItems()
    LaunchedEffect(Unit) {
        snapshotFlow {
            trackPagingItems.loadState
        }.collectLatest {
            Logger.d("PlaylistScreen", "loadState: ${trackPagingItems.loadState}")
            viewModel.setLazyTrackPagingItems(trackPagingItems)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            firstTimeGetLocalPlaylist = true
        }
    }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        viewModel.onUIEvent(
            LocalPlaylistUIEvent.ItemClick(
                videoId = videoId,
            ),
        )
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = trackPagingItems.itemSnapshotList.findLast { it?.first?.videoId == videoId }?.first
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = shouldShowSuggestions, key2 = uiState.ytPlaylistId, key3 = uiState.syncState) {
        val ytId = uiState.ytPlaylistId
        if (!ytId.isNullOrEmpty() && uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
            if (relatedPlaylists.isEmpty() || (shouldShowSuggestions && suggestedTracks.isEmpty())) {
                viewModel.loadYouTubeExtras(ytId, includeSuggestions = shouldShowSuggestions)
            }
        }
    }

    LaunchedEffect(key1 = id) {
        if (id != uiState.id) {
            Logger.w("PlaylistScreen", "new id: $id")
            viewModel.setOffset(0)
            viewModel.removeListSuggestion()
            viewModel.updatePlaylistState(id, true)
            delay(100)
            firstTimeGetLocalPlaylist = true
        }
    }
    LaunchedEffect(key1 = uiState) {
        shouldShowSuggestButton =
            !uiState.ytPlaylistId.isNullOrEmpty() &&
            uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val paletteState = rememberPaletteState()
    val hazeState = rememberHazeState()
    var bitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    // Track which thumbnail we've already extracted a palette from.
    // Prevents palette flash when LazyColumn recycles the header item on scroll —
    // AsyncImage re-mount fires onSuccess again, but we skip the regenerate.
    var paletteGeneratedFor by remember {
        mutableStateOf<String?>(null)
    }
    val currentThumbnail = uiState.thumbnail

    var heroPrepDone by remember(id) { mutableStateOf(false) }
    var heroVisualReady by remember(id) { mutableStateOf(false) }
    val hintThumbUrl = remember(id) { com.maxrave.simpmusic.utils.ArtworkHintCache.get(id.toString()) }
    val platformContext = coil3.compose.LocalPlatformContext.current

    LaunchedEffect(heroVisualReady, uiState.id, uiState.loadState, trackPagingItems.itemCount, platformContext) {
        if (!heroVisualReady || uiState.id != id || uiState.loadState != LocalPlaylistState.PlaylistLoadState.Success) {
            return@LaunchedEffect
        }
        if (trackPagingItems.itemCount <= 0) return@LaunchedEffect
        val firstUrls =
            (0 until minOf(trackPagingItems.itemCount, 8)).map { idx ->
                trackPagingItems.peek(idx)?.first?.thumbnails
            }
        com.maxrave.simpmusic.utils.enqueueTrackArtworkPrefetch(
            platformContext = platformContext,
            rawUrls = firstUrls,
        )
    }

    LaunchedEffect(uiState.id, uiState.loadState, uiState.thumbnail, platformContext) {
        if (uiState.id != id || uiState.loadState != LocalPlaylistState.PlaylistLoadState.Success) {
            if (uiState.id != id || uiState.loadState == LocalPlaylistState.PlaylistLoadState.Loading) {
                heroPrepDone = false
                heroVisualReady = false
            }
            return@LaunchedEffect
        }
        val thumbUrl = uiState.thumbnail
        if (thumbUrl.isNullOrBlank()) {
            heroPrepDone = true
            heroVisualReady = true
            return@LaunchedEffect
        }
        if (paletteGeneratedFor != thumbUrl) {
            val prep =
                com.maxrave.simpmusic.utils.prepareHeroArtworkResult(
                    platformContext = platformContext,
                    rawUrl = thumbUrl,
                    hintUrl = hintThumbUrl,
                    targetPx = com.maxrave.simpmusic.utils.THUMB_HEADER_HERO_PX,
                )
            if (prep.bitmap != null) {
                bitmap = prep.bitmap
                paletteState.generate(prep.bitmap)
                paletteGeneratedFor = thumbUrl
                heroVisualReady = true
            }
        }
        heroPrepDone = true
        if (!heroVisualReady) {
            kotlinx.coroutines.delay(500L)
            heroVisualReady = true
        }
    }

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && paletteGeneratedFor != currentThumbnail) {
            paletteState.generate(bm)
            paletteGeneratedFor = currentThumbnail
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                viewModel.setBrush(listOf(it.getColorFromPalette(), Color.Black))
            }
    }

    // Apple Music-inspired immersive treatment. Which header is used depends on the window's
    // aspect ratio alone, not on the platform: a portrait window (a phone held upright, or a
    // narrow desktop window) gets the edge-to-edge artwork header, a landscape one gets the
    // side-by-side header. Everything else on the page — the palette background, the row
    // dividers, the blurred top bar — is shared by both.
    val screenInfo = getScreenSizeInfo()
    val isPortrait = screenInfo.wDP < screenInfo.hDP
    // Apple Music-style page background. When the playlist has real artwork we pick a vivid
    // swatch from its palette (getColorFromPalette). Many local playlists have NO artwork —
    // AsyncImage shows the title placeholder and onSuccess never fires, so the palette stays
    // null; in that case we fall back to the SAME deterministic title gradient the placeholder
    // draws, so the background still matches the thumbnail. Darkened slightly for readability.
    // Held and faded (see UIExt.rememberPaletteColorState).
    val titleColors = playlistTitleGradient(uiState.title)
    val titleBase =
        if (titleColors.size >= 2) {
            lerp(titleColors[0], titleColors[1], 0.5f)
        } else {
            titleColors.firstOrNull() ?: Color.Black
        }
    val mutedPaletteBgState = rememberPaletteColorState(paletteState.palette, fallback = lerp(titleBase, Color.Black, 0.3f))

    // Loading dialog
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    if (showLoadingDialog.first) {
        LoadingDialog(
            true,
            showLoadingDialog.second,
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val dragDropState =
        rememberDragDropState(lazyState) { from, to ->
            coroutineScope.launch {
                Logger.d(TAG, "onMove from $from to $to")
                viewModel.changeLocalPlaylistItemPosition(from - 1, to - 1)
                trackPagingItems.refresh()
            }
        }
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val isPagingReady =
        uiState.trackCount == 0 ||
            trackPagingItems.itemCount > 0 ||
            trackPagingItems.loadState.refresh !is LoadState.Loading
    val localEffectiveLoadState =
        when {
            uiState.id == id && uiState.loadState == LocalPlaylistState.PlaylistLoadState.Error ->
                LocalPlaylistState.PlaylistLoadState.Error
            uiState.id == id &&
                uiState.loadState == LocalPlaylistState.PlaylistLoadState.Success &&
                isPagingReady &&
                heroPrepDone ->
                LocalPlaylistState.PlaylistLoadState.Success
            else -> LocalPlaylistState.PlaylistLoadState.Loading
        }

    Crossfade(targetState = localEffectiveLoadState) { loadState ->
        when (loadState) {
            LocalPlaylistState.PlaylistLoadState.Error -> {
                LaunchedEffect(Unit) { navController.navigateUp() }
            }

            LocalPlaylistState.PlaylistLoadState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                ) {
                    CenterLoadingBox(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .align(Alignment.Center),
                    )
                    RippleIconButton(
                        imageVector = SimpIcons.ArrowBackIosNew,
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .size(48.dp),
                    ) {
                        navController.navigateUp()
                    }
                }
            }

            LocalPlaylistState.PlaylistLoadState.Success -> {
//    Box {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(mutedPaletteBgState.value) }
                .glassHazeSource(hazeState)
                .pointerInput(changingOrder) {
                    if (!changingOrder) return@pointerInput
                    val onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, offset: Offset) -> Unit =
                        { change, offset ->
                            Logger.d(TAG, "onDrag $offset")
                            change.consume()
                            dragDropState.onDrag(offset = offset)

                            if (overscrollJob?.isActive != true) {
                                dragDropState
                                    .checkForOverScroll()
                                    .takeIf { it != 0f }
                                    ?.let {
                                        overscrollJob =
                                            coroutineScope.launch {
                                                dragDropState.state.animateScrollBy(
                                                    it * 1.3f,
                                                    tween(easing = FastOutLinearInEasing),
                                                )
                                            }
                                    }
                                    ?: run { overscrollJob?.cancel() }
                            }
                        }
                    val onDragStart: (Offset) -> Unit = { offset ->
                        Logger.d(TAG, "onDragStart $offset")
                        dragDropState.onDragStart(offset)
                    }
                    val onDragEnd: () -> Unit = {
                        Logger.d(TAG, "onDragEnd")
                        dragDropState.onDragInterrupted(true)
                        overscrollJob?.cancel()
                    }
                    val onDragCancel: () -> Unit = {
                        Logger.d(TAG, "onDragCancel")
                        dragDropState.onDragInterrupted()
                        overscrollJob?.cancel()
                    }
                    if (getPlatform() == Platform.Desktop) {
                        // Desktop: normal drag (click-and-drag), no long press needed
                        detectDragGestures(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    } else {
                        // Android: long press then drag (touch-friendly)
                        detectDragGesturesAfterLongPress(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }
                },
        state = lazyState,
    ) {
        item(contentType = "header") {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Transparent),
            ) {
                Column(
                    Modifier
                        .background(Color.Transparent),
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                    ) {
                        if (isPortrait) {
                            // Apple Music-style: edge-to-edge artwork + liquid glass buttons.
                            // Glass buttons MUST be siblings of the backdrop source (not children)
                            // to avoid render feedback loop / RuntimeShader crash.
                            val artworkBackdrop = rememberBackdrop(Color.Black)
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height((screenInfo.hDP / 2).dp),
                            ) {
                                // Inner Box — backdrop SOURCE (artwork + overlays only, NO glass)
                                Box(modifier = Modifier.fillMaxSize().layerBackdrop(artworkBackdrop)) {
                                    val localPlaylistHeroRequest =
                                        com.maxrave.simpmusic.utils.rememberOptimizedImageRequest(
                                            rawUrl = uiState.thumbnail,
                                            targetPx = com.maxrave.simpmusic.utils.THUMB_HEADER_HERO_PX,
                                            crossfadeMs = com.maxrave.simpmusic.utils.IMAGE_CROSSFADE_HERO_MS,
                                            fallbackPlaceholderUrl = hintThumbUrl,
                                        )
                                    val thumbFallbackPainter =
                                        painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp)
                                    if (localPlaylistHeroRequest == null) {
                                        androidx.compose.foundation.Image(
                                            painter = thumbFallbackPainter,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        AsyncImage(
                                            model = localPlaylistHeroRequest,
                                            transform =
                                                com.maxrave.simpmusic.utils.rememberRamFirstTransform(
                                                    fallbackPlaceholder = thumbFallbackPainter,
                                                    error = thumbFallbackPainter,
                                                ),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            onState = { imgState ->
                                                if (imgState is coil3.compose.AsyncImagePainter.State.Success) {
                                                    bitmap = imgState.result.image.toImageBitmap()
                                                    heroVisualReady = true
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    // Scrim spans 70% of the artwork (not a fixed 200dp): the shorter the
                                    // ramp, the steeper the alpha, and a steep ramp is what makes the fade
                                    // read as an edge. See artworkScrimBrush for the curve itself.
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height((screenInfo.hDP * 0.35f).dp)
                                                .align(Alignment.BottomCenter)
                                                .drawWithCache {
                                                    val scrimBrush = artworkScrimBrush(mutedPaletteBgState.value)
                                                    onDrawBehind { drawRect(scrimBrush) }
                                                },
                                    )
                                    Column(
                                        modifier =
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp)
                                                .padding(bottom = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = uiState.title,
                                            style = typo().titleLarge,
                                            color = Color.White,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(Res.string.your_playlist),
                                            style = typo().titleSmall,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text =
                                                stringResource(
                                                    Res.string.created_at,
                                                    uiState.inLibrary?.format(
                                                        LocalDateTime.Format {
                                                            hour()
                                                            char(':')
                                                            minute()
                                                            chars(" - ")
                                                            day()
                                                            char(' ')
                                                            monthName(
                                                                MonthNames.ENGLISH_FULL,
                                                            )
                                                            char(' ')
                                                            year()
                                                        },
                                                    ) ?: "",
                                                ),
                                            style = typo().bodyMedium,
                                            color = Color(0xC4FFFFFF),
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                // Back button + right pill (More only) overlays — liquid glass, siblings of source
                                Row(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .windowInsetsPadding(WindowInsets.statusBars),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    LiquidGlassIconButton(
                                        backdrop = artworkBackdrop,
                                        imageVector = SimpIcons.ArrowBackIosNew,
                                        modifier =
                                            Modifier
                                                .size(48.dp),
                                    ) {
                                        navController.navigateUp()
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Row(
                                        modifier =
                                            Modifier
                                                .height(48.dp)
                                                .liquidGlass(artworkBackdrop, RoundedCornerShape(24.dp)),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // AI Suggest — only when synced with YouTube
                                        if (shouldShowSuggestButton) {
                                            AnimatedSuggestIconButton(
                                                onClick = {
                                                    shouldShowSuggestions = !shouldShowSuggestions
                                                },
                                            )
                                        }
                                        IconButton(
                                            onClick = { showSearchBar = true },
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.Search,
                                                contentDescription = "Search in playlist",
                                                tint = Color.White,
                                            )
                                        }
                                        IconButton(
                                            onClick = onPlaylistMoreClick,
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.MoreVert,
                                                contentDescription = "More",
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Apple Music desktop header: back and the overlay actions on their own
                            // top row, then a square artwork with the text column and the action
                            // cluster laid out beside it rather than under it.
                            // Built exactly like the portrait branch, which renders correctly on both
                            // platforms: the backdrop SOURCE is the content column — artwork included,
                            // so the recorded layer holds something to refract — and the glass buttons
                            // are SIBLINGS placed with align(), never children of the source (that
                            // nesting is the render-feedback loop that kills the RuntimeShader).
                            val headerBackdrop = rememberBackdrop(Color.Black)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .layerBackdrop(headerBackdrop)
                                            .windowInsetsPadding(WindowInsets.statusBars)
                                            .padding(horizontal = 32.dp, vertical = 16.dp),
                                ) {
                                    // Reserves the strip the sibling glass buttons are drawn over.
                                    Spacer(modifier = Modifier.height(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        val localPlaylistDesktopHeroRequest =
                                            com.maxrave.simpmusic.utils.rememberOptimizedImageRequest(
                                                rawUrl = uiState.thumbnail,
                                                targetPx = com.maxrave.simpmusic.utils.THUMB_HEADER_HERO_PX,
                                                crossfadeMs = com.maxrave.simpmusic.utils.IMAGE_CROSSFADE_HERO_MS,
                                                fallbackPlaceholderUrl = hintThumbUrl,
                                            )
                                        val desktopThumbFallbackPainter =
                                            painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp)
                                        if (localPlaylistDesktopHeroRequest == null) {
                                            androidx.compose.foundation.Image(
                                                painter = desktopThumbFallbackPainter,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier =
                                                    Modifier
                                                        .size(280.dp)
                                                        .clip(ArtworkShape),
                                            )
                                        } else {
                                            AsyncImage(
                                                model = localPlaylistDesktopHeroRequest,
                                                transform =
                                                    com.maxrave.simpmusic.utils.rememberRamFirstTransform(
                                                        fallbackPlaceholder = desktopThumbFallbackPainter,
                                                        error = desktopThumbFallbackPainter,
                                                    ),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                onState = { imgState ->
                                                    if (imgState is coil3.compose.AsyncImagePainter.State.Success) {
                                                        bitmap = imgState.result.image.toImageBitmap()
                                                        heroVisualReady = true
                                                    }
                                                },
                                                modifier =
                                                    Modifier
                                                        .size(280.dp)
                                                        .clip(ArtworkShape),
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = uiState.title,
                                                style = typo().headlineSmall,
                                                color = Color.White,
                                                maxLines = 2,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(Res.string.your_playlist),
                                                style = typo().titleMedium,
                                                // The app accent, standing in for the brand red Apple uses here.
                                                color = seed,
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.created_at,
                                                        uiState.inLibrary?.format(
                                                            LocalDateTime.Format {
                                                                hour()
                                                                char(':')
                                                                minute()
                                                                chars(" - ")
                                                                day()
                                                                char(' ')
                                                                monthName(
                                                                    MonthNames.ENGLISH_FULL,
                                                                )
                                                                char(' ')
                                                                year()
                                                            },
                                                        ) ?: "",
                                                    ),
                                                style = typo().bodyMedium,
                                                color = Color(0xC4FFFFFF),
                                                textAlign = TextAlign.Center,
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            // Apple Music-style action row: [Shuffle][Play pill][Download][Suggest]
                                            // centered cluster. Suggest (AI) only shows when synced with YouTube.
                                            val isThisPlaying =
                                                isPlaying && playingPlaylistId == LOCAL_PLAYLIST_ID + uiState.id
                                            // The queue can belong to this playlist while paused; the tap must
                                            // resume it then, not restart the playlist from the first song.
                                            val isThisPlaylist = playingPlaylistId == LOCAL_PLAYLIST_ID + uiState.id
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                horizontalArrangement =
                                                    Arrangement.spacedBy(
                                                        12.dp,
                                                        Alignment.Start,
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.12f))
                                                            .clickable {
                                                                viewModel.onUIEvent(LocalPlaylistUIEvent.ShuffleClick)
                                                            },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = SimpIcons.Shuffle,
                                                        contentDescription = "Shuffle",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                }
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .height(48.dp)
                                                            .widthIn(min = 110.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                            .clickable {
                                                                if (isThisPlaylist) {
                                                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                                } else {
                                                                    viewModel.onUIEvent(LocalPlaylistUIEvent.PlayClick)
                                                                }
                                                            }.padding(horizontal = 20.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector =
                                                                if (isThisPlaying) {
                                                                    SimpIcons.Pause
                                                                } else {
                                                                    SimpIcons.PlayArrow
                                                                },
                                                            contentDescription = null,
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(22.dp),
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (isThisPlaying) "Pause" else "Play",
                                                            color = Color.Black,
                                                            style = typo().labelLarge,
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Crossfade(targetState = downloadState) { state ->
                                                        when (state) {
                                                            DownloadState.STATE_DOWNLOADED -> {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .fillMaxSize()
                                                                            .clickable {
                                                                                viewModel.makeToast(
                                                                                    runBlocking { getString(Res.string.downloaded) },
                                                                                )
                                                                            },
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(Res.drawable.baseline_downloaded),
                                                                        tint = Color(0xFF00A0CB),
                                                                        contentDescription = "",
                                                                        modifier = Modifier.size(22.dp),
                                                                    )
                                                                }
                                                            }

                                                            DownloadState.STATE_DOWNLOADING -> {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .fillMaxSize()
                                                                            .clickable {
                                                                                viewModel.makeToast(
                                                                                    runBlocking { getString(Res.string.downloading) },
                                                                                )
                                                                            },
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    DownloadingIndicator(
                                                                        modifier = Modifier.size(28.dp),
                                                                    )
                                                                }
                                                            }

                                                            else -> {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .fillMaxSize()
                                                                            .clickable {
                                                                                Logger.w("PlaylistScreen", "downloadState: $downloadState")
                                                                                viewModel.downloadFullPlaylist()
                                                                            },
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Icon(
                                                                        imageVector = SimpIcons.DownloadForOffline,
                                                                        tint = Color.White,
                                                                        contentDescription = "Download",
                                                                        modifier = Modifier.size(22.dp),
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
                                // Glass overlays — siblings of the source, over the strip the column reserved.
                                Box(Modifier.padding(start = 12.dp)) {
                                    LiquidGlassIconButton(
                                        backdrop = headerBackdrop,
                                        imageVector = SimpIcons.ArrowBackIosNew,
                                        shape = RoundedCornerShape(24.dp),
                                        // Same directional style as the like/⋯ pill, a touch thicker. The default
                                        // width of 0.5.dp becomes a ~2px stroke (HighlightModifier: ceil(width.toPx()) * 2),
                                        // which reads along the pill's long edge but vanishes around a 48dp circle. 1.dp
                                        // is the smallest step up that stays visible without looking like a border.
                                        highlight = Highlight(width = 1.dp),
                                        modifier =
                                            Modifier
                                                .align(Alignment.TopStart)
                                                .padding(12.dp)
                                                .windowInsetsPadding(WindowInsets.statusBars)
                                                .size(48.dp),
                                    ) {
                                        navController.navigateUp()
                                    }
                                }
                                Row(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .windowInsetsPadding(WindowInsets.statusBars)
                                            .padding(end = 32.dp, top = 16.dp)
                                            .height(48.dp)
                                            .liquidGlass(headerBackdrop, RoundedCornerShape(24.dp)),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // AI Suggest — only when synced with YouTube
                                    if (shouldShowSuggestButton) {
                                        AnimatedSuggestIconButton(
                                            onClick = {
                                                shouldShowSuggestions = !shouldShowSuggestions
                                            },
                                        )
                                    }
                                    IconButton(
                                        onClick = { showSearchBar = true },
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.Search,
                                            contentDescription = "Search in playlist",
                                            tint = Color.White,
                                        )
                                    }
                                    IconButton(
                                        onClick = onPlaylistMoreClick,
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.MoreVert,
                                            contentDescription = "More",
                                            tint = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                        ) {
                            Column(Modifier.padding(horizontal = 32.dp)) {
                                if (isPortrait) {
                                    // Apple Music-style action row: [Shuffle][Play pill][Download][Suggest]
                                    // centered cluster. Suggest (AI) only shows when synced with YouTube.
                                    val isThisPlaying =
                                        isPlaying && playingPlaylistId == LOCAL_PLAYLIST_ID + uiState.id
                                    // The queue can belong to this playlist while paused; the tap must
                                    // resume it then, not restart the playlist from the first song.
                                    val isThisPlaylist = playingPlaylistId == LOCAL_PLAYLIST_ID + uiState.id
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                12.dp,
                                                Alignment.CenterHorizontally,
                                            ),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.12f))
                                                    .clickable {
                                                        viewModel.onUIEvent(LocalPlaylistUIEvent.ShuffleClick)
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.Shuffle,
                                                contentDescription = "Shuffle",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                        Box(
                                            modifier =
                                                Modifier
                                                    .height(48.dp)
                                                    .widthIn(min = 110.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .clickable {
                                                        if (isThisPlaylist) {
                                                            sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                        } else {
                                                            viewModel.onUIEvent(LocalPlaylistUIEvent.PlayClick)
                                                        }
                                                    }.padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector =
                                                        if (isThisPlaying) {
                                                            SimpIcons.Pause
                                                        } else {
                                                            SimpIcons.PlayArrow
                                                        },
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isThisPlaying) "Pause" else "Play",
                                                    color = Color.Black,
                                                    style = typo().labelLarge,
                                                )
                                            }
                                        }
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Crossfade(targetState = downloadState) { state ->
                                                when (state) {
                                                    DownloadState.STATE_DOWNLOADED -> {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .clickable {
                                                                        viewModel.makeToast(
                                                                            runBlocking { getString(Res.string.downloaded) },
                                                                        )
                                                                    },
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(Res.drawable.baseline_downloaded),
                                                                tint = Color(0xFF00A0CB),
                                                                contentDescription = "",
                                                                modifier = Modifier.size(22.dp),
                                                            )
                                                        }
                                                    }

                                                    DownloadState.STATE_DOWNLOADING -> {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .clickable {
                                                                        viewModel.makeToast(
                                                                            runBlocking { getString(Res.string.downloading) },
                                                                        )
                                                                    },
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            DownloadingIndicator(
                                                                modifier = Modifier.size(28.dp),
                                                            )
                                                        }
                                                    }

                                                    else -> {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .clickable {
                                                                        Logger.w("PlaylistScreen", "downloadState: $downloadState")
                                                                        viewModel.downloadFullPlaylist()
                                                                    },
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Icon(
                                                                imageVector = SimpIcons.DownloadForOffline,
                                                                tint = Color.White,
                                                                contentDescription = "Download",
                                                                modifier = Modifier.size(22.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text =
                                        stringResource(
                                            Res.string.album_length,
                                            (uiState.trackCount).toString(),
                                            "",
                                        ),
                                    color = Color.White,
                                    style = typo().bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                                Column {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                        // Match the Apple Music action buttons: flat white@12% pill, white content.
                                        Row(
                                            modifier =
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.12f))
                                                    .clickable {
                                                        sortBottomSheetShow = true
                                                    }.padding(vertical = 8.dp, horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.Sort,
                                                contentDescription = "Sort playlist",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text(
                                                text =
                                                    stringResource(Res.string.sort_by) + ": " +
                                                        stringResource(
                                                            uiState.filterState.displayNameRes(),
                                                        ),
                                                style = typo().bodySmall,
                                                color = Color.White,
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        AnimatedVisibility(
                                            visible =
                                                uiState.filterState == FilterState.CustomOrder,
                                            enter = fadeIn(),
                                            exit = fadeOut(),
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    changingOrder = !changingOrder
                                                },
                                            ) {
                                                Text(
                                                    text =
                                                        if (changingOrder) {
                                                            stringResource(Res.string.done)
                                                        } else {
                                                            stringResource(Res.string.change_order)
                                                        },
                                                    style = typo().bodySmall,
                                                    color = Color.White,
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
        }
        item(key = "local_add_a_song_row", contentType = "add_a_song_row") {
            AddSongToPlaylistRow(onClick = { showAddSongSheet = true })
        }
        if (trackPagingItems.itemCount == 0 && uiState.loadState != LocalPlaylistState.PlaylistLoadState.Loading) {
            item(key = "local_playlist_no_tracks_yet", contentType = "empty_hint") {
                Text(
                    text = stringResource(Res.string.playlist_no_tracks_yet),
                    style = typo().bodyMedium,
                    color = Color(0xC4FFFFFF),
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }
        }
        items(
            count = trackPagingItems.itemCount,
            key = { index -> trackPagingItems.peek(index)?.second?.id ?: "item_$index" },
            contentType = { "local_track_row" },
        ) { index ->
            val item = trackPagingItems[index]?.first
            if (item != null) {
                val content = @Composable { mod: Modifier ->
                    SongFullWidthItems(
                        forceDark = true,
                        isPlaying = playingTrack?.videoId == item.videoId,
                        shouldShowDragHandle = changingOrder,
                        songEntity = item,
                        localSongOverride = item,
                        hasLocalSongOverride = true,
                        onMoreClickListener = { onItemMoreClick(it) },
                        onClickListener = { onPlaylistItemClick(it) },
                        onAddToQueue = {
                            sharedViewModel.addListToQueue(
                                arrayListOf(item.toTrack()),
                            )
                        },
                        selectionMode = selectionState.isActive,
                        isSelected = selectionState.isSelected(item.videoId),
                        // No long press while reordering: that gesture already belongs to drag.
                        onLongClick = if (changingOrder) null else ({ selectionState.start(it) }),
                        onSelectToggle = { selectionState.toggle(it) },
                        modifier = mod,
                    )
                }
                if (changingOrder) {
                    DraggableItem(
                        dragDropState,
                        index + 1,
                        Modifier.animateItem(),
                    ) {
                        content(Modifier)
                    }
                } else {
                    Column {
                        content(Modifier)
                        if (index < trackPagingItems.itemCount - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = Color.White.copy(alpha = 0.12f),
                            )
                        }
                    }
                }
            }
        }
        if (shouldShowSuggestButton && shouldShowSuggestions && (suggestionsLoading || suggestedTracks.isNotEmpty() || suggestionsReloadParams != null)) {
            item(key = "local_playlist_suggestions_section", contentType = "playlist_suggestions") {
                PlaylistSuggestionsSection(
                    suggestions = suggestedTracks,
                    isLoading = suggestionsLoading,
                    canReload = !suggestionsReloadParams.isNullOrEmpty(),
                    addingVideoIds = addingSuggestionIds,
                    playingVideoId = playingTrack?.videoId,
                    isPlaybackActive = isPlaying,
                    onPlayTrack = { track -> viewModel.onUIEvent(LocalPlaylistUIEvent.SuggestionsItemClick(track.videoId)) },
                    onAddTrack = { track -> viewModel.addSuggestTrackToListTrack(track) },
                    onReload = { viewModel.reloadSuggestion() },
                )
            }
        }
        if (relatedPlaylists.isNotEmpty()) {
            item(key = "local_related_playlists_section", contentType = "related_playlists") {
                val platformContext = LocalPlatformContext.current
                RelatedPlaylistsSection(
                    relatedPlaylists = relatedPlaylists,
                    onPlaylistClick = { related ->
                        primeCollectionArtworkNavigation(
                            platformContext = platformContext,
                            collectionId = related.browseId,
                            rawThumbUrl = related.thumbnails.lastOrNull()?.url,
                        )
                        navController.navigate(
                            PlaylistDestination(
                                playlistId = related.browseId,
                                isYourYouTubePlaylist = false,
                            ),
                        )
                    },
                )
            }
        }
        item(contentType = "end_of_page") {
            EndOfPage()
        }
    }
    VerticalScrollBar(
        scrollState = lazyState,
        modifier = Modifier.padding(bottom = DesktopMiniPlayerClearance),
    )
    AnimatedVisibility(
        visible = selectionState.isActive,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        SongSelectionTopAppBar(
            state = selectionState,
            onSelectAll = {
                // peek() rather than get(): reading an item is what asks Paging to load its page,
                // and select-all must not drag the whole playlist into memory.
                selectionState.toggleSelectAll(
                    (0 until trackPagingItems.itemCount).mapNotNull {
                        trackPagingItems.peek(it)?.first?.videoId
                    },
                )
            },
            onOpenActions = { showSelectionSheet = true },
            modifier =
                Modifier.tintedBarBlur(hazeState, mutedPaletteBgState.value, 0.55f),
        )
    }
    if (showSelectionSheet) {
        val selectedIds = selectionState.selected.toList()
        val removeLabel = stringResource(Res.string.remove_from_playlist)
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
            extraActions =
                listOf(
                    SongSelectionAction(
                        icon = SimpIcons.Delete,
                        label = removeLabel,
                        tint = Color.Red,
                    ) {
                        selectionViewModel.removeFromLocalPlaylist(uiState.id, selectedIds)
                        selectionState.exit()
                    },
                ),
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
    if (showAddSongSheet) {
        val existingIds =
            (0 until trackPagingItems.itemCount)
                .mapNotNull { trackPagingItems.peek(it)?.first?.videoId }
                .toSet()
        AddSongToPlaylistSheet(
            existingVideoIds = existingIds,
            onAddTrack = { track -> viewModel.addTrackFromSearch(track) },
            onDismiss = { showAddSongSheet = false },
        )
    }
    if (itemBottomSheetShow && currentItem != null) {
        val track = currentItem ?: return@Crossfade
        NowPlayingBottomSheet(
            onDelete = { viewModel.deleteItem(uiState.id, track) },
            onDismiss = {
                itemBottomSheetShow = false
                currentItem = null
            },
            navController = navController,
            song = track,
        )
    }
    if (playlistBottomSheetShow) {
        Logger.w("PlaylistScreen", "PlaylistBottomSheet")
        LocalPlaylistBottomSheet(
            isBottomSheetVisible = playlistBottomSheetShow,
            onDismiss = { playlistBottomSheetShow = false },
            title = uiState.title,
            ytPlaylistId = uiState.ytPlaylistId,
            onEditTitle =
                { newTitle ->
                    viewModel.updatePlaylistTitle(newTitle, uiState.id)
                },
            onEditThumbnail =
                { thumbUri ->
                    viewModel.updatePlaylistThumbnail(thumbUri, uiState.id)
                },
            onAddToQueue = {
                viewModel.addAllToQueue()
            },
            onSync = {
                if (uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
                    showUnsyncAlertDialog = true
                } else {
                    showSyncAlertDialog = true
                }
            },
            onUpdatePlaylist = {
                viewModel.updateListTrackSynced(uiState.id)
            },
            onDelete = {
                viewModel.deletePlaylist(uiState.id)
                navController.navigateUp()
            },
        )
    }
    if (showSyncAlertDialog) {
        AlertDialog(
            containerColor = rememberSurfaceDarkColors().container,
            titleContentColor = rememberSurfaceDarkColors().content,
            textContentColor = rememberSurfaceDarkColors().content,
            title = { Text(text = stringResource(Res.string.warning)) },
            text = { Text(text = stringResource(Res.string.sync_playlist_warning)) },
            onDismissRequest = { showSyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.syncPlaylistWithYouTubePlaylist(uiState.id)
                    showSyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
    if (showUnsyncAlertDialog) {
        AlertDialog(
            containerColor = rememberSurfaceDarkColors().container,
            titleContentColor = rememberSurfaceDarkColors().content,
            textContentColor = rememberSurfaceDarkColors().content,
            title = { Text(text = stringResource(Res.string.warning)) },
            text = { Text(text = stringResource(Res.string.unsync_playlist_warning)) },
            onDismissRequest = { showUnsyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unsyncPlaylistWithYouTubePlaylist(uiState.id)
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
    // A sibling of the paged list, not a branch inside it. The paged reader carries drag
    // reordering, in-place removal and its own scroll state; searching has none of that, and
    // threading a second data source through it would put all of that at risk for a feature that
    // only needs a flat list. This draws over it instead and leaves it untouched.
    //
    // Results come from LocalPlaylistViewModel.searchResults, which queries the database —
    // filtering trackPagingItems would only ever search the pages already scrolled through.
    AnimatedVisibility(
        visible = showSearchBar,
        enter = SearchBarEnter,
        exit = SearchBarExit,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(mutedPaletteBgState.value),
        ) {
            // Same bar as the one on Playlist and Album: blurred over the content behind it,
            // back on the left, clear on the right.
            Box(
                Modifier
                    .fillMaxWidth()
                    .tintedBarBlur(hazeState, mutedPaletteBgState.value, 0.55f),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .windowInsetsPadding(WindowInsets.statusBars),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RippleIconButton(
                        imageVector = SimpIcons.ArrowBackIosNew,
                    ) {
                        navController.navigateUp()
                    }
                    SearchBar(
                        modifier =
                            Modifier
                                .height(50.dp)
                                .padding(horizontal = 12.dp)
                                .weight(1f),
                        colors =
                            SearchBarDefaults.colors().copy(
                                containerColor = Color.Transparent,
                            ),
                        inputField = {
                            CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
                                SearchBarDefaults.InputField(
                                    query = searchQuery,
                                    onQueryChange = { viewModel.setSearchQuery(it) },
                                    onSearch = {},
                                    expanded = showSearchBar,
                                    onExpandedChange = { showSearchBar = it },
                                    placeholder = {
                                        Text(
                                            stringResource(Res.string.search),
                                            style = typo().bodyMedium,
                                        )
                                    },
                                )
                            }
                        },
                        expanded = false,
                        onExpandedChange = {},
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                    }
                    IconButton(
                        onClick = {
                            showSearchBar = false
                            viewModel.setSearchQuery("")
                        },
                    ) {
                        Icon(SimpIcons.Close, null, tint = Color.White)
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    searchResults,
                    key = { (song, pair) -> song.videoId + "_" + pair.position },
                ) { (song, _) ->
                    SongFullWidthItems(
                        forceDark = true,
                        isPlaying = playingTrack?.videoId == song.videoId,
                        songEntity = song,
                        localSongOverride = song,
                        hasLocalSongOverride = true,
                        onMoreClickListener = { onItemMoreClick(it) },
                        onClickListener = { onPlaylistItemClick(it) },
                        onAddToQueue = { sharedViewModel.addListToQueue(arrayListOf(song.toTrack())) },
                        selectionMode = selectionState.isActive,
                        isSelected = selectionState.isSelected(song.videoId),
                        onLongClick = { selectionState.start(it) },
                        onSelectToggle = { selectionState.toggle(it) },
                        // Required, no default — the paged list passes it down through `mod`.
                        modifier = Modifier,
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp))
                }
            }
        }
    }
    if (sortBottomSheetShow) {
        SortPlaylistBottomSheet(
            selectedState = uiState.filterState,
            onDismiss = { sortBottomSheetShow = false },
            onSortChanged = {
                viewModel.onUIEvent(LocalPlaylistUIEvent.ChangeFilter(it))
                sortBottomSheetShow = false
            },
        )
    }
    AnimatedVisibility(
        visible = shouldHideTopBar && !selectionState.isActive,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        TopAppBar(
            windowInsets =
                TopAppBarDefaults.windowInsets.exclude(
                    TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start),
                ),
            title = {
                Text(
                    text = uiState.title,
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        SimpIcons.ArrowBackIosNew,
                        Modifier
                            .size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            modifier =
                Modifier.tintedBarBlur(hazeState, mutedPaletteBgState.value, 0.55f),
        )
    }
            }
        }
    }
}

@Composable
private fun AnimatedSuggestIconButton(onClick: () -> Unit) {
    val aiPainter = rememberVectorPainter(SimpIcons.TipsAndUpdates)
    val transition = rememberInfiniteTransition(label = "suggest_shimmer")
    val progressAnimated by transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "suggest_shimmer_progress",
    )
    IconButton(onClick = onClick) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }.drawWithContent {
                        val width = size.width
                        val height = size.height
                        val offsetDraw = width * progressAnimated
                        val brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color(0xFF4C82EF),
                                        Color(0xFFD96570),
                                    ),
                                start = Offset(offsetDraw, 0f),
                                end = Offset(offsetDraw + width, height),
                            )
                        with(aiPainter) {
                            draw(size = Size(width, height))
                        }
                        drawRect(
                            brush = brush,
                            blendMode = BlendMode.SrcIn,
                        )
                    },
        )
    }
}