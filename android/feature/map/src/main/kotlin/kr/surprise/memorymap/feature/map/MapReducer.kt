package kr.surprise.memorymap.feature.map

import kr.surprise.memorymap.core.model.PhotoId
import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.domain.model.PhotoBoard

internal object MapReducer {

    fun fillsRebuilt(state: MapState, fills: List<RegionFill>): MapState =
        state.copy(fills = fills)

    fun pinsRebuilt(state: MapState, pins: List<RegionPin>): MapState =
        state.copy(pins = pins)

    fun queryTyped(state: MapState, value: String, results: List<Region>): MapState =
        state.copy(query = value, results = results)

    fun queryCleared(state: MapState): MapState =
        state.copy(query = "", results = emptyList(), sheet = null)

    /**
     * 지역을 고르면 그 지역에 지도를 맞추고 아래에서 시트가 올라옵니다.
     * 검색창은 고른 지역 이름으로 **채워집니다**.
     */
    fun regionOpened(
        state: MapState,
        region: Region,
        board: PhotoBoard,
        focus: MapFocus?,
        outline: RegionOutline?,
    ): MapState {
        val photos = board.photosIn(region.code)
        return state.copy(
            query = region.displayName,
            results = emptyList(),
            focus = focus,
            focusCount = state.focusCount + 1,
            outline = outline,
            sheet = RegionSheetUi(
                region = region,
                photos = photos,
                coverId = board.regionCover(region.code)?.id,
            ),
        )
    }

    fun coverChanged(state: MapState, id: PhotoId): MapState =
        state.copy(sheet = state.sheet?.copy(coverId = id))

    fun sheetDismissed(state: MapState): MapState =
        state.copy(sheet = null, query = "", results = emptyList())
}
