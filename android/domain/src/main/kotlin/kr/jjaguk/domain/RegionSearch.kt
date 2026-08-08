package kr.jjaguk.domain

import kr.jjaguk.core.model.Region
import kr.jjaguk.core.model.RegionKind

/**
 * 지역 검색. 웹과 같은 느낌이어야 해서 순서 규칙을 여기 고정합니다.
 *
 * 1. 이름이 정확히 같은 것
 * 2. 이름이 검색어로 시작하는 것
 * 3. 이름에 검색어가 들어 있는 것
 * 4. 상위 이름(서울, 일본 …)에 들어 있는 것
 *
 * 같은 등급이면 **국내 시군구 → 나라 → 해외 시도 → 장소** 순서입니다.
 * 한국에서 쓰는 앱이라 "중구" 를 치면 서울 중구가 먼저 나와야 합니다.
 */
object RegionSearch {

    fun rank(query: String, regions: List<Region>, limit: Int = 30): List<Region> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        return regions
            .mapNotNull { region -> score(q, region)?.let { region to it } }
            .sortedWith(
                compareBy(
                    { (_, score) -> score },
                    { (region, _) -> kindOrder(region.code.kind) },
                    { (region, _) -> region.name.length },
                    { (region, _) -> region.displayName },
                )
            )
            .take(limit)
            .map { it.first }
    }

    private fun score(query: String, region: Region): Int? {
        // 띄어쓰기는 무시하고 견줍니다 — 표시명은 "성남시 수정구" 인데
        // 붙여 치는 사람이 많고, 그 반대도 있습니다.
        val name = region.name.replace(" ", "")
        val q = query.replace(" ", "")
        return when {
            name.equals(q, ignoreCase = true) -> 0
            name.startsWith(q, ignoreCase = true) -> 1
            name.contains(q, ignoreCase = true) -> 2
            region.parentName?.replace(" ", "")
                ?.contains(q, ignoreCase = true) == true -> 3
            else -> null
        }
    }

    private fun kindOrder(kind: RegionKind): Int = when (kind) {
        RegionKind.KoreanDistrict -> 0
        RegionKind.Country -> 1
        RegionKind.Subdivision -> 2
        RegionKind.Place -> 3
    }
}
