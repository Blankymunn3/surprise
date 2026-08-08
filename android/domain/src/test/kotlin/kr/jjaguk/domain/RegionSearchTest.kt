package kr.jjaguk.domain

import kr.jjaguk.core.model.Region
import kr.jjaguk.core.model.RegionCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionSearchTest {

    private val jungguSeoul = Region(RegionCode("11140"), "중구", "서울")
    private val jungguBusan = Region(RegionCode("26110"), "중구", "부산")
    private val japan = Region(RegionCode("C-JPN"), "일본", null)
    private val osaka = Region(RegionCode("P-JPN-27"), "오사카부", "일본")
    private val bali = Region(RegionCode("bali"), "발리", "인도네시아")

    private val all = listOf(osaka, japan, jungguBusan, jungguSeoul, bali)

    @Test
    fun `이름이 정확히 같은 것이 먼저 나온다`() {
        val hits = RegionSearch.rank("일본", all)

        assertEquals(japan, hits.first())
    }

    @Test
    fun `국내 시군구가 해외보다 먼저 나온다`() {
        val hits = RegionSearch.rank("중구", all)

        assertTrue(hits.take(2).all { it.name == "중구" })
    }

    @Test
    fun `상위 이름으로도 찾힌다`() {
        val hits = RegionSearch.rank("서울", all)

        assertEquals(listOf(jungguSeoul), hits)
    }

    @Test
    fun `앞부분이 맞는 것이 가운데 맞는 것보다 먼저다`() {
        val regions = listOf(
            Region(RegionCode("C-USA"), "미국", null),
            Region(RegionCode("C-FSM"), "미크로네시아", null),
        )

        val hits = RegionSearch.rank("미크", regions)

        assertEquals("미크로네시아", hits.first().name)
    }

    @Test
    fun `빈 검색어는 아무것도 내지 않는다`() {
        assertTrue(RegionSearch.rank("   ", all).isEmpty())
    }

    @Test
    fun `없는 이름은 빈 결과다`() {
        assertTrue(RegionSearch.rank("없는지역", all).isEmpty())
    }
}
