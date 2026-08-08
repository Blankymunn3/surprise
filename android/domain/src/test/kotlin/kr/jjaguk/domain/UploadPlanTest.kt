package kr.jjaguk.domain

import kr.jjaguk.core.model.RegionCode
import kr.jjaguk.domain.model.ExifHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UploadPlanTest {

    private val today = LocalDate.of(2026, 7, 30)
    private val seoul = RegionCode("11140")
    private val osaka = RegionCode("P-JPN-27")

    @Test
    fun `사진에서 읽은 지역과 날짜를 기본값으로 채운다`() {
        val hints = listOf(
            ExifHint(LocalDate.of(2026, 3, 5), seoul),
            ExifHint(LocalDate.of(2026, 3, 5), seoul),
        )

        val d = UploadPlan.defaults(hints, today)

        assertEquals(seoul, d.regionCode)
        assertEquals(LocalDate.of(2026, 3, 5), d.takenOn)
        assertTrue(d.regionFromExif)
        assertTrue(d.dateFromExif)
        assertEquals(0, d.regionMismatch)
    }

    @Test
    fun `사진마다 지역이 다르면 많은 쪽을 기본으로 두고 몇 장이 다른지 알린다`() {
        val hints = listOf(
            ExifHint(null, seoul),
            ExifHint(null, seoul),
            ExifHint(null, osaka),
        )

        val d = UploadPlan.defaults(hints, today)

        assertEquals(seoul, d.regionCode)
        assertEquals(1, d.regionMismatch)   // "3장 중 1장은 다른 곳이에요"
    }

    @Test
    fun `날짜를 하나도 못 읽으면 오늘로 둔다`() {
        val d = UploadPlan.defaults(listOf(ExifHint(null, null), ExifHint(null, null)), today)

        assertEquals(today, d.takenOn)
        assertFalse(d.dateFromExif)
    }

    @Test
    fun `지역을 못 읽으면 비워 둔다 - 사용자가 반드시 골라야 한다`() {
        val d = UploadPlan.defaults(listOf(ExifHint(LocalDate.of(2026, 3, 5), null)), today)

        assertNull(d.regionCode)
        assertFalse(d.regionFromExif)
    }

    @Test
    fun `같은 수로 갈리면 먼저 나온 쪽을 쓴다`() {
        val hints = listOf(ExifHint(null, osaka), ExifHint(null, seoul))

        assertEquals(osaka, UploadPlan.defaults(hints, today).regionCode)
    }

    @Test
    fun `사진이 하나도 없으면 오늘 날짜에 지역은 비어 있다`() {
        val d = UploadPlan.defaults(emptyList(), today)

        assertNull(d.regionCode)
        assertEquals(today, d.takenOn)
    }
}
