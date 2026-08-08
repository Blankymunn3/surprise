package kr.jjaguk.domain

import kr.jjaguk.core.model.RegionCode
import kr.jjaguk.domain.model.ExifHint
import java.time.LocalDate

/**
 * 여러 장을 한 번에 올릴 때 지역·날짜 기본값을 정합니다.
 *
 * 한 번에 여러 장 올리는 건 보통 **같은 곳에서 같은 날** 찍은 사진이라,
 * 고른 사진 전부에 같은 값을 붙입니다. 사진마다 값이 다르면 많은 쪽을 기본으로
 * 두고 몇 장이 다른지 알려 줍니다 — 나눠서 올리면 되니까요.
 */
object UploadPlan {

    data class Defaults(
        val regionCode: RegionCode?,
        val takenOn: LocalDate,
        /** 기본값과 지역이 다른 사진 수. 0 이면 알릴 것이 없습니다. */
        val regionMismatch: Int,
        /** 기본값과 날짜가 다른 사진 수 */
        val dateMismatch: Int,
        /** 지역을 사진에서 찾아 채웠는가 (화면의 '자동' 칩) */
        val regionFromExif: Boolean,
        /** 날짜를 사진에서 찾아 채웠는가 */
        val dateFromExif: Boolean,
    )

    fun defaults(hints: List<ExifHint>, today: LocalDate): Defaults {
        val regions = hints.mapNotNull { it.regionCode }
        val dates = hints.mapNotNull { it.takenOn }

        val region = regions.majorityOrNull()
        val date = dates.majorityOrNull()

        return Defaults(
            regionCode = region,
            // 날짜가 하나도 없으면 오늘. 오늘 찍은 사진을 오늘 올리는 게 가장 흔합니다.
            takenOn = date ?: today,
            regionMismatch = if (region == null) 0 else regions.count { it != region },
            dateMismatch = if (date == null) 0 else dates.count { it != date },
            regionFromExif = region != null,
            dateFromExif = date != null,
        )
    }

    /**
     * 가장 많이 나온 값. 같은 수면 **먼저 나온 쪽** — 사용자가 고른 순서가
     * 보통 사진 순서라, 첫 사진 기준이 덜 놀랍습니다.
     */
    private fun <T> List<T>.majorityOrNull(): T? {
        if (isEmpty()) return null
        val counts = LinkedHashMap<T, Int>()
        for (v in this) counts[v] = (counts[v] ?: 0) + 1
        return counts.maxByOrNull { it.value }?.key
    }
}
