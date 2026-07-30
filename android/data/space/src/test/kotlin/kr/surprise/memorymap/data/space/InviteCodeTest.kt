package kr.surprise.memorymap.data.space

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class InviteCodeTest {

    @Test
    fun `여섯 글자이고 헷갈리는 글자가 없다`() {
        repeat(200) {
            val code = InviteCode.generate(Random(it))
            assertEquals(6, code.length)
            assertTrue(code, code.none { c -> c in "OI01" })
        }
    }

    @Test
    fun `소문자나 하이픈으로 쳐도 받아 준다`() {
        assertEquals("ABC234", InviteCode.normalize(" abc-234 "))
    }

    @Test
    fun `길이가 안 맞으면 거절한다`() {
        assertNull(InviteCode.normalize("ABC"))
        assertNull(InviteCode.normalize("ABC2345"))
        assertNull(InviteCode.normalize(""))
    }

    @Test
    fun `코드가 곧 공간 ID 라 경로에 바로 쓸 수 있어야 한다`() {
        val code = InviteCode.generate(Random(7))
        assertTrue(kr.surprise.memorymap.core.model.PathSafe.isSafe(code))
    }
}
