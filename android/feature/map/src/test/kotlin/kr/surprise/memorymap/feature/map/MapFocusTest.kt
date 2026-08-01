package kr.surprise.memorymap.feature.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapFocusTest {

    @Test
    fun `경계를 다 감싼다`() {
        val area = boundsOf(box(west = 126.7, south = 37.4, east = 127.2, north = 37.7))!!

        assertEquals(126.7, area.west, 1e-9)
        assertEquals(127.2, area.east, 1e-9)
        assertEquals(37.4, area.south, 1e-9)
        assertEquals(37.7, area.north, 1e-9)
    }

    /** 나라를 골랐을 때 시군구만 한 배율로 서던 것을 고친 자리입니다. */
    @Test
    fun `나라는 나라만큼 넓게 잡는다`() {
        val japan = boundsOf(box(west = 129.4, south = 31.0, east = 145.5, north = 45.6))!!

        assertEquals(16.1, japan.east - japan.west, 1e-9)
        assertEquals(14.6, japan.north - japan.south, 1e-9)
    }

    /**
     * 날짜변경선을 넘는 나라. 그냥 최솟값·최댓값을 쓰면 -180..180 이 되어 **지구 한 바퀴**가
     * 나옵니다. 피지는 5° 남짓입니다.
     */
    @Test
    fun `날짜변경선을 넘어도 짧은 쪽으로 감싼다`() {
        val fiji = boundsOf(
            listOf(
                ring(177.0, -18.0, 180.0, -17.0),
                ring(-180.0, -18.0, -178.0, -16.0),
            )
        )!!

        assertEquals(177.0, fiji.west, 1e-9)
        // 180 을 넘어간 채로 둡니다. 접으면 다시 지구 한 바퀴가 됩니다.
        assertEquals(182.0, fiji.east, 1e-9)
        assertTrue(fiji.east - fiji.west < 10.0)
    }

    /** 러시아는 선을 넘지만 실제로도 넓습니다 — 넓은 것과 한 바퀴는 다릅니다. */
    @Test
    fun `선을 넘는 넓은 나라도 제 너비만큼만 잡는다`() {
        val russia = boundsOf(
            listOf(
                ring(19.6, 41.2, 180.0, 81.3),
                ring(-180.0, 64.0, -169.0, 71.0),
            )
        )!!

        assertEquals(19.6, russia.west, 1e-9)
        assertEquals(191.0, russia.east, 1e-9)
    }

    @Test
    fun `아메리카처럼 서쪽에만 있어도 그대로 잡는다`() {
        val usa = boundsOf(box(west = -171.8, south = 18.9, east = -67.0, north = 71.4))!!

        assertEquals(-171.8, usa.west, 1e-9)
        assertEquals(-67.0, usa.east, 1e-9)
    }

    @Test
    fun `경계가 없으면 맞출 것도 없다`() {
        assertNull(boundsOf(emptyList()))
        assertNull(boundsOf(listOf(listOf(emptyList()))))
    }

    /** 한 점으로 뭉친 경계는 넓이가 없습니다. 배율을 정해 주는 쪽에 넘깁니다. */
    @Test
    fun `한 점짜리 경계는 넓이로 보지 않는다`() {
        assertNull(boundsOf(listOf(listOf(listOf(doubleArrayOf(127.0, 37.5))))))
    }

    private fun box(west: Double, south: Double, east: Double, north: Double) =
        listOf(ring(west, south, east, north))

    private fun ring(west: Double, south: Double, east: Double, north: Double) =
        listOf(
            listOf(
                doubleArrayOf(west, south),
                doubleArrayOf(east, south),
                doubleArrayOf(east, north),
                doubleArrayOf(west, north),
                doubleArrayOf(west, south),
            )
        )
}
