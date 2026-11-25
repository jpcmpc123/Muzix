package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.extension.now
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.DesktopMiniPlayerClearance
import com.maxrave.simpmusic.expect.ui.VerticalScrollBar
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.extension.glassHazeSource
import com.maxrave.simpmusic.extension.ultraThinBarBlur
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RecentlyAddedRow
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HistoryDateBucket
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.groupRecentlyAddedByDate
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.history
import simpmusic.composeapp.generated.resources.history_older
import simpmusic.composeapp.generated.resources.history_this_month
import simpmusic.composeapp.generated.resources.history_this_week
import simpmusic.composeapp.generated.resources.history_today
import simpmusic.composeapp.generated.resources.history_yesterday
import simpmusic.composeapp.generated.resources.no_history
import simpmusic.composeapp.generated.resources.radio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryHistoryScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LibraryViewModel = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val density = LocalDensity.current
    val hazeState = rememberHazeState()
    var topAppBarHeight by remember { mutableStateOf(0.dp) }

    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    var recentSheetSong by remember { mutableStateOf<SongEntity?>(null) }

    LaunchedEffect(nowPlaying) {
        viewModel.getRecentlyAdded()
    }

    val recentItems =
        remember(recentlyAdded) {
            if (recentlyAdded is LocalResource.Loading) {
                emptyList()
            } else {
                (recentlyAdded.data ?: emptyList()).filterIsInstance<RecentlyType>()
            }
        }
    data class KeyedHistorySection(
        val bucket: HistoryDateBucket,
        val keyedItems: List<Pair<String, RecentlyType>>,
    )

    val sections =
        remember(recentItems) {
            val grouped = groupRecentlyAddedByDate(recentItems, today = now().date)
            val seen = HashMap<String, Int>(recentItems.size)
            grouped.map { section ->
                val keyed =
                    section.items.map { entry ->
                        val base =
                            when (entry) {
                                is SongEntity -> "song_${entry.videoId}"
                                is ArtistEntity -> "artist_${entry.channelId}"
                                is AlbumEntity -> "album_${entry.browseId}"
                                is PlaylistEntity -> "playlist_${entry.id}"
                                is PodcastsEntity -> "podcast_${entry.podcastId}"
                                else -> "recent_${entry.hashCode()}"
                            }
                        val count = seen[base] ?: 0
                        seen[base] = count + 1
                        val uniqueKey =
                            if (count == 0) {
                                "${section.bucket.name}_$base"
                            } else {
                                "${section.bucket.name}_${base}#$count"
                            }
                        uniqueKey to entry
                    }
                KeyedHistorySection(section.bucket, keyed)
            }
        }
    val radioSuffix = stringResource(Res.string.radio)

    if (recentSheetSong != null) {
        NowPlayingBottomSheet(
            onDismiss = { recentSheetSong = null },
            navController = navController,
            song = recentSheetSong!!,
            onLibraryDelete = {
                recentSheetSong?.videoId?.let { viewModel.deleteSong(it) }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        BoxWithConstraints(Modifier.fillMaxSize().glassHazeSource(hazeState)) {
            val layoutDirection = LocalLayoutDirection.current
            val innerStart = innerPadding.calculateStartPadding(layoutDirection)
            val innerEnd = innerPadding.calculateEndPadding(layoutDirection)
            val sideInset =
                if (getPlatform() == Platform.Desktop) {
                    ((maxWidth - innerStart - innerEnd - YOUR_LIBRARY_MAX_WIDTH) / 2).coerceAtLeast(0.dp)
                } else {
                    0.dp
                }

            when {
                recentlyAdded is LocalResource.Loading && recentItems.isEmpty() -> {
                    CenterLoadingBox(
                        Modifier
                            .fillMaxSize()
                            .padding(top = topAppBarHeight),
                    )
                }

                sections.isEmpty() -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    start = innerStart + sideInset + 16.dp,
                                    top = topAppBarHeight,
                                    end = innerEnd + sideInset + 16.dp,
                                    bottom = innerPadding.calculateBottomPadding(),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.no_history),
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding =
                            innerPadding.copy(
                                start = innerStart + sideInset,
                                top = topAppBarHeight,
                                end = innerEnd + sideInset,
                            ),
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        sections.forEach { section ->
                            item(key = "history_header_${section.bucket.name}", contentType = "history_section_header") {
                                val headerTitle =
                                    when (section.bucket) {
                                        HistoryDateBucket.TODAY -> stringResource(Res.string.history_today)
                                        HistoryDateBucket.YESTERDAY -> stringResource(Res.string.history_yesterday)
                                        HistoryDateBucket.THIS_WEEK -> stringResource(Res.string.history_this_week)
                                        HistoryDateBucket.THIS_MONTH -> stringResource(Res.string.history_this_month)
                                        HistoryDateBucket.OLDER -> stringResource(Res.string.history_older)
                                    }
                                Text(
                                    text = headerTitle,
                                    style = typo().headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 16.dp, bottom = 6.dp),
                                )
                            }

                            itemsIndexed(
                                items = section.keyedItems,
                                key = { _, pair -> pair.first },
                                contentType = { _, pair -> pair.second.objectType().name },
                            ) { _, (_, entry) ->
                                RecentlyAddedRow(
                                    item = entry,
                                    playingVideoId = nowPlaying,
                                    radioSuffix = radioSuffix,
                                    navController = navController,
                                    viewModel = viewModel,
                                    sharedViewModel = sharedViewModel,
                                    selectionState = selectionState,
                                    onShowBottomSheet = { clicked -> recentSheetSong = clicked },
                                )
                            }
                        }
                        item(key = "history_end") {
                            EndOfPage()
                        }
                    }
                    VerticalScrollBar(
                        scrollState = listState,
                        modifier = Modifier.padding(top = topAppBarHeight, bottom = DesktopMiniPlayerClearance),
                    )
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .ultraThinBarBlur(hazeState)
                    .onGloballyPositioned { coordinates ->
                        topAppBarHeight = with(density) { coordinates.size.height.toDp() }
                    },
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.history),
                        style = typo().titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            SimpIcons.ArrowBackIosNew,
                            Modifier.size(32.dp),
                            true,
                            tint = MaterialTheme.colorScheme.onBackground,
                        ) {
                            navController.navigateUp()
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
            AnimatedVisibility(visible = selectionState.isActive) {
                SongSelectionTopAppBar(
                    state = selectionState,
                    windowInsets = WindowInsets(0),
                    onSelectAll = {
                        selectionState.toggleSelectAll(
                            recentItems
                                .filterIsInstance<SongEntity>()
                                .map { it.videoId },
                        )
                    },
                    onOpenActions = { showSelectionSheet = true },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                )
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
    }
}
