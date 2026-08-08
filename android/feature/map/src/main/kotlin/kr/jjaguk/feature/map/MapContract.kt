package kr.jjaguk.feature.map

import android.content.Context

import kr.jjaguk.core.model.Photo
import kr.jjaguk.core.model.PhotoId
import kr.jjaguk.core.model.Region
import kr.jjaguk.core.model.SpaceId

data class MapState(
    val spaceId: SpaceId,
    val pins: List<RegionPin> = emptyList(),
    val query: String = "",
    val results: List<Region> = emptyList(),
    val sheet: RegionSheetUi? = null,
    val focus: MapFocus? = null,
    /** 몇 번째 맞춤인지. 같은 지역을 다시 골라도 화면을 다시 맞추려고 셉니다 —
     *  맞출 곳만 보면 값이 그대로라 지도가 꿈쩍도 안 합니다. */
    val focusCount: Int = 0,
    val outline: RegionOutline? = null,
    val fills: List<RegionFill> = emptyList(),
    /**
     * 지금 내가 있는 자리. **찾았을 때만** 채워집니다.
     *
     * 화면을 옮기는 것만으로는 부족해서 둡니다 — 옮겨 준 뒤 조금만 손으로 밀면
     * 어디가 그 자리였는지 잃습니다. 표시가 남아 있으면 다시 찾아갈 수 있습니다.
     */
    val myLocation: MyPin? = null,
)

/** 지도에 찍는 내 자리. */
data class MyPin(val latitude: Double, val longitude: Double)

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
)

sealed interface MapIntent {
    data class QueryTyped(val value: String) : MapIntent
    data object QueryCleared : MapIntent
    data class RegionChosen(val region: Region) : MapIntent
    data class MapTapped(val latitude: Double, val longitude: Double) : MapIntent
    /**
     * 시트에서 사진을 누름 = **그 사진을 대표로 지정.**
     *
     * 고르고 나서 '대표로 지정' 을 또 누르는 두 단계였는데, 시트에서 사진을 누르는
     * 일이 그것 말고는 없어서 한 단계로 합쳤습니다.
     */
    data class PhotoTapped(val id: PhotoId) : MapIntent
    data object AddPhotoTapped : MapIntent
    data object SheetDismissed : MapIntent
    data object MyLocationTapped : MapIntent

    /**
     * 기기가 알려 준 지금 자리. 위치를 **찾는 일은 앱 껍데기**가 합니다 —
     * 권한을 묻고 안드로이드 위치 서비스를 부르는 일이라 도메인이 알 필요가 없습니다.
     * 여기서는 좌표만 받아 지도를 옮깁니다.
     */
    data class MyLocationFound(val latitude: Double, val longitude: Double) : MapIntent
}

/**
 * 화면에 알릴 일. **문구가 아니라 "무슨 일이 있었는지"** 를 나릅니다 —
 * 말은 화면이 고릅니다.
 *
 * 뷰모델이 문장을 만들면 그 문장이 리소스로 못 가고 코드에 박힙니다.
 * 뷰모델은 Composable 이 아니라 `stringResource` 를 쓸 수 없기 때문입니다.
 */
enum class MapMessage {
    /** 누른 자리가 어느 지역인지 판정하지 못함 */
    RegionUnknown,
    /** 대표사진을 바꾸지 못함 */
    CoverFailed,
}

sealed interface MapEffect {
    /**
     * 사진 올리기를 엽니다.
     *
     * [region] 은 **지역 시트에서 눌렀을 때** 그 지역입니다. 이미 고른 곳을 알고 있는데
     * 올리기 화면에서 다시 고르게 하면 안 됩니다. 아래쪽 ＋ 로 열었으면 `null` 이고,
     * 그때는 사진의 EXIF 가 지역을 정합니다.
     */
    data class OpenUpload(val region: Region?) : MapEffect
    data class ShowMessage(val message: MapMessage) : MapEffect
    data object AskMyLocation : MapEffect
}

/**
 * [MapMessage] 를 사람이 읽을 말로. **화면 쪽에서 고릅니다.**
 *
 * `Context` 를 받는 이유: 스낵바는 `LaunchedEffect` 안에서 띄우는데 그 안은
 * Composable 이 아니라 `stringResource` 를 못 씁니다.
 */
fun MapMessage.say(context: Context): String = context.getString(
    when (this) {
        MapMessage.RegionUnknown -> R.string.map_msg_region_unknown
        MapMessage.CoverFailed -> R.string.map_msg_cover_failed
    }
)
