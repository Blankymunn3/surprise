package kr.surprise.memorymap.feature.map

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kr.surprise.memorymap.core.common.Outcome
import kr.surprise.memorymap.core.model.CoverKey
import kr.surprise.memorymap.core.model.SpaceId
import kr.surprise.memorymap.core.ui.MviViewModel
import kr.surprise.memorymap.domain.model.PhotoBoard
import kr.surprise.memorymap.domain.repository.RegionCatalog
import kr.surprise.memorymap.domain.usecase.ObservePhotoBoardUseCase
import kr.surprise.memorymap.domain.usecase.SearchRegionsUseCase
import kr.surprise.memorymap.domain.usecase.SetCoverPhotoUseCase

class MapViewModel(
    spaceId: SpaceId,
    observeBoard: ObservePhotoBoardUseCase,
    private val searchRegions: SearchRegionsUseCase,
    private val setCover: SetCoverPhotoUseCase,
    private val regions: RegionCatalog,
) : MviViewModel<MapIntent, MapState, MapEffect>(MapState(spaceId)) {

    private var board: PhotoBoard = PhotoBoard.Empty

    init {
        observeBoard(spaceId)
            .onEach { board = it; rebuildPins() }
            .launchIn(viewModelScope)
    }

    /**
     * 지역 하나를 여는 길은 **한 곳뿐**입니다 — 검색으로 고르든 지도를 누르든 같습니다.
     * 가운데 좌표와 경계선을 함께 받아 옵니다.
     */
    private suspend fun open(region: Region) {
        val center = regions.centerOf(region.code)
        val outline = RegionOutline(region.code.value, regions.outlineOf(region.code))
        setState { MapReducer.regionOpened(this, region, board, center, outline) }
    }

    override fun onIntent(intent: MapIntent) {
        when (intent) {
            is MapIntent.QueryTyped -> viewModelScope.launch {
                val results = if (intent.value.isBlank()) emptyList() else searchRegions(intent.value)
                setState { MapReducer.queryTyped(this, intent.value, results) }
            }

            MapIntent.QueryCleared -> setState { MapReducer.queryCleared(this) }

            is MapIntent.RegionChosen -> viewModelScope.launch {
                open(intent.region)
            }

            // 지도를 누르면 그 좌표가 어느 지역인지 기기 안에서 판정합니다 (사진 EXIF 와 같은 길)
            is MapIntent.MapTapped -> viewModelScope.launch {
                val region = regions.regionAt(intent.latitude, intent.longitude)
                if (region == null) {
                    sendEffect(MapEffect.ShowMessage("여기는 아직 지역을 알 수 없어요."))
                } else {
                    open(region)
                }
            }

            is MapIntent.PhotoTapped -> setState { MapReducer.photoSelected(this, intent.id) }

            MapIntent.SetCoverTapped -> {
                val state = currentState()
                val sheet = state.sheet
                val picked = sheet?.selected
                if (sheet == null || picked == null || !state.canSetCover()) return
                viewModelScope.launch {
                    val result = setCover(state.spaceId, CoverKey.ForRegion(sheet.region.code), picked)
                    when (result) {
                        is Outcome.Ok -> setState { MapReducer.coverChanged(this, picked) }
                        is Outcome.Fail -> sendEffect(MapEffect.ShowMessage("대표사진을 바꾸지 못했어요."))
                    }
                }
            }

            MapIntent.AddPhotoTapped -> sendEffect(MapEffect.OpenUpload)
            MapIntent.SheetDismissed -> setState { MapReducer.sheetDismissed(this) }
            MapIntent.MyLocationTapped -> sendEffect(MapEffect.AskMyLocation)
        }
    }

    private fun rebuildPins() {
        viewModelScope.launch {
            val pins = board.byRegion.mapNotNull { (code, photos) ->
                val region = regions.find(code.value) ?: return@mapNotNull null
                val center = regions.centerOf(code) ?: return@mapNotNull null
                RegionPin(
                    region = region,
                    latitude = center[0],
                    longitude = center[1],
                    coverUrl = board.regionCover(code)?.downloadUrl,
                    photoCount = photos.size,
                )
            }
            setState { MapReducer.pinsRebuilt(this, pins) }
        }
    }
}
