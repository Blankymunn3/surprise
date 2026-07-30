package kr.surprise.memorymap.data.region

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointInPolygonTest {

    // 경도/위도 순서 (GeoJSON)
    private val square = listOf(
        doubleArrayOf(0.0, 0.0), doubleArrayOf(10.0, 0.0),
        doubleArrayOf(10.0, 10.0), doubleArrayOf(0.0, 10.0), doubleArrayOf(0.0, 0.0),
    )

    @Test
    fun `안에 있으면 안이다`() {
        assertTrue(PointInPolygon.inRing(5.0, 5.0, square))
    }

    @Test
    fun `밖에 있으면 밖이다`() {
        assertFalse(PointInPolygon.inRing(15.0, 5.0, square))
        assertFalse(PointInPolygon.inRing(5.0, -1.0, square))
    }

    @Test
    fun `구멍 안은 밖이다`() {
        val hole = listOf(
            doubleArrayOf(4.0, 4.0), doubleArrayOf(6.0, 4.0),
            doubleArrayOf(6.0, 6.0), doubleArrayOf(4.0, 6.0), doubleArrayOf(4.0, 4.0),
        )

        assertFalse(PointInPolygon.inPolygon(5.0, 5.0, listOf(square, hole)))
        assertTrue(PointInPolygon.inPolygon(2.0, 2.0, listOf(square, hole)))
    }

    @Test
    fun `오목한 모양도 맞게 가른다`() {
        // ㄷ 자 모양
        val c = listOf(
            doubleArrayOf(0.0, 0.0), doubleArrayOf(10.0, 0.0), doubleArrayOf(10.0, 3.0),
            doubleArrayOf(3.0, 3.0), doubleArrayOf(3.0, 7.0), doubleArrayOf(10.0, 7.0),
            doubleArrayOf(10.0, 10.0), doubleArrayOf(0.0, 10.0), doubleArrayOf(0.0, 0.0),
        )

        assertTrue(PointInPolygon.inRing(1.0, 5.0, c))    // 왼쪽 기둥 안
        assertFalse(PointInPolygon.inRing(7.0, 5.0, c))   // 파인 곳
    }
}
