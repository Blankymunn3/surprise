package kr.surprise.memorymap.core.model

/**
 * 지역 코드. **웹과 반드시 같아야 합니다** — 웹에서 넣은 사진이 앱에서도 보여야 하니까요.
 * 규칙은 `docs/app/CONVENTIONS.md` 와 웹의 `map/index.html` 에 있습니다.
 *
 * | 코드        | 뜻                          |
 * |-------------|-----------------------------|
 * | `11140`     | 국내 시군구 (행정구역 코드)  |
 * | `C-JPN`     | 나라 (ISO alpha-3)          |
 * | `P-JPN-12`  | 해외 시도 (나라 + 원본 순번) |
 * | `bali`      | 경계 없이 좌표만 있는 장소   |
 */
@JvmInline
value class RegionCode(val value: String) {

    val kind: RegionKind
        get() = when {
            value.startsWith("C-") -> RegionKind.Country
            value.startsWith("P-") -> RegionKind.Subdivision
            value.isNotEmpty() && value.all { it.isDigit() } -> RegionKind.KoreanDistrict
            else -> RegionKind.Place
        }

    /** `P-JPN-12` → `JPN`. 해외 시도가 어느 나라 것인지 검색 결과에 나라 이름을 붙이려고 씁니다. */
    val countryIso3: String?
        get() = when (kind) {
            RegionKind.Country -> value.removePrefix("C-")
            RegionKind.Subdivision -> value.removePrefix("P-").substringBefore('-')
            else -> null
        }

    override fun toString(): String = value
}

enum class RegionKind { KoreanDistrict, Country, Subdivision, Place }

/**
 * 검색과 표시에 쓰는 지역 한 줄.
 * [parentName] 은 "중구" 만으로는 어디인지 모르기 때문에 붙이는 상위 이름입니다 (서울, 일본 …).
 */
data class Region(
    val code: RegionCode,
    val name: String,
    val parentName: String?,
) {
    val displayName: String get() = if (parentName.isNullOrBlank()) name else "$parentName $name"
}
