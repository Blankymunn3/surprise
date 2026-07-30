package kr.surprise.memorymap.domain.usecase

import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.domain.RegionSearch
import kr.surprise.memorymap.domain.repository.RegionCatalog

class SearchRegionsUseCase(private val catalog: RegionCatalog) {
    suspend operator fun invoke(query: String): List<Region> =
        RegionSearch.rank(query, catalog.all())
}

/** 좌표 → 지역. **기기 안에서만** 판정합니다 — 사진 위치를 밖으로 보내지 않으려는 것. */
class ResolveRegionFromLocationUseCase(private val catalog: RegionCatalog) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Region? =
        catalog.regionAt(latitude, longitude)
}

class FindRegionUseCase(private val catalog: RegionCatalog) {
    suspend operator fun invoke(codeValue: String): Region? = catalog.find(codeValue)
}
