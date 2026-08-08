package kr.jjaguk.domain.model

import kr.jjaguk.core.model.Cover
import kr.jjaguk.core.model.CoverKey
import kr.jjaguk.core.model.Photo
import kr.jjaguk.core.model.PhotoId
import kr.jjaguk.core.model.RegionCode
import kr.jjaguk.domain.Covers
import java.time.LocalDate

/**
 * 한 공간의 사진 전부를 지도용·달력용으로 한 번에 갈라 둔 것.
 *
 * 탭을 옮길 때 사진을 다시 받지 않기 위해, 지도 Store 와 달력 Store 가
 * **같은 이 객체**를 봅니다. 순수 데이터라 계산 결과를 테스트할 수 있습니다.
 */
data class PhotoBoard(
    val photos: List<Photo>,
    private val chosenCovers: Map<String, PhotoId>,
) {
    val byRegion: Map<RegionCode, List<Photo>> =
        photos.groupBy { it.regionCode }
            .mapValues { (_, list) -> list.sortedByDescending { it.uploadedAtEpochSeconds } }

    val byDay: Map<LocalDate, List<Photo>> =
        photos.groupBy { it.takenOn }
            .mapValues { (_, list) -> list.sortedByDescending { it.uploadedAtEpochSeconds } }

    val regionCount: Int get() = byRegion.size

    fun photosIn(code: RegionCode): List<Photo> = byRegion[code].orEmpty()

    fun photosOn(date: LocalDate): List<Photo> = byDay[date].orEmpty()

    /** 지도에서 그 지역을 칠할 사진 */
    fun regionCover(code: RegionCode): Photo? = cover(CoverKey.ForRegion(code), photosIn(code))

    /** 달력 칸에 놓을 사진 */
    fun dayCover(date: LocalDate): Photo? = cover(CoverKey.ForDay(date), photosOn(date))

    fun byId(id: PhotoId): Photo? = photos.firstOrNull { it.id == id }

    private fun cover(key: CoverKey, candidates: List<Photo>): Photo? {
        val chosen = chosenCovers[key.documentId]
        val id = Covers.resolve(candidates, chosen) ?: return null
        return candidates.firstOrNull { it.id == id }
    }

    companion object {
        fun of(photos: List<Photo>, covers: List<Cover>): PhotoBoard =
            PhotoBoard(photos, covers.associate { it.key.documentId to it.photoId })

        val Empty = PhotoBoard(emptyList(), emptyMap())
    }
}
