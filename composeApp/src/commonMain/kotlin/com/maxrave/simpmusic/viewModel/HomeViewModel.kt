package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.SUPPORTED_LANGUAGE
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.home.prioritizeQuickPicksFirst
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.music_video
import simpmusic.composeapp.generated.resources.new_release
import simpmusic.composeapp.generated.resources.song
import simpmusic.composeapp.generated.resources.view_count

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(arrayListOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList

    private var _homeListState = MutableStateFlow<ListState>(ListState.IDLE)
    val homeListState: StateFlow<ListState> = _homeListState

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private val _exploreMoodItem: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val exploreMoodItem: StateFlow<Mood?> = _exploreMoodItem
    private val _accountInfo: MutableStateFlow<Pair<String?, String?>?> = MutableStateFlow(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo

    private var homeJob: Job? = null
    private var paginationJob: Job? = null
    private var moodJob: Job? = null
    private var chartJob: Job? = null
    private var newReleaseJob: Job? = null

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableStateFlow<Chart?> = MutableStateFlow(null)
    val chart: StateFlow<Chart?> = _chart
    private val _newRelease: MutableStateFlow<List<HomeItem>> = MutableStateFlow(arrayListOf())
    val newRelease: StateFlow<List<HomeItem>> = _newRelease
    var regionCodeChart: MutableStateFlow<String?> = MutableStateFlow(null)

    val loading = MutableStateFlow<Boolean>(true)
    val loadingChart = MutableStateFlow<Boolean>(true)
    private var regionCode: String = ""
    private var language: String = ""

    private val _songEntity: MutableStateFlow<SongEntity?> = MutableStateFlow(null)
    val songEntity: StateFlow<SongEntity?> = _songEntity

    private var _params: MutableStateFlow<String?> = MutableStateFlow(null)
    val params: StateFlow<String?> = _params

    // For showing alert that should log in to YouTube
    private val _showLogInAlert: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLogInAlert: StateFlow<Boolean> = _showLogInAlert

    val dataSyncId =
        dataStoreManager
            .dataSyncId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _mainHomeThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val mainHomeThumbnail: StateFlow<String?> = _mainHomeThumbnail

    // The page background image YouTube sends with Home, null when it sends none.
    private val _homeBackground: MutableStateFlow<String?> = MutableStateFlow(null)
    val homeBackground: StateFlow<String?> = _homeBackground

    init {
        viewModelScope.launch {
            if (dataStoreManager.cookie.first().isEmpty() &&
                dataStoreManager.shouldShowLogInRequiredAlert.first() == TRUE
            ) {
                _showLogInAlert.update { true }
            }
            regionCodeChart.value = dataStoreManager.chartKey.first()
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                ?: SUPPORTED_LANGUAGE.codes.first()
            //  One single load trigger for Home. Region, language, the login cookie and
            //  the mood params each used to be collected by a separate job, and every job
            //  called getHomeItemList on its first emission — so opening the app fired
            //  the same load up to five times back to back: the feed showed, the loading
            //  state came back, and the feed showed again. Combined into one flow, the
            //  load runs once, and then only when one of the four actually changes.
            val job1 =
                launch {
                    var previousTrigger: HomeLoadTrigger? = null
                    combine(
                        dataStoreManager.location,
                        dataStoreManager.language,
                        dataStoreManager.cookie,
                        params,
                    ) { location, language, cookie, params ->
                        HomeLoadTrigger(location, language, cookie, params)
                    }.distinctUntilChanged().collectLatest { trigger ->
                        regionCode = trigger.location
                        language = trigger.language
                        val prev = previousTrigger
                        previousTrigger = trigger
                        val isFirstEmission = prev == null
                        val cookieChanged = !isFirstEmission && prev.cookie != trigger.cookie
                        val onlyParamsChanged =
                            prev != null &&
                                prev.location == trigger.location &&
                                prev.language == trigger.language &&
                                prev.cookie == trigger.cookie &&
                                prev.params != trigger.params
                        if (isFirstEmission || cookieChanged) {
                            _accountInfo.emit(
                                Pair(
                                    dataStoreManager.getString("AccountName").first(),
                                    dataStoreManager.getString("AccountThumbUrl").first(),
                                ),
                            )
                        }
                        if (cookieChanged && trigger.cookie.isNotEmpty()) {
                            Logger.w(tag, "Cookie changed, refreshing home")
                            loading.value = true
                            delay(1000) // To wait for the cookie to be saved properly
                        }
                        loadHomeFeed(
                            params = trigger.params,
                            includeSecondary = !onlyParamsChanged,
                        )
                    }
                }
            val job6 =
                launch {
                    homeItemList.collectLatest { list ->
                        _mainHomeThumbnail.value =
                            list
                                .firstOrNull()
                                ?.contents
                                ?.firstOrNull()
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url
                    }
                }
            job1.join()
            job6.join()
        }
    }

    fun doneShowLogInAlert(neverShowAgain: Boolean = false) {
        viewModelScope.launch {
            _showLogInAlert.update { false }
            if (neverShowAgain) {
                dataStoreManager.setShouldShowLogInRequiredAlert(false)
            }
        }
    }

    fun getHomeItemList(params: String? = null) {
        loadHomeFeed(params = params, includeSecondary = true)
    }

    private fun loadHomeFeed(
        params: String?,
        includeSecondary: Boolean,
    ) {
        loading.value = true
        _homeListState.value = ListState.LOADING
        homeJob?.cancel()
        paginationJob?.cancel()
        paginationJob = null
        homeJob =
            viewModelScope.launch {
                language =
                    dataStoreManager.getString(SELECTED_LANGUAGE).first()
                        ?: SUPPORTED_LANGUAGE.codes.first()
                regionCode = dataStoreManager.location.first()
                homeRepository
                    .getHomeData(
                        params,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ).collect { home ->
                        Logger.d("home size", "${home.data?.second?.size}")
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                _homeItemList.value = (home.data?.second ?: listOf()).prioritizeQuickPicksFirst()
                                _homeBackground.value = home.data?.third
                                _homeListState.value =
                                    if (continuation.value.isNullOrEmpty()) {
                                        ListState.PAGINATION_EXHAUST
                                    } else {
                                        ListState.IDLE
                                    }
                                if (dataStoreManager.cookie.first().isNotEmpty() &&
                                    _accountInfo.value?.first.isNullOrBlank()
                                ) {
                                    _accountInfo.emit(
                                        Pair(
                                            dataStoreManager.getString("AccountName").first(),
                                            dataStoreManager.getString("AccountThumbUrl").first(),
                                        ),
                                    )
                                }
                            }

                            is Resource.Error -> {
                                if (_homeItemList.value.isEmpty()) {
                                    _continuation.value = null
                                    _homeItemList.value = listOf()
                                    _homeListState.value = ListState.PAGINATION_EXHAUST
                                } else {
                                    _homeListState.value =
                                        if (_continuation.value.isNullOrEmpty()) {
                                            ListState.PAGINATION_EXHAUST
                                        } else {
                                            ListState.IDLE
                                        }
                                }
                                home.message?.let {
                                    showSnackBarErrorState.emit(it)
                                    Logger.w("Error", "getHomeItemList: $it")
                                }
                            }
                        }
                        loading.value = false
                    }
                if (includeSecondary) {
                    loadSecondarySections()
                }
            }
    }

    private fun loadSecondarySections() {
        moodJob?.cancel()
        moodJob =
            viewModelScope.launch {
                homeRepository.getMoodAndMomentsData().collect { exploreMoodItem ->
                    when (exploreMoodItem) {
                        is Resource.Success -> {
                            _exploreMoodItem.value = exploreMoodItem.data
                        }

                        is Resource.Error -> {
                            exploreMoodItem.message?.let {
                                Logger.w("Error", "getMoodAndMomentsData: $it")
                                if (_exploreMoodItem.value == null) {
                                    showSnackBarErrorState.emit(it)
                                }
                            }
                        }
                    }
                }
            }

        newReleaseJob?.cancel()
        newReleaseJob =
            viewModelScope.launch {
                homeRepository
                    .getNewRelease(
                        getString(Res.string.new_release),
                        getString(Res.string.music_video),
                    ).collect { newRelease ->
                        when (newRelease) {
                            is Resource.Success -> {
                                _newRelease.value = newRelease.data ?: arrayListOf()
                            }

                            is Resource.Error -> {
                                Logger.w("Error", "getNewRelease: ${newRelease.message}")
                            }
                        }
                    }
            }

        loadChart(region = null, persistRegion = false)
    }

    /** A shelf's "Play all": YouTube's own finite queue of that shelf's songs. */
    fun playAll(item: HomeItem) {
        item.playAllEndpoint?.let { playFromEndpoint(it, item.title) }
    }

    fun getContinueHomeItem(continuation: String?) {
        paginationJob?.cancel()
        paginationJob =
            viewModelScope.launch {
                if (continuation.isNullOrEmpty()) {
                    _homeListState.value = ListState.PAGINATION_EXHAUST
                    return@launch
                } else {
                    log("Get more home item with continuation: $continuation")
                    _homeListState.value = ListState.PAGINATING
                    homeRepository
                        .getHomeDataContinue(
                            continuation,
                            getString(Res.string.view_count),
                            getString(Res.string.song),
                        ).collect { home ->
                            when (home) {
                                is Resource.Success -> {
                                    _continuation.value = home.data?.first
                                    val newItems = home.data?.second ?: listOf()
                                    _homeItemList.update { (it + newItems).prioritizeQuickPicksFirst() }
                                    if (home.data?.first.isNullOrEmpty()) {
                                        _homeListState.value = ListState.PAGINATION_EXHAUST
                                    } else {
                                        _homeListState.value = ListState.IDLE
                                    }
                                }

                                is Resource.Error -> {
                                    _continuation.value = null
                                    Logger.w(tag, "getContinueHomeItem: ${home.message}")
                                    showSnackBarErrorState.emit(home.message ?: "Unknown error")
                                    _homeListState.value = ListState.PAGINATION_EXHAUST
                                }
                            }
                        }
                }
            }
    }

    fun exploreChart(region: String) {
        loadChart(region = region, persistRegion = true)
    }

    private fun loadChart(
        region: String?,
        persistRegion: Boolean,
    ) {
        chartJob?.cancel()
        lateinit var job: Job
        job =
            viewModelScope.launch(start = CoroutineStart.LAZY) {
                loadingChart.value = true
                try {
                    val targetRegion = region ?: dataStoreManager.chartKey.first()
                    regionCodeChart.value = targetRegion
                    homeRepository
                        .getChartData(
                            targetRegion,
                        ).collect { values ->
                            regionCodeChart.value = targetRegion
                            if (persistRegion) {
                                dataStoreManager.setChartKey(targetRegion)
                            }
                            when (values) {
                                is Resource.Success -> {
                                    _chart.value = values.data
                                }

                                is Resource.Error -> {
                                    if (persistRegion) {
                                        _chart.value = null
                                    }
                                    values.message?.let {
                                        Logger.w("Error", "getChartData: $it")
                                        if (_chart.value == null) {
                                            showSnackBarErrorState.emit(it)
                                        }
                                    }
                                }
                            }
                            loadingChart.value = false
                        }
                } finally {
                    if (chartJob === job) {
                        loadingChart.value = false
                    }
                }
            }
        chartJob = job
        job.start()
    }

    fun setParams(params: String?) {
        _params.value = params
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
        paginationJob?.cancel()
        moodJob?.cancel()
        chartJob?.cancel()
        newReleaseJob?.cancel()
    }

    companion object {
        // Home params
        const val HOME_PARAMS_RELAX = "ggM8SgQIBxADSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SLEEP = "ggM8SgQIBxABSgQIBRADSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ENERGIZE = "ggM8SgQIBxABSgQIBRABSgQICRADSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SAD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChADSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ROMANCE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRADSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_FEEL_GOOD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBADSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_WORKOUT = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBADSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_PARTY = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhADSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_COMMUTE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxADSgQIBhAB"
        const val HOME_PARAMS_FOCUS = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAD"
    }
}

/** One emission of the combined Home load trigger: every input that can change what Home shows. */
private data class HomeLoadTrigger(
    val location: String,
    val language: String,
    val cookie: String,
    val params: String?,
)