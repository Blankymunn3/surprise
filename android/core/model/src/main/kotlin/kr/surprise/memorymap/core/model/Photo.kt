package kr.surprise.memorymap.core.model

import java.time.LocalDate

/**
 * 사진 한 장.
 *
 * [takenOn] 은 **날짜만** 입니다. 시각까지 저장하면 시간대 때문에 밤 11시에 찍은 사진이
 * 다음 날 칸으로 밀립니다. EXIF 의 촬영 시각은 *찍은 곳의 벽시계* 시각이므로
 * 그 날짜를 그대로 씁니다. (`docs/app/SCREENS.md`)
 */
data class Photo(
    val id: PhotoId,
    val regionCode: RegionCode,
    val takenOn: LocalDate,
    val storagePath: String,
    val downloadUrl: String,
    val uploadedBy: String,
    val uploadedAtEpochSeconds: Long,
)

/** 지도에 칠할 사진 / 달력 칸에 놓을 사진. 사진 문서의 깃발이 아니라 따로 둡니다. */
data class Cover(
    val key: CoverKey,
    val photoId: PhotoId,
)

sealed interface CoverKey {
    data class ForRegion(val code: RegionCode) : CoverKey
    data class ForDay(val date: LocalDate) : CoverKey

    /** Firestore 문서 ID. `region_11140`, `day_2026-03-05` */
    val documentId: String
        get() = when (this) {
            is ForRegion -> "region_${code.value}"
            is ForDay -> "day_$date"
        }

    companion object {
        /**
         * [documentId] 의 반대. 규칙에 안 맞으면 `null` — 손으로 넣은 문서가 섞여도
         * 앱이 죽지 않아야 합니다.
         *
         * 짝이 되는 두 함수라 **한 파일에 둡니다.** 떨어뜨려 놓으면 한쪽만 고치게 됩니다.
         */
        fun of(documentId: String): CoverKey? = when {
            documentId.startsWith("region_") ->
                ForRegion(RegionCode(documentId.removePrefix("region_")))
            documentId.startsWith("day_") -> try {
                ForDay(LocalDate.parse(documentId.removePrefix("day_")))
            } catch (e: Exception) {
                null
            }
            else -> null
        }
    }
}
