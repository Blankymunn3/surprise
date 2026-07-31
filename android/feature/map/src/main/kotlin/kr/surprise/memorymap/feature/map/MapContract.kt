package kr.surprise.memorymap.feature.map

import kr.surprise.memorymap.core.model.Photo
import kr.surprise.memorymap.core.model.PhotoId
import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.core.model.SpaceId

data class MapState(
    val spaceId: SpaceId,
    val pins: List<RegionPin> = emptyList(),
    val query: String = "",
    val results: List<Region> = emptyList(),
    val sheet: RegionSheetUi? = null,
    val focus: DoubleArray? = null,
    val outline: RegionOutline? = null,
    val fills: List<RegionFill> = emptyList(),
) {
    // focus 가 DoubleArray 라 data class 의 equals 가 참조 비교를 합니다.
    // 화면을 다시 그릴지 판단할 때 틀리지 않게 직접 씁니다.
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is MapState &&
                spaceId == other.spaceId && pins == other.pins && query == other.query &&
                results == other.results && sheet == other.sheet &&
                outline == other.outline && fills == other.fills &&
                (focus?.toList() == other.focus?.toList())
            )

    override fun hashCode(): Int {
        var result = spaceId.hashCode()
        result = 31 * result + pins.hashCode()
        result = 31 * result + query.hashCode()
        result = 31 * result + results.hashCode()
        result = 31 * result + (sheet?.hashCode() ?: 0)
        result = 31 * result + (focus?.toList()?.hashCode() ?: 0)
        result = 31 * result + (outline?.hashCode() ?: 0)
        result = 31 * result + fills.hashCode()
        return result
    }
}

/**
 * 고른 지역의 **경계선**. 지도에 테두리를 그립니다.
 *
 * 같은지 비교할 때 **코드만** 봅니다. 점이 수천 개라 매번 전부 견주면 화면을 다시 그릴
 * 때마다 그 값을 통째로 훑게 되는데, 코드가 같으면 선도 같으므로 볼 필요가 없습니다.
 */
class RegionOutline(val code: String, val polygons: List<List<List<DoubleArray>>>) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is RegionOutline && code == other.code)

    override fun hashCode(): Int = code.hashCode()
}

/**
 * 사진으로 칠할 지역 하나. 지도에 다녀온 곳을 **그 지역의 대표사진으로** 채웁니다.
 *
 * 같은지 비교할 때 **코드와 사진 주소만** 봅니다 — 경계 점이 수천 개라 매번 견주면
 * 화면을 다시 그릴 때마다 그 값을 통째로 훑게 됩니다. 지역이 같고 사진이 같으면
 * 칠할 것도 같습니다.
 */
class RegionFill(
    val code: String,
    val coverUrl: String,
    val polygons: List<List<List<DoubleArray>>>,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is RegionFill && code == other.code && coverUrl == other.coverUrl)

    override fun hashCode(): Int = 31 * code.hashCode() + coverUrl.hashCode()
}

/** 지도에 찍히는 지역 하나. [coverUrl] 이 그 지역을 대표하는 사진입니다. */
data class RegionPin(
    val region: Region,
    val latitude: Double,
    val longitude: Double,
    val coverUrl: String?,
    val photoCount: Int,
)

data class RegionSheetUi(
    val region: Region,
    val photos: List<Photo>,
    val coverId: PhotoId?,
    val selected: PhotoId?,
)

sealed interface MapIntent {
    data class QueryTyped(val value: String) : MapIntent
    data object QueryCleared : MapIntent
    data class RegionChosen(val region: Region) : MapIntent
    data class MapTapped(val latitude: Double, val longitude: Double) : MapIntent
    data class PhotoTapped(val id: PhotoId) : MapIntent
    data object SetCoverTapped : MapIntent
    data object AddPhotoTapped : MapIntent
    data object SheetDismissed : MapIntent
    data object MyLocationTapped : MapIntent
}

sealed interface MapEffect {
    data object OpenUpload : MapEffect
    data class ShowMessage(val text: String) : MapEffect
    data object AskMyLocation : MapEffect
}
