package kr.surprise.memorymap.feature.upload

import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.core.model.SpaceId
import java.time.LocalDate

data class UploadState(
    val spaceId: SpaceId,
    val picked: List<PickedPhoto> = emptyList(),
    val region: Region? = null,
    val takenOn: LocalDate? = null,
    /** '자동' 칩 — 그 칸을 사진에서 읽어 채웠다는 표시 */
    val regionFromExif: Boolean = false,
    val dateFromExif: Boolean = false,
    val regionMismatch: Int = 0,
    val dateMismatch: Int = 0,
    val step: UploadStep = UploadStep.Editing,
    val regionQuery: String = "",
    val regionResults: List<Region> = emptyList(),
    val pickingRegion: Boolean = false,
)

/** 고른 사진 한 장. 실제 파일 주소는 문자열로만 들고 다닙니다. */
@JvmInline
value class PickedPhoto(val uri: String)

sealed interface UploadStep {
    data object Editing : UploadStep
    data object Reading : UploadStep
    data object Uploading : UploadStep
    data object Done : UploadStep
    data class Failed(val savedLocally: Boolean) : UploadStep
}

sealed interface UploadIntent {
    data class PhotosPicked(val uris: List<PickedPhoto>) : UploadIntent
    data object RegionFieldTapped : UploadIntent
    data class RegionQueryTyped(val value: String) : UploadIntent
    data class RegionChosen(val region: Region) : UploadIntent
    data class DateChosen(val date: LocalDate) : UploadIntent
    data object Confirmed : UploadIntent
    data object Dismissed : UploadIntent
}

sealed interface UploadEffect {
    data object Close : UploadEffect
    data class ShowMessage(val text: String) : UploadEffect
    data object OpenDatePicker : UploadEffect
}
