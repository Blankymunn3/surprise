package kr.surprise.memorymap.domain

import kr.surprise.memorymap.core.model.PathSafe
import kr.surprise.memorymap.core.model.RegionCode
import kr.surprise.memorymap.core.model.RegionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 지역 코드가 바뀌면 **이미 올린 사진이 지도에서 사라집니다.**
 * 웹과 같은 규칙인지 여기서 붙잡습니다.
 */
class RegionCodeTest {

    @Test
    fun `코드 모양으로 종류를 가른다`() {
        assertEquals(RegionKind.KoreanDistrict, RegionCode("11140").kind)
        assertEquals(RegionKind.Country, RegionCode("C-JPN").kind)
        assertEquals(RegionKind.Subdivision, RegionCode("P-JPN-27").kind)
        assertEquals(RegionKind.Place, RegionCode("bali").kind)
    }

    @Test
    fun `해외 시도에서 나라를 뽑아낸다`() {
        assertEquals("JPN", RegionCode("P-JPN-27").countryIso3)
        assertEquals("JPN", RegionCode("C-JPN").countryIso3)
        assertNull(RegionCode("11140").countryIso3)
        assertNull(RegionCode("bali").countryIso3)
    }

    @Test
    fun `경로를 벗어나는 값은 막는다`() {
        assertTrue(PathSafe.isSafe("11140"))
        assertTrue(PathSafe.isSafe("P-JPN-27"))
        assertFalse(PathSafe.isSafe("../secret"))
        assertFalse(PathSafe.isSafe("a/b"))
        assertFalse(PathSafe.isSafe(""))
    }
}
