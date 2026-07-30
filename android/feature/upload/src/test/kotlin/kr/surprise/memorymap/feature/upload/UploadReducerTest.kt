package kr.surprise.memorymap.feature.upload

import kr.surprise.memorymap.core.model.Region
import kr.surprise.memorymap.core.model.RegionCode
import kr.surprise.memorymap.core.model.SpaceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * State 가 android.net.Uri 를 들지 않기 때문에 JVM 에서 그대로 돌릴 수 있습니다.
 * 화면 상태에 플랫폼 타입을 넣지 않는 이유가 이것입니다.
 */
class UploadReducerTest {

    private val seoul = Region(RegionCode("11140"), "중구", "서울")
    private val space = SpaceId("ABC123")
    private val someDay = LocalDate.of(2026, 3, 5)

    private fun state(picked: Int = 0) = UploadState(
        spaceId = space,
        picked = List(picked) { PickedPhoto("content://photo/$it") },
    )

    @Test
    fun `지역이 비어 있으면 올릴 수 없다 - 지도에 올라갈 자리가 없다`() {
        val s = state(picked = 2).copy(takenOn = someDay, region = null)

        assertFalse(s.canUpload())
    }

    @Test
    fun `사진과 지역과 날짜가 다 있으면 올릴 수 있다`() {
        val s = state(picked = 2).copy(takenOn = someDay, region = seoul)

        assertTrue(s.canUpload())
    }

    @Test
    fun `올리는 중에는 버튼을 다시 누를 수 없다`() {
        val s = state(picked = 1).copy(takenOn = someDay, region = seoul, step = UploadStep.Uploading)

        assertFalse(s.canUpload())
    }

    @Test
    fun `직접 고르면 자동 딱지가 떨어진다`() {
        val s = state(picked = 3).copy(regionFromExif = true, regionMismatch = 2)

        val after = UploadReducer.regionChosen(s, seoul)

        assertFalse(after.regionFromExif)
        assertEquals(0, after.regionMismatch)
        assertFalse(after.pickingRegion)
    }

    @Test
    fun `사진마다 지역이 다르면 몇 장이 다른지 알려 준다`() {
        val s = state(picked = 3).copy(regionMismatch = 2)

        assertEquals("3장 중 2장은 다른 곳이에요", s.mismatchNotice())
    }

    @Test
    fun `알릴 것이 없으면 문구도 없다`() {
        assertNull(state(picked = 3).mismatchNotice())
    }

    @Test
    fun `올리기가 실패하면 기기에 저장했다는 상태로 간다`() {
        val after = UploadReducer.failed(state(picked = 1), savedLocally = true)

        assertEquals(UploadStep.Failed(savedLocally = true), after.step)
    }
}
