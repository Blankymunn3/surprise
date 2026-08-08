package kr.jjaguk.feature.map

import kr.jjaguk.core.model.PhotoId
import kr.jjaguk.core.model.Region
import kr.jjaguk.domain.model.PhotoBoard

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

    /**
     * 지금 자리로 지도를 옮깁니다.
     *
     * **시트는 건드리지 않습니다.** 지역을 보다가 "여기가 어디쯤이지" 하고 누르는
     * 일이라, 보던 시트가 닫히면 하던 일이 끊깁니다. 지역 테두리도 그대로 둡니다.
     *
     * [MapState.focusCount] 를 올려야 지도가 실제로 움직입니다 — 같은 자리에서 두 번
     * 눌러도 좌표가 같아서, 횟수를 세지 않으면 두 번째부터 꿈쩍도 안 합니다.
     */
    fun movedToMyLocation(state: MapState, latitude: Double, longitude: Double): MapState =
        state.copy(
            focus = MapFocus.Me(latitude, longitude),
            focusCount = state.focusCount + 1,
            myLocation = MyPin(latitude, longitude),
        )
}
