package kr.surprise.memorymap.feature.upload

import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.domain.UploadPlan
import java.time.LocalDate

internal object UploadReducer {

    fun picked(state: UploadState, uris: List<PickedPhoto>): UploadState =
        state.copy(picked = uris, step = if (uris.isEmpty()) UploadStep.Editing else UploadStep.Reading)

    /** 사진에서 읽은 힌트를 기본값으로 채웁니다. */
    fun hintsRead(state: UploadState, defaults: UploadPlan.Defaults, region: Region?): UploadState =
        state.copy(
            region = region ?: state.region,
            takenOn = defaults.takenOn,
            regionFromExif = defaults.regionFromExif && region != null,
            dateFromExif = defaults.dateFromExif,
            regionMismatch = defaults.regionMismatch,
            dateMismatch = defaults.dateMismatch,
            step = UploadStep.Editing,
        )

    fun regionPickerOpened(state: UploadState): UploadState =
        state.copy(pickingRegion = true, regionQuery = "", regionResults = emptyList())

    fun regionQueryTyped(state: UploadState, value: String, results: List<Region>): UploadState =
        state.copy(regionQuery = value, regionResults = results)

    fun regionChosen(state: UploadState, region: Region): UploadState =
        state.copy(
            region = region,
            // 사용자가 직접 고른 값이므로 '자동' 딱지를 뗍니다
            regionFromExif = false,
            regionMismatch = 0,
            pickingRegion = false,
            regionQuery = "",
            regionResults = emptyList(),
        )

    fun dateChosen(state: UploadState, date: LocalDate): UploadState =
        state.copy(takenOn = date, dateFromExif = false, dateMismatch = 0)

    fun uploading(state: UploadState): UploadState = state.copy(step = UploadStep.Uploading)

    fun uploaded(state: UploadState): UploadState = state.copy(step = UploadStep.Done)

    /**
     * 올리기가 실패하면 **기기에 저장하고 그 사실을 알립니다.** 사진을 잃지 않습니다.
     * 웹에서 사용자와 한 약속이라 바꾸지 않습니다 (`docs/app/ARCHITECTURE.md`).
     */
    fun failed(state: UploadState, savedLocally: Boolean): UploadState =
        state.copy(step = UploadStep.Failed(savedLocally))
}

/**
 * 올릴 수 있는가. **지역은 비울 수 없습니다** — 지도에 올라갈 자리가 없어집니다.
 * 화면의 버튼이 꺼지는 조건과 이 함수가 같아야 합니다.
 */
internal fun UploadState.canUpload(): Boolean =
    picked.isNotEmpty() && region != null && takenOn != null && step == UploadStep.Editing

/** "3장 중 2장은 다른 곳이에요" — 나눠 올리라고 알려 주는 문구. 알릴 것이 없으면 null. */
internal fun UploadState.mismatchNotice(): String? = when {
    regionMismatch > 0 -> "${picked.size}장 중 ${regionMismatch}장은 다른 곳이에요"
    dateMismatch > 0 -> "${picked.size}장 중 ${dateMismatch}장은 다른 날이에요"
    else -> null
}
