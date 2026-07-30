package kr.surprise.memorymap.feature.space

import kr.surprise.memorymap.core.common.Failure
import kr.surprise.memorymap.core.model.Member
import kr.surprise.memorymap.core.model.MemberRole
import kr.surprise.memorymap.core.model.Space
import kr.surprise.memorymap.core.model.SpaceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceListReducerTest {

    private fun space(id: String, name: String = "우리 추억 지도") = Space(
        id = SpaceId(id),
        name = name,
        members = listOf(Member("u1", "나", MemberRole.Owner)),
        photoCount = 0,
        regionCount = 0,
        coverPhotoUrl = null,
        lastPhotoOn = null,
    )

    @Test
    fun `새로고침이 실패해도 보고 있던 목록은 지우지 않는다`() {
        val shown = SpaceListReducer.spacesLoaded(SpaceListState(), listOf(space("A")))

        val after = SpaceListReducer.loadFailed(shown, Failure.Network)

        assertTrue(after.spaces is SpacesUi.Ready)
        assertEquals(1, (after.spaces as SpacesUi.Ready).items.size)
    }

    @Test
    fun `처음부터 실패하면 실패 화면을 보여 준다`() {
        val after = SpaceListReducer.loadFailed(SpaceListState(), Failure.Network)

        assertTrue(after.spaces is SpacesUi.Failed)
    }

    @Test
    fun `공간을 만들면 초대 코드 시트가 바로 뜬다`() {
        val state = SpaceListReducer.spacesLoaded(SpaceListState(), emptyList())

        val after = SpaceListReducer.created(state, space("K7QF2M"), "K7QF2M")

        assertEquals(SpaceListSheet.Invited("우리 추억 지도", "K7QF2M"), after.sheet)
        assertFalse(after.working)
    }

    @Test
    fun `이미 들어가 있는 공간에 다시 참여해도 목록에 두 번 나오지 않는다`() {
        val state = SpaceListReducer.spacesLoaded(SpaceListState(), listOf(space("A", "가족 여행")))

        val after = SpaceListReducer.joined(state, space("A", "가족 여행"))

        assertEquals(1, (after.spaces as SpacesUi.Ready).items.size)
    }

    @Test
    fun `이름이 비어 있으면 만들기 버튼이 꺼진다`() {
        assertFalse(SpaceListState(pendingName = "   ").canCreate())
        assertTrue(SpaceListState(pendingName = "우리").canCreate())
    }

    @Test
    fun `만드는 중에는 버튼을 다시 누를 수 없다`() {
        assertFalse(SpaceListState(pendingName = "우리", working = true).canCreate())
    }

    @Test
    fun `코드가 여섯 글자가 안 되면 참여 버튼이 꺼진다`() {
        assertFalse(SpaceListState(pendingCode = "K7QF").canJoin())
        assertTrue(SpaceListState(pendingCode = "K7QF2M").canJoin())
    }

    @Test
    fun `시트를 닫으면 입력하던 값이 남지 않는다`() {
        val dirty = SpaceListState(pendingName = "쓰다 말았음", pendingCode = "ABC", working = true)

        val after = SpaceListReducer.sheetDismissed(dirty)

        assertEquals("", after.pendingName)
        assertEquals("", after.pendingCode)
        assertFalse(after.working)
    }
}
