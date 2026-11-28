package com.maxrave.simpmusic.viewModel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.LOCAL_DEVICE_PLAYLIST_ID
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.simpmusic.expect.hasDeviceAudioPermission
import com.maxrave.simpmusic.expect.readDeviceAudioTechnicalDetails
import com.maxrave.simpmusic.expect.scanDeviceAudioTracks
import com.maxrave.simpmusic.localaudio.DeviceAudioAlbum
import com.maxrave.simpmusic.localaudio.DeviceAudioArtist
import com.maxrave.simpmusic.localaudio.DeviceAudioFilterConfig
import com.maxrave.simpmusic.localaudio.DeviceAudioFolder
import com.maxrave.simpmusic.localaudio.DeviceAudioLibraryEngine
import com.maxrave.simpmusic.localaudio.DeviceAudioSortOrder
import com.maxrave.simpmusic.localaudio.DeviceAudioSubTab
import com.maxrave.simpmusic.localaudio.DeviceAudioTechnicalDetails
import com.maxrave.simpmusic.localaudio.DeviceAudioTrack
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

private const val DEVICE_AUDIO_FILTER_CONFIG_KEY = "device_audio_filter_config_v1"
private const val DEVICE_AUDIO_SORT_ORDER_KEY = "device_audio_sort_order_v1"

@Immutable
data class DeviceAudioUiState(
    val hasPermission: Boolean = false,
    val isScanning: Boolean = false,
    val hasScannedOnce: Boolean = false,
    val selectedSubTab: DeviceAudioSubTab = DeviceAudioSubTab.SONGS,
    val searchQuery: String = "",
    val sortOrder: DeviceAudioSortOrder = DeviceAudioSortOrder.TITLE_ASC,
    val filterConfig: DeviceAudioFilterConfig = DeviceAudioFilterConfig(),
    val rawTracks: List<DeviceAudioTrack> = emptyList(),
    val visibleTracks: List<DeviceAudioTrack> = emptyList(),
    val folders: List<DeviceAudioFolder> = emptyList(),
    val albums: List<DeviceAudioAlbum> = emptyList(),
    val artists: List<DeviceAudioArtist> = emptyList(),
    val hiddenTracks: List<DeviceAudioTrack> = emptyList(),
)

class DeviceAudioViewModel(
    private val dataStoreManager: DataStoreManager,
) : BaseViewModel() {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val _hasPermission = MutableStateFlow(hasDeviceAudioPermission())
    private val _isScanning = MutableStateFlow(false)
    private val _hasScannedOnce = MutableStateFlow(false)
    private val _selectedSubTab = MutableStateFlow(DeviceAudioSubTab.SONGS)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(DeviceAudioSortOrder.TITLE_ASC)
    private val _filterConfig = MutableStateFlow(DeviceAudioFilterConfig())
    private val _rawTracks = MutableStateFlow<List<DeviceAudioTrack>>(emptyList())

    private val _selectedDetailsTrack = MutableStateFlow<DeviceAudioTrack?>(null)
    val selectedDetailsTrack: StateFlow<DeviceAudioTrack?> = _selectedDetailsTrack.asStateFlow()

    private val _technicalDetails = MutableStateFlow<DeviceAudioTechnicalDetails?>(null)
    val technicalDetails: StateFlow<DeviceAudioTechnicalDetails?> = _technicalDetails.asStateFlow()

    private val _isLoadingDetails = MutableStateFlow(false)
    val isLoadingDetails: StateFlow<Boolean> = _isLoadingDetails.asStateFlow()

    private data class ControlInputs(
        val hasPermission: Boolean,
        val isScanning: Boolean,
        val hasScannedOnce: Boolean,
        val selectedSubTab: DeviceAudioSubTab,
        val searchQuery: String,
    )

    private val controlInputsFlow =
        combine(
            _hasPermission,
            _isScanning,
            _hasScannedOnce,
            _selectedSubTab,
            _searchQuery,
        ) { hasPermission, isScanning, hasScannedOnce, selectedSubTab, searchQuery ->
            ControlInputs(
                hasPermission = hasPermission,
                isScanning = isScanning,
                hasScannedOnce = hasScannedOnce,
                selectedSubTab = selectedSubTab,
                searchQuery = searchQuery,
            )
        }

    val uiState: StateFlow<DeviceAudioUiState> =
        combine(
            controlInputsFlow,
            _sortOrder,
            _filterConfig,
            _rawTracks,
        ) { controls, sortOrder, filterConfig, rawTracks ->
            val filtered = DeviceAudioLibraryEngine.applyFilters(rawTracks, filterConfig)
            val searched = DeviceAudioLibraryEngine.searchTracks(filtered, controls.searchQuery)
            val sortedTracks = DeviceAudioLibraryEngine.sortTracks(searched, sortOrder)
            val folders = DeviceAudioLibraryEngine.groupByFolder(searched, sortOrder)
            val albums = DeviceAudioLibraryEngine.groupByAlbum(searched)
            val artists = DeviceAudioLibraryEngine.groupByArtist(searched, sortOrder)
            val hiddenTracks =
                rawTracks.filter { it.mediaStoreId in filterConfig.hiddenMediaStoreIds }

            DeviceAudioUiState(
                hasPermission = controls.hasPermission,
                isScanning = controls.isScanning,
                hasScannedOnce = controls.hasScannedOnce,
                selectedSubTab = controls.selectedSubTab,
                searchQuery = controls.searchQuery,
                sortOrder = sortOrder,
                filterConfig = filterConfig,
                rawTracks = rawTracks,
                visibleTracks = sortedTracks,
                folders = folders,
                albums = albums,
                artists = artists,
                hiddenTracks = hiddenTracks,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceAudioUiState(hasPermission = _hasPermission.value),
        )

    init {
        viewModelScope.launch {
            dataStoreManager.getString(DEVICE_AUDIO_FILTER_CONFIG_KEY).first()?.let { raw ->
                runCatching { json.decodeFromString<DeviceAudioFilterConfig>(raw) }
                    .getOrNull()
                    ?.let { _filterConfig.value = it }
            }
            _sortOrder.value =
                DeviceAudioSortOrder.fromStored(
                    dataStoreManager.getString(DEVICE_AUDIO_SORT_ORDER_KEY).first(),
                )
            syncPermissionAndScanIfNeeded()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _hasPermission.value = granted || hasDeviceAudioPermission()
        if (_hasPermission.value) {
            rescan()
        }
    }

    fun syncPermissionAndScanIfNeeded() {
        val granted = hasDeviceAudioPermission()
        _hasPermission.value = granted
        if (granted && !_hasScannedOnce.value && !_isScanning.value) {
            rescan()
        }
    }

    fun rescan() {
        if (!hasDeviceAudioPermission()) {
            _hasPermission.value = false
            return
        }
        _hasPermission.value = true
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val scanned = scanDeviceAudioTracks()
                _rawTracks.value = scanned
                _hasScannedOnce.value = true
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun selectSubTab(tab: DeviceAudioSubTab) {
        _selectedSubTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: DeviceAudioSortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            dataStoreManager.putString(DEVICE_AUDIO_SORT_ORDER_KEY, order.name)
        }
    }

    fun setMinDurationSeconds(seconds: Int) {
        updateFilterConfig { it.copy(minDurationSeconds = seconds) }
    }

    fun setMinSizeBytes(bytes: Long) {
        updateFilterConfig { it.copy(minSizeBytes = bytes) }
    }

    fun setIgnoreSystemAndMessagingFolders(ignore: Boolean) {
        updateFilterConfig { it.copy(ignoreSystemAndMessagingFolders = ignore) }
    }

    fun hideFolder(folderPath: String) {
        val normalized = DeviceAudioLibraryEngine.normalizeFolderPath(folderPath)
        updateFilterConfig { it.copy(hiddenFolderPaths = it.hiddenFolderPaths + normalized) }
    }

    fun unhideFolder(folderPath: String) {
        val normalized = DeviceAudioLibraryEngine.normalizeFolderPath(folderPath)
        updateFilterConfig { it.copy(hiddenFolderPaths = it.hiddenFolderPaths - normalized) }
    }

    fun hideTrack(mediaStoreId: Long) {
        updateFilterConfig { it.copy(hiddenMediaStoreIds = it.hiddenMediaStoreIds + mediaStoreId) }
    }

    fun unhideTrack(mediaStoreId: Long) {
        updateFilterConfig { it.copy(hiddenMediaStoreIds = it.hiddenMediaStoreIds - mediaStoreId) }
    }

    fun restoreAllHiddenTracks() {
        updateFilterConfig { it.copy(hiddenMediaStoreIds = emptySet()) }
    }

    fun onTrackDeletedFromDevice(mediaStoreId: Long) {
        _rawTracks.value = _rawTracks.value.filterNot { it.mediaStoreId == mediaStoreId }
        updateFilterConfig { it.copy(hiddenMediaStoreIds = it.hiddenMediaStoreIds - mediaStoreId) }
    }

    fun playTracks(
        tracks: List<DeviceAudioTrack>,
        startIndex: Int = 0,
        queueTitle: String,
        shuffle: Boolean = false,
        unknownArtistFallback: String = "Unknown artist",
    ) {
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            val orderedTracks =
                if (shuffle) {
                    val shuffled = tracks.toMutableList()
                    if (shuffled.size > 1) {
                        val firstIdx = Random.nextInt(shuffled.size)
                        val picked = shuffled.removeAt(firstIdx)
                        shuffled.shuffle()
                        shuffled.add(0, picked)
                    }
                    shuffled
                } else {
                    tracks
                }
            val safeIndex = if (shuffle) 0 else startIndex.coerceIn(0, orderedTracks.lastIndex)
            val mappedTracks = orderedTracks.map { it.toTrack(unknownArtistFallback) }
            val firstTrack = mappedTracks[safeIndex]
            mediaPlayerHandler.setQueueData(
                QueueData.Data(
                    listTracks = ArrayList(mappedTracks),
                    firstPlayedTrack = firstTrack,
                    playlistId = LOCAL_DEVICE_PLAYLIST_ID,
                    playlistName = queueTitle,
                    playlistType = PlaylistType.LOCAL_PLAYLIST,
                    continuation = null,
                ),
            )
            mediaPlayerHandler.loadMediaItem(
                firstTrack,
                Config.PLAYLIST_CLICK,
                safeIndex,
            )
        }
    }

    fun playNext(
        track: DeviceAudioTrack,
        unknownArtistFallback: String = "Unknown artist",
    ) {
        viewModelScope.launch {
            mediaPlayerHandler.playNext(track.toTrack(unknownArtistFallback))
        }
    }

    fun loadTechnicalDetails(track: DeviceAudioTrack) {
        _selectedDetailsTrack.value = track
        _technicalDetails.value = null
        _isLoadingDetails.value = true
        viewModelScope.launch {
            try {
                _technicalDetails.value = readDeviceAudioTechnicalDetails(track)
            } finally {
                _isLoadingDetails.value = false
            }
        }
    }

    fun clearTechnicalDetails() {
        _selectedDetailsTrack.value = null
        _technicalDetails.value = null
        _isLoadingDetails.value = false
    }

    private fun updateFilterConfig(transform: (DeviceAudioFilterConfig) -> DeviceAudioFilterConfig) {
        val updated = transform(_filterConfig.value)
        _filterConfig.value = updated
        viewModelScope.launch {
            runCatching { json.encodeToString(updated) }.getOrNull()?.let { encoded ->
                dataStoreManager.putString(DEVICE_AUDIO_FILTER_CONFIG_KEY, encoded)
            }
        }
    }
}
