package kr.jjaguk.data.photo

import kr.jjaguk.core.model.PhotoId
import kr.jjaguk.core.model.RegionCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PhotoObjectNameTest {

    @Test
    fun `이름을 만들고 다시 읽으면 그대로다`() {
        val name = PhotoObjectName.build(PhotoId("abc123"), RegionCode("11140"), LocalDate.of(2026, 3, 5))

        assertEquals("2026-03-05_11140_abc123.jpg", name)

        val back = PhotoObjectName.parse(name)!!
        assertEquals(PhotoId("abc123"), back.id)
        assertEquals(RegionCode("11140"), back.regionCode)
        assertEquals(LocalDate.of(2026, 3, 5), back.takenOn)
    }

    @Test
    fun `해외 시도 코드처럼 하이픈이 있어도 읽힌다`() {
        val name = PhotoObjectName.build(PhotoId("x1"), RegionCode("P-JPN-27"), LocalDate.of(2026, 1, 14))

        assertEquals(RegionCode("P-JPN-27"), PhotoObjectName.parse(name)!!.regionCode)
    }

    @Test
    fun `지역 코드에 밑줄이 있어도 자리로 잘라 읽는다`() {
        val name = PhotoObjectName.build(PhotoId("id9"), RegionCode("some_place"), LocalDate.of(2026, 2, 2))

        val back = PhotoObjectName.parse(name)!!
        assertEquals(RegionCode("some_place"), back.regionCode)
        assertEquals(PhotoId("id9"), back.id)
    }

    @Test
    fun `규칙에 안 맞는 이름은 건너뛴다 - 손으로 올린 파일이 섞여도 앱이 죽지 않게`() {
        assertNull(PhotoObjectName.parse("IMG_0001.jpg"))
        assertNull(PhotoObjectName.parse("2026-03-05_11140_abc123.png"))
        assertNull(PhotoObjectName.parse("not-a-date_11140_abc.jpg"))
        assertNull(PhotoObjectName.parse("2026-03-05-11140-abc.jpg"))
        assertNull(PhotoObjectName.parse(""))
    }

    @Test
    fun `사진 ID 에 경로 문자가 들어오면 만들 때 막는다`() {
        val failed = try {
            PhotoObjectName.build(PhotoId("../evil"), RegionCode("11140"), LocalDate.of(2026, 3, 5)); false
        } catch (e: IllegalArgumentException) {
            true
        }
        org.junit.Assert.assertTrue(failed)
    }
}
