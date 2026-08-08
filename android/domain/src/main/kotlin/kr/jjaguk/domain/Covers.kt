package kr.jjaguk.domain

import kr.jjaguk.core.model.Photo
import kr.jjaguk.core.model.PhotoId

/**
 * 대표사진을 고르는 규칙.
 *
 * 대표사진은 사진 문서의 깃발이 아니라 따로 둡니다. 깃발이면 대표를 바꿀 때
 * 문서 둘을 고쳐야 하고, 중간에 실패하면 대표가 둘이거나 없어집니다.
 * 그래서 "대표가 비었을 때 무엇을 보여줄지" 를 여기서 정합니다.
 */
object Covers {

    /** 정해진 대표가 없으면 **가장 최근에 올린 사진**. */
    fun fallback(photos: List<Photo>): PhotoId? =
        photos.maxByOrNull { it.uploadedAtEpochSeconds }?.id

    /**
     * 지금 보여줄 대표사진.
     * 정해 둔 [chosen] 이 이미 지워졌으면 없는 셈 치고 [fallback] 으로 내려갑니다.
     */
    fun resolve(photos: List<Photo>, chosen: PhotoId?): PhotoId? {
        if (photos.isEmpty()) return null
        val stillThere = chosen != null && photos.any { it.id == chosen }
        return if (stillThere) chosen else fallback(photos)
    }
}
