package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.expect.deleteLegacyDeviceAudioTrack
import com.maxrave.simpmusic.expect.rememberDeviceAudioDeleteRequester
import com.maxrave.simpmusic.expect.rememberDeviceAudioPermissionRequester
import com.maxrave.simpmusic.expect.shareDeviceAudioTrack
import com.maxrave.simpmusic.expect.ui.PlatformBackHandler
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.localaudio.DeviceAudioAlbum
import com.maxrave.simpmusic.localaudio.DeviceAudioArtist
import com.maxrave.simpmusic.localaudio.DeviceAudioFolder
import com.maxrave.simpmusic.localaudio.DeviceAudioLibraryEngine
import com.maxrave.simpmusic.localaudio.DeviceAudioSortOrder
import com.maxrave.simpmusic.localaudio.DeviceAudioSubTab
import com.maxrave.simpmusic.localaudio.DeviceAudioTechnicalDetails
import com.maxrave.simpmusic.localaudio.DeviceAudioTrack
import com.maxrave.simpmusic.localaudio.formatAudioTechnicalLabel
import com.maxrave.simpmusic.localaudio.formatDurationMs
import com.maxrave.simpmusic.localaudio.formatFileSize
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.icon.Album
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Delete
import com.maxrave.simpmusic.ui.icon.FormatListBulleted
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.MusicNote
import com.maxrave.simpmusic.ui.icon.Person
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.Refresh
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Smartphone
import com.maxrave.simpmusic.ui.icon.Sort
import com.maxrave.simpmusic.ui.icon.Tune
import com.maxrave.simpmusic.ui.theme.ArtworkShape
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.DeviceAudioViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.delete
import simpmusic.composeapp.generated.resources.device_audio_albums_count
import simpmusic.composeapp.generated.resources.device_audio_artists_count
import simpmusic.composeapp.generated.resources.device_audio_delete_confirm_body
import simpmusic.composeapp.generated.resources.device_audio_delete_confirm_title
import simpmusic.composeapp.generated.resources.device_audio_delete_failed_toast
import simpmusic.composeapp.generated.resources.device_audio_delete_from_device
import simpmusic.composeapp.generated.resources.device_audio_deleted_toast
import simpmusic.composeapp.generated.resources.device_audio_detail_bitrate
import simpmusic.composeapp.generated.resources.device_audio_detail_duration
import simpmusic.composeapp.generated.resources.device_audio_detail_file_name
import simpmusic.composeapp.generated.resources.device_audio_detail_format
import simpmusic.composeapp.generated.resources.device_audio_detail_path
import simpmusic.composeapp.generated.resources.device_audio_detail_sample_rate
import simpmusic.composeapp.generated.resources.device_audio_detail_size
import simpmusic.composeapp.generated.resources.device_audio_empty_subtitle
import simpmusic.composeapp.generated.resources.device_audio_empty_title
import simpmusic.composeapp.generated.resources.device_audio_filter_off
import simpmusic.composeapp.generated.resources.device_audio_folders_count
import simpmusic.composeapp.generated.resources.device_audio_grant_permission
import simpmusic.composeapp.generated.resources.device_audio_hidden_tracks
import simpmusic.composeapp.generated.resources.device_audio_hide_folder
import simpmusic.composeapp.generated.resources.device_audio_hide_track
import simpmusic.composeapp.generated.resources.device_audio_ignore_messaging_folders
import simpmusic.composeapp.generated.resources.device_audio_manage_folders
import simpmusic.composeapp.generated.resources.device_audio_min_duration
import simpmusic.composeapp.generated.resources.device_audio_min_size
import simpmusic.composeapp.generated.resources.device_audio_no_hidden_tracks
import simpmusic.composeapp.generated.resources.device_audio_open_settings
import simpmusic.composeapp.generated.resources.device_audio_permission_description
import simpmusic.composeapp.generated.resources.device_audio_permission_title
import simpmusic.composeapp.generated.resources.device_audio_play_next_added
import simpmusic.composeapp.generated.resources.device_audio_rescan
import simpmusic.composeapp.generated.resources.device_audio_restore
import simpmusic.composeapp.generated.resources.device_audio_restore_all
import simpmusic.composeapp.generated.resources.device_audio_scan_filters
import simpmusic.composeapp.generated.resources.device_audio_search_hint
import simpmusic.composeapp.generated.resources.device_audio_share_file
import simpmusic.composeapp.generated.resources.device_audio_sort_album
import simpmusic.composeapp.generated.resources.device_audio_sort_artist
import simpmusic.composeapp.generated.resources.device_audio_sort_duration
import simpmusic.composeapp.generated.resources.device_audio_sort_recent
import simpmusic.composeapp.generated.resources.device_audio_sort_title_az
import simpmusic.composeapp.generated.resources.device_audio_technical_details
import simpmusic.composeapp.generated.resources.device_audio_tracks_count
import simpmusic.composeapp.generated.resources.device_audio_unknown_album
import simpmusic.composeapp.generated.resources.device_audio_unknown_artist
import simpmusic.composeapp.generated.resources.folders
import simpmusic.composeapp.generated.resources.ok
import simpmusic.composeapp.generated.resources.on_device
import simpmusic.composeapp.generated.resources.play_all
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.shuffle
import simpmusic.composeapp.generated.resources.songs
import simpmusic.composeapp.generated.resources.sort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAudioLibraryTab(
    contentPadding: PaddingValues,
    onScrolling: (onTop: Boolean) -> Unit,
    viewModel: DeviceAudioViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDetailsTrack by viewModel.selectedDetailsTrack.collectAsStateWithLifecycle()
    val technicalDetails by viewModel.technicalDetails.collectAsStateWithLifecycle()
    val isLoadingDetails by viewModel.isLoadingDetails.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()
    val isScrollingUpState = listState.isScrollingUp()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex <= 1 || isScrollingUpState.value }
            .distinctUntilChanged()
            .collect { onScrolling(it) }
    }

    var permissionAskedOnce by remember { mutableStateOf(false) }
    val permissionRequester =
        rememberDeviceAudioPermissionRequester { granted ->
            permissionAskedOnce = true
            viewModel.onPermissionResult(granted)
        }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.syncPermissionAndScanIfNeeded()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.syncPermissionAndScanIfNeeded()
    }

    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    var selectedAlbumKey by remember { mutableStateOf<String?>(null) }
    var selectedArtistKey by remember { mutableStateOf<String?>(null) }

    var menuTrack by remember { mutableStateOf<DeviceAudioTrack?>(null) }
    var pendingLegacyDeleteTrack by remember { mutableStateOf<DeviceAudioTrack?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val deleteRequester =
        rememberDeviceAudioDeleteRequester(
            onDeleted = { mediaStoreId ->
                viewModel.onTrackDeletedFromDevice(mediaStoreId)
                scope.launch {
                    viewModel.makeToast(getString(Res.string.device_audio_deleted_toast))
                }
            },
            onCancelled = {},
            onLegacyConfirmationRequired = { track ->
                pendingLegacyDeleteTrack = track
            },
        )

    val activeFolder =
        remember(selectedFolderPath, uiState.folders) {
            selectedFolderPath?.let { path -> uiState.folders.firstOrNull { it.folderPath == path } }
        }
    val activeAlbum =
        remember(selectedAlbumKey, uiState.albums) {
            selectedAlbumKey?.let { key -> uiState.albums.firstOrNull { it.albumKey == key } }
        }
    val activeArtist =
        remember(selectedArtistKey, uiState.artists) {
            selectedArtistKey?.let { key -> uiState.artists.firstOrNull { it.artistKey == key } }
        }

    val hasDrilldown = activeFolder != null || activeAlbum != null || activeArtist != null
    PlatformBackHandler(enabled = hasDrilldown) {
        selectedFolderPath = null
        selectedAlbumKey = null
        selectedArtistKey = null
    }

    val queueDefaultTitle = stringResource(Res.string.on_device)
    val unknownArtist = stringResource(Res.string.device_audio_unknown_artist)
    val unknownAlbum = stringResource(Res.string.device_audio_unknown_album)

    if (!uiState.hasPermission) {
        DeviceAudioPermissionView(
            contentPadding = contentPadding,
            permissionAskedOnce = permissionAskedOnce,
            onRequestPermission = { permissionRequester.request() },
            onOpenSettings = { permissionRequester.openAppSettings() },
        )
        return
    }

    val drilldownTracks: List<DeviceAudioTrack>? =
        activeFolder?.tracks ?: activeAlbum?.tracks ?: activeArtist?.tracks
    val drilldownTitle: String? =
        activeFolder?.folderName
            ?: activeAlbum?.albumName?.ifBlank { unknownAlbum }
            ?: activeArtist?.artistName?.ifBlank { unknownArtist }
    val currentPlayableTracks = drilldownTracks ?: uiState.visibleTracks
    val currentQueueTitle = drilldownTitle ?: queueDefaultTitle

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        if (uiState.isScanning) {
            item(key = "scanning_indicator") {
                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (hasDrilldown && drilldownTitle != null && drilldownTracks != null) {
            item(key = "drilldown_header") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            selectedFolderPath = null
                            selectedAlbumKey = null
                            selectedArtistKey = null
                        },
                    ) {
                        Icon(
                            imageVector = SimpIcons.ArrowBackIosNew,
                            contentDescription = null,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = drilldownTitle,
                            style = typo().titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(Res.string.device_audio_tracks_count, drilldownTracks.size),
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            item(key = "device_audio_search") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.device_audio_search_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = SimpIcons.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = SimpIcons.Close,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            item(key = "device_audio_subtabs") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeviceAudioSubTabChip(
                        label = stringResource(Res.string.songs),
                        count = uiState.visibleTracks.size,
                        selected = uiState.selectedSubTab == DeviceAudioSubTab.SONGS,
                        onClick = { viewModel.selectSubTab(DeviceAudioSubTab.SONGS) },
                    )
                    DeviceAudioSubTabChip(
                        label = stringResource(Res.string.folders),
                        count = uiState.folders.size,
                        selected = uiState.selectedSubTab == DeviceAudioSubTab.FOLDERS,
                        onClick = { viewModel.selectSubTab(DeviceAudioSubTab.FOLDERS) },
                    )
                    DeviceAudioSubTabChip(
                        label = stringResource(Res.string.albums),
                        count = uiState.albums.size,
                        selected = uiState.selectedSubTab == DeviceAudioSubTab.ALBUMS,
                        onClick = { viewModel.selectSubTab(DeviceAudioSubTab.ALBUMS) },
                    )
                    DeviceAudioSubTabChip(
                        label = stringResource(Res.string.artists),
                        count = uiState.artists.size,
                        selected = uiState.selectedSubTab == DeviceAudioSubTab.ARTISTS,
                        onClick = { viewModel.selectSubTab(DeviceAudioSubTab.ARTISTS) },
                    )
                }
            }
        }

        item(key = "device_audio_controls") {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    enabled = currentPlayableTracks.isNotEmpty(),
                    onClick = {
                        viewModel.playTracks(
                            tracks = currentPlayableTracks,
                            startIndex = 0,
                            queueTitle = currentQueueTitle,
                            shuffle = false,
                            unknownArtistFallback = unknownArtist,
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = SimpIcons.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.play_all), maxLines = 1)
                }

                OutlinedButton(
                    enabled = currentPlayableTracks.isNotEmpty(),
                    onClick = {
                        viewModel.playTracks(
                            tracks = currentPlayableTracks,
                            startIndex = 0,
                            queueTitle = currentQueueTitle,
                            shuffle = true,
                            unknownArtistFallback = unknownArtist,
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = SimpIcons.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.shuffle), maxLines = 1)
                }

                if (!hasDrilldown) {
                    OutlinedButton(
                        onClick = { showSortSheet = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(Res.string.sort), maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showFilterSheet = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(Res.string.device_audio_scan_filters), maxLines = 1)
                    }
                }
            }
        }

        if (drilldownTracks != null) {
            if (drilldownTracks.isEmpty()) {
                item(key = "empty_drilldown") {
                    DeviceAudioEmptyView(
                        onRescan = viewModel::rescan,
                        onOpenFilters = { showFilterSheet = true },
                    )
                }
            } else {
                itemsIndexed(
                    items = drilldownTracks,
                    key = { _, item -> "drill_${item.mediaStoreId}" },
                ) { index, track ->
                    DeviceAudioTrackRow(
                        track = track,
                        unknownArtist = unknownArtist,
                        onClick = {
                            viewModel.playTracks(
                                tracks = drilldownTracks,
                                startIndex = index,
                                queueTitle = currentQueueTitle,
                                shuffle = false,
                                unknownArtistFallback = unknownArtist,
                            )
                        },
                        onMoreClick = { menuTrack = track },
                    )
                }
            }
        } else {
            when (uiState.selectedSubTab) {
                DeviceAudioSubTab.SONGS -> {
                    if (uiState.visibleTracks.isEmpty() && !uiState.isScanning) {
                        item(key = "empty_songs") {
                            DeviceAudioEmptyView(
                                onRescan = viewModel::rescan,
                                onOpenFilters = { showFilterSheet = true },
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.visibleTracks,
                            key = { _, item -> "song_${item.mediaStoreId}" },
                        ) { index, track ->
                            DeviceAudioTrackRow(
                                track = track,
                                unknownArtist = unknownArtist,
                                onClick = {
                                    viewModel.playTracks(
                                        tracks = uiState.visibleTracks,
                                        startIndex = index,
                                        queueTitle = queueDefaultTitle,
                                        shuffle = false,
                                        unknownArtistFallback = unknownArtist,
                                    )
                                },
                                onMoreClick = { menuTrack = track },
                            )
                        }
                    }
                }

                DeviceAudioSubTab.FOLDERS -> {
                    if (uiState.folders.isEmpty() && !uiState.isScanning) {
                        item(key = "empty_folders") {
                            DeviceAudioEmptyView(
                                onRescan = viewModel::rescan,
                                onOpenFilters = { showFilterSheet = true },
                            )
                        }
                    } else {
                        items(
                            items = uiState.folders,
                            key = { "folder_${it.folderPath}" },
                        ) { folder ->
                            DeviceAudioCollectionRow(
                                title = folder.folderName,
                                subtitle =
                                    stringResource(Res.string.device_audio_tracks_count, folder.tracks.size) +
                                        " · " + folder.folderPath,
                                artworkUri = null,
                                fallbackIcon = SimpIcons.FormatListBulleted,
                                circularArtwork = false,
                                onClick = { selectedFolderPath = folder.folderPath },
                            )
                        }
                    }
                }

                DeviceAudioSubTab.ALBUMS -> {
                    if (uiState.albums.isEmpty() && !uiState.isScanning) {
                        item(key = "empty_albums") {
                            DeviceAudioEmptyView(
                                onRescan = viewModel::rescan,
                                onOpenFilters = { showFilterSheet = true },
                            )
                        }
                    } else {
                        items(
                            items = uiState.albums,
                            key = { "album_${it.albumKey}" },
                        ) { album ->
                            DeviceAudioCollectionRow(
                                title = album.albumName.ifBlank { unknownAlbum },
                                subtitle =
                                    album.artistName.ifBlank { unknownArtist } +
                                        " · " +
                                        stringResource(Res.string.device_audio_tracks_count, album.tracks.size),
                                artworkUri = album.artworkUri,
                                fallbackIcon = SimpIcons.Album,
                                circularArtwork = false,
                                onClick = { selectedAlbumKey = album.albumKey },
                            )
                        }
                    }
                }

                DeviceAudioSubTab.ARTISTS -> {
                    if (uiState.artists.isEmpty() && !uiState.isScanning) {
                        item(key = "empty_artists") {
                            DeviceAudioEmptyView(
                                onRescan = viewModel::rescan,
                                onOpenFilters = { showFilterSheet = true },
                            )
                        }
                    } else {
                        items(
                            items = uiState.artists,
                            key = { "artist_${it.artistKey}" },
                        ) { artist ->
                            DeviceAudioCollectionRow(
                                title = artist.artistName.ifBlank { unknownArtist },
                                subtitle =
                                    stringResource(Res.string.device_audio_albums_count, artist.albumCount) +
                                        " · " +
                                        stringResource(Res.string.device_audio_tracks_count, artist.tracks.size),
                                artworkUri = artist.artworkUri,
                                fallbackIcon = SimpIcons.Person,
                                circularArtwork = true,
                                onClick = { selectedArtistKey = artist.artistKey },
                            )
                        }
                    }
                }
            }
        }

        item(key = "device_audio_end") {
            EndOfPage()
        }
    }

    menuTrack?.let { track ->
        DeviceAudioTrackMenuSheet(
            track = track,
            unknownArtist = unknownArtist,
            onDismiss = { menuTrack = null },
            onPlayNext = {
                viewModel.playNext(track, unknownArtistFallback = unknownArtist)
                menuTrack = null
                scope.launch {
                    viewModel.makeToast(getString(Res.string.device_audio_play_next_added))
                }
            },
            onShare = {
                menuTrack = null
                scope.launch {
                    shareDeviceAudioTrack(track, getString(Res.string.device_audio_share_file))
                }
            },
            onTechnicalDetails = {
                menuTrack = null
                viewModel.loadTechnicalDetails(track)
            },
            onHideTrack = {
                viewModel.hideTrack(track.mediaStoreId)
                menuTrack = null
            },
            onHideFolder = {
                viewModel.hideFolder(track.folderPath)
                menuTrack = null
            },
            onDelete = {
                menuTrack = null
                deleteRequester.requestDelete(track)
            },
        )
    }

    if (showSortSheet) {
        DeviceAudioSortSheet(
            currentSort = uiState.sortOrder,
            onSelect = {
                viewModel.setSortOrder(it)
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }

    if (showFilterSheet) {
        DeviceAudioScanFilterSheet(
            uiState = uiState,
            unknownArtist = unknownArtist,
            onRescan = viewModel::rescan,
            onMinDurationChange = viewModel::setMinDurationSeconds,
            onMinSizeChange = viewModel::setMinSizeBytes,
            onIgnoreMessagingChange = viewModel::setIgnoreSystemAndMessagingFolders,
            onHideFolder = viewModel::hideFolder,
            onUnhideFolder = viewModel::unhideFolder,
            onUnhideTrack = viewModel::unhideTrack,
            onRestoreAllHiddenTracks = viewModel::restoreAllHiddenTracks,
            onDismiss = { showFilterSheet = false },
        )
    }

    selectedDetailsTrack?.let { track ->
        DeviceAudioTechnicalDetailsDialog(
            track = track,
            details = technicalDetails,
            isLoading = isLoadingDetails,
            onDismiss = viewModel::clearTechnicalDetails,
        )
    }

    pendingLegacyDeleteTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { pendingLegacyDeleteTrack = null },
            title = { Text(stringResource(Res.string.device_audio_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.device_audio_delete_confirm_body, track.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingLegacyDeleteTrack = null
                        scope.launch {
                            val deleted = deleteLegacyDeviceAudioTrack(track)
                            if (deleted) {
                                viewModel.onTrackDeletedFromDevice(track.mediaStoreId)
                                viewModel.makeToast(getString(Res.string.device_audio_deleted_toast))
                            } else {
                                viewModel.makeToast(getString(Res.string.device_audio_delete_failed_toast))
                            }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLegacyDeleteTrack = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun DeviceAudioSubTabChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = "$label ($count)",
                maxLines = 1,
            )
        },
    )
}

@Composable
private fun DeviceAudioTrackRow(
    track: DeviceAudioTrack,
    unknownArtist: String,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val techLabel =
        remember(track.mediaStoreId, track.mimeType, track.displayName) {
            formatAudioTechnicalLabel(
                mimeType = track.mimeType,
                displayName = track.displayName,
                bitrateKbps = track.bitrateKbps,
                sampleRateHz = track.sampleRateHz,
            )
        }
    val subtitle =
        buildString {
            append(track.artist.ifBlank { unknownArtist })
            if (techLabel.isNotBlank()) {
                append(" · ")
                append(techLabel)
            }
            append(" · ")
            append(formatDurationMs(track.durationMs))
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceAudioArtwork(
            artworkUri = track.albumArtUri,
            fallbackIcon = SimpIcons.MusicNote,
            circular = false,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = typo().bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = SimpIcons.MoreVert,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun DeviceAudioCollectionRow(
    title: String,
    subtitle: String,
    artworkUri: String?,
    fallbackIcon: ImageVector,
    circularArtwork: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceAudioArtwork(
            artworkUri = artworkUri,
            fallbackIcon = fallbackIcon,
            circular = circularArtwork,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typo().bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = SimpIcons.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun DeviceAudioArtwork(
    artworkUri: String?,
    fallbackIcon: ImageVector,
    circular: Boolean,
) {
    val shape = if (circular) CircleShape else ArtworkShape
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = artworkUri,
                placeholder = rememberVectorPainter(fallbackIcon),
                error = rememberVectorPainter(fallbackIcon),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun DeviceAudioPermissionView(
    contentPadding: PaddingValues,
    permissionAskedOnce: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = SimpIcons.Smartphone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = stringResource(Res.string.device_audio_permission_title),
                style = typo().titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.device_audio_permission_description),
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onRequestPermission) {
                Text(stringResource(Res.string.device_audio_grant_permission))
            }
            if (permissionAskedOnce) {
                OutlinedButton(onClick = onOpenSettings) {
                    Text(stringResource(Res.string.device_audio_open_settings))
                }
            }
        }
    }
}

@Composable
private fun DeviceAudioEmptyView(
    onRescan: () -> Unit,
    onOpenFilters: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = SimpIcons.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(Res.string.device_audio_empty_title),
            style = typo().titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.device_audio_empty_subtitle),
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onRescan) {
                Icon(SimpIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.device_audio_rescan))
            }
            OutlinedButton(onClick = onOpenFilters) {
                Icon(SimpIcons.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.device_audio_scan_filters))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceAudioTrackMenuSheet(
    track: DeviceAudioTrack,
    unknownArtist: String,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onShare: () -> Unit,
    onTechnicalDetails: () -> Unit,
    onHideTrack: () -> Unit,
    onHideFolder: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeviceAudioArtwork(
                    artworkUri = track.albumArtUri,
                    fallbackIcon = SimpIcons.MusicNote,
                    circular = false,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = typo().titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${track.artist.ifBlank { unknownArtist }} · ${track.folderName}",
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SheetMenuItem(
                icon = SimpIcons.PlaylistAdd,
                label = stringResource(Res.string.play_next),
                onClick = onPlayNext,
            )
            SheetMenuItem(
                icon = SimpIcons.Share,
                label = stringResource(Res.string.device_audio_share_file),
                onClick = onShare,
            )
            SheetMenuItem(
                icon = SimpIcons.Info,
                label = stringResource(Res.string.device_audio_technical_details),
                onClick = onTechnicalDetails,
            )
            SheetMenuItem(
                icon = SimpIcons.Close,
                label = stringResource(Res.string.device_audio_hide_track),
                onClick = onHideTrack,
            )
            SheetMenuItem(
                icon = SimpIcons.FormatListBulleted,
                label = stringResource(Res.string.device_audio_hide_folder, track.folderName),
                onClick = onHideFolder,
            )
            SheetMenuItem(
                icon = SimpIcons.Delete,
                label = stringResource(Res.string.device_audio_delete_from_device),
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SheetMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = typo().bodyLarge,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceAudioSortSheet(
    currentSort: DeviceAudioSortOrder,
    onSelect: (DeviceAudioSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = stringResource(Res.string.sort),
                style = typo().titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            val options =
                listOf(
                    DeviceAudioSortOrder.TITLE_ASC to stringResource(Res.string.device_audio_sort_title_az),
                    DeviceAudioSortOrder.DATE_ADDED_DESC to stringResource(Res.string.device_audio_sort_recent),
                    DeviceAudioSortOrder.ARTIST_ASC to stringResource(Res.string.device_audio_sort_artist),
                    DeviceAudioSortOrder.ALBUM_ASC to stringResource(Res.string.device_audio_sort_album),
                    DeviceAudioSortOrder.DURATION_DESC to stringResource(Res.string.device_audio_sort_duration),
                )
            options.forEach { (order, label) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(order) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = currentSort == order,
                        onClick = { onSelect(order) },
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = label,
                        style = typo().bodyLarge,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceAudioScanFilterSheet(
    uiState: com.maxrave.simpmusic.viewModel.DeviceAudioUiState,
    unknownArtist: String,
    onRescan: () -> Unit,
    onMinDurationChange: (Int) -> Unit,
    onMinSizeChange: (Long) -> Unit,
    onIgnoreMessagingChange: (Boolean) -> Unit,
    onHideFolder: (String) -> Unit,
    onUnhideFolder: (String) -> Unit,
    onUnhideTrack: (Long) -> Unit,
    onRestoreAllHiddenTracks: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val discoveredFolders =
        remember(uiState.rawTracks) {
            uiState.rawTracks
                .groupBy { DeviceAudioLibraryEngine.normalizeFolderPath(it.folderPath) }
                .map { (path, tracks) ->
                    Triple(path, DeviceAudioLibraryEngine.folderDisplayName(path), tracks.size)
                }.sortedBy { it.second.lowercase() }
        }
    val hiddenFolderSet =
        remember(uiState.filterConfig.hiddenFolderPaths) {
            uiState.filterConfig.hiddenFolderPaths.map(DeviceAudioLibraryEngine::normalizeFolderPath).toSet()
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.device_audio_scan_filters),
                style = typo().titleMedium,
            )

            Button(
                onClick = onRescan,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) {
                Icon(SimpIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.device_audio_rescan))
            }

            HorizontalDivider()

            Text(
                text = stringResource(Res.string.device_audio_min_duration),
                style = typo().labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 30, 60).forEach { sec ->
                    val label = if (sec == 0) stringResource(Res.string.device_audio_filter_off) else "${sec}s"
                    FilterChip(
                        selected = uiState.filterConfig.minDurationSeconds == sec,
                        onClick = { onMinDurationChange(sec) },
                        label = { Text(label) },
                    )
                }
            }

            Text(
                text = stringResource(Res.string.device_audio_min_size),
                style = typo().labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val offLabel = stringResource(Res.string.device_audio_filter_off)
                listOf(
                    0L to offLabel,
                    100L * 1024L to "100 KB",
                    500L * 1024L to "500 KB",
                ).forEach { (bytes, label) ->
                    FilterChip(
                        selected = uiState.filterConfig.minSizeBytes == bytes,
                        onClick = { onMinSizeChange(bytes) },
                        label = { Text(label) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.device_audio_ignore_messaging_folders),
                    style = typo().bodyMedium,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Switch(
                    checked = uiState.filterConfig.ignoreSystemAndMessagingFolders,
                    onCheckedChange = onIgnoreMessagingChange,
                )
            }

            HorizontalDivider()

            Text(
                text = stringResource(Res.string.device_audio_manage_folders),
                style = typo().titleSmall,
            )
            discoveredFolders.forEach { (path, name, count) ->
                val isHidden = path in hiddenFolderSet
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$name ($count)",
                            style = typo().bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = path,
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Switch(
                        checked = !isHidden,
                        onCheckedChange = { visible ->
                            if (visible) onUnhideFolder(path) else onHideFolder(path)
                        },
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.device_audio_hidden_tracks, uiState.hiddenTracks.size),
                    style = typo().titleSmall,
                )
                if (uiState.hiddenTracks.isNotEmpty()) {
                    TextButton(onClick = onRestoreAllHiddenTracks) {
                        Text(stringResource(Res.string.device_audio_restore_all))
                    }
                }
            }

            if (uiState.hiddenTracks.isEmpty()) {
                Text(
                    text = stringResource(Res.string.device_audio_no_hidden_tracks),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.hiddenTracks.forEach { hiddenTrack ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = hiddenTrack.title,
                                style = typo().bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = hiddenTrack.artist.ifBlank { unknownArtist },
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { onUnhideTrack(hiddenTrack.mediaStoreId) }) {
                            Text(stringResource(Res.string.device_audio_restore))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAudioTechnicalDetailsDialog(
    track: DeviceAudioTrack,
    details: DeviceAudioTechnicalDetails?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = track.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLoading && details == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                val codecText =
                    details?.codecLabel
                        ?: details?.mimeType
                        ?: track.mimeType
                        ?: "—"
                val bitrateText =
                    details?.bitrateKbps?.let { "$it kbps" } ?: "—"
                val sampleRateText =
                    details?.sampleRateHz?.let { hz ->
                        if (hz % 1000 == 0) "${hz / 1000} kHz" else "$hz Hz"
                    } ?: "—"

                DetailRow(
                    label = stringResource(Res.string.device_audio_detail_file_name),
                    value = details?.displayName ?: track.displayName,
                )
                DetailRow(
                    label = stringResource(Res.string.device_audio_detail_path),
                    value = details?.fullPathOrRelative ?: track.relativePath,
                )
                DetailRow(
                    label = stringResource(Res.string.device_audio_detail_format),
                    value = codecText,
                )
                DetailRow(
                    label = stringResource(Res.string.device_audio_detail_bitrate),
                    value = bitrateText,
                )
                DetailRow(
                    label = stringResource(Res.string.device_audio_detail_sample_rate),
                    value = sampleRateText,
                )
                DetailRow(
                    label = stringResource(Res.string.device_audio_detail_size),
                    value = formatFileSize(details?.sizeBytes ?: track.sizeBytes),
                )
                DetailRow(
                    label = stringResource(Res.string.device_audio_detail_duration),
                    value = formatDurationMs(details?.durationMs ?: track.durationMs),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        },
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = typo().labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = typo().bodyMedium,
        )
    }
}
