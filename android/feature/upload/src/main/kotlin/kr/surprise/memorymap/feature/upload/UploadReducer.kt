package kr.surprise.memorymap.feature.upload

import kr.surprise.memorymap.core.model.Region
import java.time.LocalDate

internal object UploadReducer {

    fun picked(state: UploadState, uris: List<PickedPhoto>): UploadState =
        state.copy(
            items = uris.map { UploadItem(uri = it.uri, region = null, takenOn = LocalDate.MIN) },
            step = if (uris.isEmpty()) UploadStep.Editing else UploadStep.Reading,
        )

    /** 사진에서 읽은 값을 **한 장씩** 채웁니다. */
    fun hintsRead(state: UploadState, items: List<UploadItem>): UploadState =
        state.copy(items = items, step = UploadStep.Editing)

    fun regionPickerOpened(state: UploadState, uri: String): UploadState =
        state.copy(editingRegionOf = uri, regionQuery = "", regionResults = emptyList())

    fun regionPickerDismissed(state: UploadState): UploadState =
        state.copy(editingRegionOf = null, regionQuery = "", regionResults = emptyList())

    fun regionQueryTyped(state: UploadState, value: String, results: List<Region>): UploadState =
        state.copy(regionQuery = value, regionResults = results)

    fun regionChosen(state: UploadState, region: Region): UploadState {
        val uri = state.editingRegionOf ?: return state
        return state
            .copy(items = state.items.map {
                // 사용자가 직접 고른 값이므로 '자동' 딱지를 뗍니다
                if (it.uri == uri) it.copy(region = region, regionAuto = false) else it
            })
            .let(::regionPickerDismissed)
    }

    fun dateChosen(state: UploadState, uri: String, date: LocalDate): UploadState =
        state.copy(items = state.items.map {
            if (it.uri == uri) it.copy(takenOn = date, dateAuto = false) else it
        })

    fun uploading(state: UploadState): UploadState = state.copy(step = UploadStep.Uploading)

    fun uploaded(state: UploadState): UploadState = state.copy(step = UploadStep.Done)

    /**
     * 올리기가 실패하면 **기기에 저장하고 그 사실을 알립니다.** 사진을 잃지 않습니다.
     * 웹에서 사용자와 한 약속이라 바꾸지 않습니다 (`docs/app/ARCHITECTURE.md`).
     */
    fun failed(state: UploadState, savedLocally: Boolean): UploadState =
        state.copy(step = UploadStep.Failed(savedLocally))

    /** '다시 시도' — 고친 값은 그대로 두고 올릴 수 있는 상태로만 되돌립니다. */
    fun retry(state: UploadState): UploadState = state.copy(step = UploadStep.Editing)
}

/**
 * 올릴 수 있는가. **지역이 빈 사진이 하나라도 있으면 안 됩니다** —
 * 지도에 올라갈 자리가 없어집니다. 화면의 버튼이 꺼지는 조건과 같아야 합니다.
 */
internal fun UploadState.canUpload(): Boolean =
    items.isNotEmpty() &&
        items.all { it.region != null } &&
        (step == UploadStep.Editing || step is UploadStep.Failed)

/**
 * "지역 2곳 · 날짜 2일로 나눠 올라가요" — 한 번에 고른 사진이 여러 곳·여러 날에
 * 걸쳐 있을 때만 알립니다. 한 곳 한 날이면 알릴 것이 없습니다.
 */
internal fun UploadState.splitNotice(): String? {
    val places = items.mapNotNull { it.region?.code }.distinct().size
    val days = items.map { it.takenOn }.distinct().size
    if (places <= 1 && days <= 1) return null
    return "지역 ${places}곳 · 날짜 ${days}일로 나눠 올라가요"
}
