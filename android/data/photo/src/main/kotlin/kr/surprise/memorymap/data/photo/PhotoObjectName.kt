package kr.surprise.memorymap.data.photo

import kr.surprise.memorymap.core.model.PathSafe
import kr.surprise.memorymap.core.model.PhotoId
import kr.surprise.memorymap.core.model.RegionCode
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 사진 파일 이름에 지역과 날짜를 함께 적습니다.
 *
 * `spaces/<공간ID>/photos/2026-03-05_11140_a1b2c3.jpg`
 *
 * **왜 파일 이름에 넣나**: 지금은 로그인과 Firestore 가 없습니다. 그래서 사진 문서를
 * 따로 둘 곳이 없어, 목록 조회 한 번으로 지역·날짜까지 알 수 있게 이름에 적었습니다.
 * 로그인이 붙으면 문서(`spaces/{id}/photos/{photoId}`)로 옮기고 이 파서는 지웁니다.
 * 목표 구조는 `docs/app/SCREENS.md` 에 그대로 남아 있습니다.
 *
 * 파싱은 **자리로** 합니다 (앞 10글자가 날짜, 마지막 `_` 뒤가 사진 ID).
 * 지역 코드에 `_` 가 들어 있어도 깨지지 않아야 하기 때문입니다.
 */
internal object PhotoObjectName {

    private const val EXT = ".jpg"

    data class Parsed(val id: PhotoId, val regionCode: RegionCode, val takenOn: LocalDate)

    fun build(id: PhotoId, regionCode: RegionCode, takenOn: LocalDate): String {
        PathSafe.require(id.value, "사진 ID")
        require(!regionCode.value.contains('/')) { "지역 코드에 / 를 넣을 수 없습니다" }
        return "$takenOn" + "_" + regionCode.value + "_" + id.value + EXT
    }

    /** 이름이 규칙에 안 맞으면 null. 사람이 손으로 올린 파일이 섞여도 앱이 죽지 않게. */
    fun parse(fileName: String): Parsed? {
        if (!fileName.endsWith(EXT)) return null
        val body = fileName.removeSuffix(EXT)
        if (body.length < 13) return null

        val date = try {
            LocalDate.parse(body.substring(0, 10))
        } catch (e: DateTimeParseException) {
            return null
        }
        if (body[10] != '_') return null

        val rest = body.substring(11)
        val cut = rest.lastIndexOf('_')
        if (cut <= 0 || cut == rest.length - 1) return null

        val region = rest.substring(0, cut)
        val id = rest.substring(cut + 1)
        if (!PathSafe.isSafe(id)) return null

        return Parsed(PhotoId(id), RegionCode(region), date)
    }
}
