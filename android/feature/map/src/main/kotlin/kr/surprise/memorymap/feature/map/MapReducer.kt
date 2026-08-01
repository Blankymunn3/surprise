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
                selected = null,
            ),
        )
    }

    fun photoSelected(state: MapState, id: PhotoId): MapState =
        state.copy(sheet = state.sheet?.copy(selected = if (state.sheet.selected == id) null else id))

    fun coverChanged(state: MapState, id: PhotoId): MapState =
        state.copy(sheet = state.sheet?.copy(coverId = id, selected = null))

    fun sheetDismissed(state: MapState): MapState =
        state.copy(sheet = null, query = "", results = emptyList())
}

/** 대표로 지정할 수 있는가 — 사진을 하나 골랐고 그게 이미 대표가 아닐 때만. */
internal fun MapState.canSetCover(): Boolean {
    val sheet = sheet ?: return false
    val picked = sheet.selected ?: return false
    return picked != sheet.coverId
}
