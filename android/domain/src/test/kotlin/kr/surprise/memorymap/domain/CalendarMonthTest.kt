package kr.surprise.memorymap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarMonthTest {

    @Test
    fun `1일이 일요일이면 앞 빈 칸이 없다`() {
        // 2026년 3월 1일은 일요일입니다. 디자인 시안이 이 달을 씁니다.
        val grid = CalendarMonth.grid(YearMonth.of(2026, 3))

        assertEquals(LocalDate.of(2026, 3, 1), grid.first())
    }

    @Test
    fun `1일이 목요일이면 앞 빈 칸이 넷이다`() {
        // 2026년 1월 1일은 목요일 (일 월 화 수 다음)
        val grid = CalendarMonth.grid(YearMonth.of(2026, 1))

        assertEquals(listOf(null, null, null, null), grid.take(4))
        assertEquals(LocalDate.of(2026, 1, 1), grid[4])
    }

    @Test
    fun `칸 수는 항상 7의 배수다`() {
        for (month in 1..12) {
            val grid = CalendarMonth.grid(YearMonth.of(2026, month))
            assertEquals("2026-$month", 0, grid.size % 7)
        }
    }

    @Test
    fun `그달의 모든 날이 빠짐없이 한 번씩 들어간다`() {
        val month = YearMonth.of(2026, 3)
        val days = CalendarMonth.grid(month).filterNotNull()

        assertEquals(31, days.size)
        assertEquals(days.distinct(), days)
        assertTrue(days.all { it.month == month.month })
    }

    @Test
    fun `윤년 2월은 29일까지 나온다`() {
        val days = CalendarMonth.grid(YearMonth.of(2028, 2)).filterNotNull()

        assertEquals(29, days.size)
    }

    @Test
    fun `마지막 칸이 그달을 넘어가면 빈 칸이다`() {
        val grid = CalendarMonth.grid(YearMonth.of(2026, 3))

        // 3월은 31일 + 앞 빈 칸 0 = 31칸 → 35칸으로 채워지고 뒤 4칸이 빔
        assertEquals(35, grid.size)
        assertNull(grid.last())
    }

    @Test
    fun `일요일 판정`() {
        assertTrue(CalendarMonth.isSunday(LocalDate.of(2026, 3, 1)))
        assertTrue(CalendarMonth.isSunday(LocalDate.of(2026, 3, 8)))
        assertTrue(!CalendarMonth.isSunday(LocalDate.of(2026, 3, 2)))
    }
}
