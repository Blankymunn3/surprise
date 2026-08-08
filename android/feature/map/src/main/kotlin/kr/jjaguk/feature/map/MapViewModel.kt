package kr.jjaguk.feature.map

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kr.jjaguk.core.common.Outcome
import kr.jjaguk.core.model.CoverKey
import kr.jjaguk.core.model.Region
import kr.jjaguk.core.model.SpaceId
import kr.jjaguk.core.ui.MviViewModel
import kr.jjaguk.domain.model.PhotoBoard
import kr.jjaguk.domain.repository.RegionCatalog
import kr.jjaguk.domain.usecase.ObservePhotoBoardUseCase
import kr.jjaguk.domain.usecase.SearchRegionsUseCase
import kr.jjaguk.domain.usecase.SetCoverPhotoUseCase

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
     * 경계선을 받아 와 테두리로도 쓰고, 지도를 맞출 범위로도 씁니다.
     *
     * 경계가 있으면 **그 경계가 다 들어오게** 맞춥니다. 경계가 없는 장소만 가운데 좌표에
     * 배율을 정해 세웁니다 — 맞출 넓이가 없으니까요.
     */
    private suspend fun open(region: Region) {
        val polygons = regions.shapeOf(region.code)
        val outline = RegionOutline(region.code.value, polygons)
        val focus = boundsOf(polygons)
            ?: regions.centerOf(region.code)?.let { MapFocus.Spot(it[0], it[1]) }
        setState { MapReducer.regionOpened(this, region, board, focus, outline) }
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
                    sendEffect(MapEffect.ShowMessage(MapMessage.RegionUnknown))
                } else {
                    open(region)
                }
            }

            // 누르면 **바로** 대표가 됩니다. 이미 대표인 것을 또 누르면 아무 일도
            // 없습니다 — 같은 값을 서버에 다시 쓸 까닭이 없습니다.
            is MapIntent.PhotoTapped -> {
                val state = currentState()
                val sheet = state.sheet
                if (sheet == null || sheet.coverId == intent.id) return
                viewModelScope.launch {
                    val result = setCover(state.spaceId, CoverKey.ForRegion(sheet.region.code), intent.id)
                    when (result) {
                        is Outcome.Ok -> setState { MapReducer.coverChanged(this, intent.id) }
                        is Outcome.Fail -> sendEffect(MapEffect.ShowMessage(MapMessage.CoverFailed))
                    }
                }
            }

            MapIntent.AddPhotoTapped ->
                sendEffect(MapEffect.OpenUpload(currentState().sheet?.region))
            MapIntent.SheetDismissed -> setState { MapReducer.sheetDismissed(this) }
            MapIntent.MyLocationTapped -> sendEffect(MapEffect.AskMyLocation)
            is MapIntent.MyLocationFound ->
                setState { MapReducer.movedToMyLocation(this, intent.latitude, intent.longitude) }
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

            // 다녀온 지역을 그 지역의 대표사진으로 칠합니다. 사진이 없는 지역은
            // 칠할 것이 없으니 건너뜁니다 — 표시(핀)만 찍힙니다.
            val fills = pins.mapNotNull { pin ->
                val cover = pin.coverUrl ?: return@mapNotNull null
                val polygons = regions.shapeOf(pin.region.code)
                if (polygons.isEmpty()) null
                else RegionFill(pin.region.code.value, cover, polygons)
            }
            setState { MapReducer.fillsRebuilt(this, fills) }
        }
    }
}
