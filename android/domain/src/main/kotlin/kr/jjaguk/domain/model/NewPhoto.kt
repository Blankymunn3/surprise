package kr.jjaguk.domain.model

import kr.jjaguk.core.model.RegionCode
import java.time.LocalDate

/** 아직 올리지 않은 사진. [bytes] 는 이미 760px / 품질 72 로 줄인 JPEG 입니다. */
data class NewPhoto(
    val localId: String,
    val bytes: ByteArray,
    val regionCode: RegionCode,
    val takenOn: LocalDate,
) {
    // ByteArray 가 있어 data class 의 equals 가 참조 비교를 합니다. 목록 비교에서 틀리지 않게 직접 씁니다.
    override fun equals(other: Any?): Boolean =
        this === other || (other is NewPhoto && localId == other.localId)

    override fun hashCode(): Int = localId.hashCode()
}

/** 사진 파일에서 읽어낸 힌트. 둘 다 없을 수 있습니다. */
data class ExifHint(
    val takenOn: LocalDate?,
    val regionCode: RegionCode?,
)
