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
    private val busan = Region(RegionCode("21110"), "중구", "부산")
    private val space = SpaceId("ABC123")
    private val someDay = LocalDate.of(2026, 3, 5)
    private val nextDay = LocalDate.of(2026, 3, 6)

    private fun item(index: Int, region: Region? = seoul, day: LocalDate = someDay) =
        UploadItem(uri = "content://photo/$index", region = region, takenOn = day)

    private fun state(vararg items: UploadItem) =
        UploadState(spaceId = space, items = items.toList())

    @Test
    fun `지역이 빈 사진이 하나라도 있으면 올릴 수 없다 - 지도에 올라갈 자리가 없다`() {
        val s = state(item(0), item(1, region = null))

        assertFalse(s.canUpload())
    }

    @Test
    fun `사진마다 지역이 있으면 올릴 수 있다`() {
        val s = state(item(0), item(1, region = busan))

        assertTrue(s.canUpload())
    }

    @Test
    fun `올리는 중에는 버튼을 다시 누를 수 없다`() {
        val s = state(item(0)).copy(step = UploadStep.Uploading)

        assertFalse(s.canUpload())
    }

    /** 실패한 뒤에는 다시 눌러야 하므로 버튼이 살아 있어야 합니다. */
    @Test
    fun `실패한 뒤에는 다시 올릴 수 있다`() {
        val s = state(item(0)).copy(step = UploadStep.Failed(savedLocally = true))

        assertTrue(s.canUpload())
    }

    @Test
    fun `직접 고르면 그 사진의 자동 딱지만 떨어진다`() {
        val s = state(
            item(0).copy(regionAuto = true),
            item(1).copy(regionAuto = true),
        ).copy(editingRegionOf = "content://photo/0")

        val after = UploadReducer.regionChosen(s, busan)

        assertEquals(busan, after.items[0].region)
        assertFalse(after.items[0].regionAuto)
        // 옆 사진은 건드리지 않습니다
        assertEquals(seoul, after.items[1].region)
        assertTrue(after.items[1].regionAuto)
        assertNull(after.editingRegionOf)
    }

    @Test
    fun `날짜를 고치면 그 사진만 바뀐다`() {
        val s = state(item(0).copy(dateAuto = true), item(1).copy(dateAuto = true))

        val after = UploadReducer.dateChosen(s, "content://photo/1", nextDay)

        assertEquals(someDay, after.items[0].takenOn)
        assertEquals(nextDay, after.items[1].takenOn)
        assertFalse(after.items[1].dateAuto)
    }

    @Test
    fun `여러 곳 여러 날에 걸치면 나눠 올라간다고 알려 준다`() {
        val s = state(item(0), item(1, region = busan, day = nextDay))

        assertEquals("지역 2곳 · 날짜 2일로 나눠 올라가요", s.splitNotice())
    }

    @Test
    fun `한 곳 한 날이면 알릴 것이 없다`() {
        assertNull(state(item(0), item(1)).splitNotice())
    }

    @Test
    fun `올리기가 실패하면 기기에 저장했다는 상태로 간다`() {
        val after = UploadReducer.failed(state(item(0)), savedLocally = true)

        assertEquals(UploadStep.Failed(savedLocally = true), after.step)
    }

    /** '다시 시도' 는 고쳐 둔 값을 지우지 않습니다. */
    @Test
    fun `다시 시도해도 고친 값은 남는다`() {
        val s = state(item(0, region = busan)).copy(step = UploadStep.Failed(savedLocally = true))

        val after = UploadReducer.retry(s)

        assertEquals(UploadStep.Editing, after.step)
        assertEquals(busan, after.items[0].region)
    }
}
