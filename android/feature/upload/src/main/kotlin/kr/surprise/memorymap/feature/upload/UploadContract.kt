package kr.surprise.memorymap.feature.upload

import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.core.model.SpaceId
import java.time.LocalDate

data class UploadState(
    val spaceId: SpaceId,
    /**
     * 고른 사진들. **한 장 한 장이 제 지역·날짜를 듭니다.**
     *
     * 예전에는 화면 전체에 지역 하나·날짜 하나였습니다. 그러면 강릉에서 찍은 것과
     * 속초에서 찍은 것을 한 번에 고른 사람은, 많은 쪽으로 뭉뚱그려 올리거나
     * 두 번에 나눠 올려야 했습니다. 사진마다 들고 있으면 그냥 한 번에 올라갑니다.
     */
    val items: List<UploadItem> = emptyList(),
    val step: UploadStep = UploadStep.Editing,
    /** 지역을 고르는 중인 사진. `null` 이면 목록 화면입니다. */
    val editingRegionOf: String? = null,
    val regionQuery: String = "",
    val regionResults: List<Region> = emptyList(),
)

/** 올릴 사진 한 장과 거기 붙은 값들. */
data class UploadItem(
    val uri: String,
    val region: Region?,
    val takenOn: LocalDate,
    /** '자동' 딱지 — 사진에서 읽어 채웠다는 표시. 사용자가 고치면 떨어집니다. */
    val regionAuto: Boolean = false,
    val dateAuto: Boolean = false,
)

/** 고른 사진 한 장. 실제 파일 주소는 문자열로만 들고 다닙니다. */
@JvmInline
value class PickedPhoto(val uri: String)

sealed interface UploadStep {
    data object Editing : UploadStep
    data object Reading : UploadStep
    data object Uploading : UploadStep
    data object Done : UploadStep

    /**
     * 못 올렸다. [savedLocally] 면 사진은 기기에 남아 있습니다 —
     * 화면이 그 사실과 '다시 시도' 를 같이 보여 줍니다.
     */
    data class Failed(val savedLocally: Boolean) : UploadStep
}

sealed interface UploadIntent {
    data class PhotosPicked(val uris: List<PickedPhoto>) : UploadIntent
    /** 그 사진의 '어디' 를 누름 */
    data class RegionFieldTapped(val uri: String) : UploadIntent
    data class RegionQueryTyped(val value: String) : UploadIntent
    data class RegionChosen(val region: Region) : UploadIntent
    /** 그 사진의 '언제' 를 누름 */
    data class DateFieldTapped(val uri: String) : UploadIntent
    data class DateChosen(val uri: String, val date: LocalDate) : UploadIntent
    data object Confirmed : UploadIntent
    data object Dismissed : UploadIntent
    /** 실패 화면의 '다시 시도' */
    data object RetryTapped : UploadIntent
}

sealed interface UploadEffect {
    data object Close : UploadEffect
    data class ShowMessage(val text: String) : UploadEffect
    /** 그 사진의 날짜를 고르는 창 */
    data class OpenDatePicker(val uri: String, val current: LocalDate) : UploadEffect
}
