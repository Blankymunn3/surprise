package kr.jjaguk.domain

import kr.jjaguk.core.model.Cover
import kr.jjaguk.core.model.CoverKey
import kr.jjaguk.core.model.PhotoId
import kr.jjaguk.core.model.RegionCode
import kr.jjaguk.domain.model.PhotoBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PhotoBoardTest {

    private val mar5 = LocalDate.of(2026, 3, 5)
    private val mar21 = LocalDate.of(2026, 3, 21)
    private val seoul = RegionCode("11140")
    private val osaka = RegionCode("P-JPN-27")

    private val photos = listOf(
        photo("a", region = seoul.value, takenOn = mar5, uploadedAt = 10),
        photo("b", region = seoul.value, takenOn = mar5, uploadedAt = 30),
        photo("c", region = osaka.value, takenOn = mar21, uploadedAt = 20),
    )

    @Test
    fun `같은 사진을 지역별과 날짜별로 함께 본다`() {
        val board = PhotoBoard.of(photos, emptyList())

        assertEquals(2, board.photosIn(seoul).size)
        assertEquals(1, board.photosOn(mar21).size)
        assertEquals(2, board.regionCount)
    }

    @Test
    fun `대표를 안 정하면 가장 최근 사진이 지역을 칠한다`() {
        val board = PhotoBoard.of(photos, emptyList())

        assertEquals(PhotoId("b"), board.regionCover(seoul)?.id)
    }

    @Test
    fun `정해둔 대표가 지도와 달력에 각각 따로 적용된다`() {
        val board = PhotoBoard.of(
            photos,
            listOf(
                Cover(CoverKey.ForRegion(seoul), PhotoId("a")),
                Cover(CoverKey.ForDay(mar5), PhotoId("b")),
            ),
        )

        assertEquals(PhotoId("a"), board.regionCover(seoul)?.id)
        assertEquals(PhotoId("b"), board.dayCover(mar5)?.id)
    }

    @Test
    fun `사진이 없는 지역과 날짜는 대표도 없다`() {
        val board = PhotoBoard.of(photos, emptyList())

        assertNull(board.regionCover(RegionCode("99999")))
        assertNull(board.dayCover(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `목록은 최근에 올린 것부터 나온다`() {
        val board = PhotoBoard.of(photos, emptyList())

        assertEquals(listOf(PhotoId("b"), PhotoId("a")), board.photosIn(seoul).map { it.id })
    }

    @Test
    fun `빈 판은 아무것도 없다`() {
        assertTrue(PhotoBoard.Empty.photos.isEmpty())
        assertEquals(0, PhotoBoard.Empty.regionCount)
    }
}
