package kr.jjaguk.domain

import kr.jjaguk.core.model.PhotoId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoversTest {

    @Test
    fun `정해둔 대표가 없으면 가장 최근에 올린 사진이 대표다`() {
        val photos = listOf(photo("a", uploadedAt = 10), photo("b", uploadedAt = 30), photo("c", uploadedAt = 20))

        assertEquals(PhotoId("b"), Covers.resolve(photos, chosen = null))
    }

    @Test
    fun `정해둔 대표가 있으면 그대로 쓴다`() {
        val photos = listOf(photo("a", uploadedAt = 10), photo("b", uploadedAt = 30))

        assertEquals(PhotoId("a"), Covers.resolve(photos, chosen = PhotoId("a")))
    }

    @Test
    fun `대표사진이 지워지면 그다음으로 최근 사진이 대표가 된다`() {
        // 'a' 를 대표로 정해 뒀는데 목록에서 사라진 상황
        val left = listOf(photo("b", uploadedAt = 30), photo("c", uploadedAt = 20))

        assertEquals(PhotoId("b"), Covers.resolve(left, chosen = PhotoId("a")))
    }

    @Test
    fun `사진이 하나도 없으면 대표도 없다`() {
        assertNull(Covers.resolve(emptyList(), chosen = PhotoId("a")))
    }
}
