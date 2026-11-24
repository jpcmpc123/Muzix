package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.AutoEqEntryEntity
import com.maxrave.domain.data.player.AutoEqLayer
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AutoEqRepository
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the picker is doing, so it can say so instead of looking empty. */
enum class AutoEqStatus {
    /** Showing whatever is cached; any refresh is happening quietly behind it. */
    READY,

    /** Nothing cached yet, so the first download has to finish before there is a list. */
    DOWNLOADING,

    /** The download failed and there is nothing cached to fall back on. */
    UNAVAILABLE,

    /** A profile was picked and its curve is on the way. */
    APPLYING,
}

class AutoEqViewModel(
    private val autoEqRepository: AutoEqRepository,
    private val dataStoreManager: DataStoreManager,
) : BaseViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<AutoEqEntryEntity>>(emptyList())
    val results: StateFlow<List<AutoEqEntryEntity>> = _results

    private val _status = MutableStateFlow(AutoEqStatus.READY)
    val status: StateFlow<AutoEqStatus> = _status

    /** Profiles whose curve is already on disk, so the list can mark what works without a network. */
    private val _cachedPaths = MutableStateFlow<Set<String>>(emptySet())
    val cachedPaths: StateFlow<Set<String>> = _cachedPaths

    private val _customProfiles = MutableStateFlow<List<AutoEqLayer>>(emptyList())
    val customProfiles: StateFlow<List<AutoEqLayer>> = _customProfiles

    init {
        viewModelScope.launch {
            launch {
                _query.collectLatest { term ->
                    delay(SEARCH_DEBOUNCE_MS)
                    _results.emit(autoEqRepository.search(term))
                }
            }
            launch {
                dataStoreManager.equalizerCustomAutoEqProfiles.collect {
                    _customProfiles.emit(it)
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onOpen() {
        viewModelScope.launch {
            _cachedPaths.value = autoEqRepository.cachedCurvePaths()
            val cached = autoEqRepository.cachedCount()
            _status.value = if (cached == 0) AutoEqStatus.DOWNLOADING else AutoEqStatus.READY
            if (cached > 0) _results.emit(autoEqRepository.search(_query.value))

            val changed = autoEqRepository.refreshIndex()
            if (changed) _results.emit(autoEqRepository.search(_query.value))
            _status.value =
                if (autoEqRepository.cachedCount() == 0) AutoEqStatus.UNAVAILABLE else AutoEqStatus.READY
        }
    }

    /**
     * Loads the headphone's ParametricEQ (or FixedBand fallback) into the independent AutoEq layer
     * so user EQ bands, presets, and tone enhancements are preserved intact.
     */
    fun apply(
        entry: AutoEqEntryEntity,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            _status.value = AutoEqStatus.APPLYING
            val label = labelFor(entry)
            val layer = autoEqRepository.loadAutoEqLayer(entry, label)
            if (layer == null) {
                _status.value = AutoEqStatus.READY
                onResult(false)
                return@launch
            }
            dataStoreManager.setEqualizerAutoEqLayer(layer)
            if (dataStoreManager.equalizerEnabled.first() != DataStoreManager.TRUE) {
                dataStoreManager.setEqualizerEnabled(true)
            }
            _cachedPaths.value = autoEqRepository.cachedCurvePaths()
            _status.value = AutoEqStatus.READY
            onResult(true)
        }
    }

    fun importCustomText(
        name: String,
        rawText: String,
    ): Boolean {
        val parsed = autoEqRepository.parseCustomAutoEqText(name, rawText) ?: return false
        viewModelScope.launch {
            val nextCustom = _customProfiles.value + parsed
            dataStoreManager.setEqualizerCustomAutoEqProfiles(nextCustom)
            dataStoreManager.setEqualizerAutoEqLayer(parsed)
            if (dataStoreManager.equalizerEnabled.first() != DataStoreManager.TRUE) {
                dataStoreManager.setEqualizerEnabled(true)
            }
        }
        return true
    }

    fun applyCustomProfile(layer: AutoEqLayer) {
        viewModelScope.launch {
            dataStoreManager.setEqualizerAutoEqLayer(layer.copy(enabled = true))
            if (dataStoreManager.equalizerEnabled.first() != DataStoreManager.TRUE) {
                dataStoreManager.setEqualizerEnabled(true)
            }
        }
    }

    fun deleteCustomProfile(path: String) {
        viewModelScope.launch {
            dataStoreManager.setEqualizerCustomAutoEqProfiles(
                _customProfiles.value.filterNot { it.path == path },
            )
            if (dataStoreManager.equalizerAutoEqLayer.first()?.path == path) {
                dataStoreManager.setEqualizerAutoEqLayer(null)
            }
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 200L

        fun labelFor(entry: AutoEqEntryEntity): String = "${entry.name} · ${entry.source}"
    }
}
