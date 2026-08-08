package kr.jjaguk.feature.space

import kr.jjaguk.core.common.Failure
import kr.jjaguk.core.model.Member
import kr.jjaguk.core.model.MemberRole
import kr.jjaguk.core.model.Space
import kr.jjaguk.core.model.SpaceId
import kr.jjaguk.core.model.SpaceKind
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

        // 코드가 있는 건 같이 쓰는 짜국뿐입니다.
        val made = space("K7QF2M").copy(kind = SpaceKind.Shared)
        val after = SpaceListReducer.created(state, made, "K7QF2M")

        // id·종류까지 실려야 그 화면의 '짜국 열기' 가 바로 들어갈 수 있습니다.
        assertEquals(
            SpaceListSheet.Invited(SpaceId("K7QF2M"), SpaceKind.Shared, "우리 추억 지도", "K7QF2M"),
            after.sheet,
        )
        assertFalse(after.working)
    }

    /**
     * 공간을 만들면 저장소가 먼저 목록에 넣고(흐름으로 `spacesLoaded` 가 돌고), 그 다음
     * `created` 가 또 넣습니다. 그래서 같은 공간이 두 번 들어가 목록이 죽었습니다
     * (`Key "7M8FRY" was already used`).
     */
    @Test
    fun `저장소가 먼저 넣어 둔 공간을 만들어도 목록에 두 번 나오지 않는다`() {
        val made = space("7M8FRY")
        val alreadyInList = SpaceListReducer.spacesLoaded(SpaceListState(), listOf(made))

        val after = SpaceListReducer.created(alreadyInList, made, "7M8FRY")

        val items = (after.spaces as SpacesUi.Ready).items
        assertEquals(1, items.size)
        assertEquals(items.size, items.distinctBy { it.id }.size)
    }

    @Test
    fun `이미 들어가 있는 공간에 다시 참여해도 목록에 두 번 나오지 않는다`() {
        val state = SpaceListReducer.spacesLoaded(SpaceListState(), listOf(space("A", "가족 여행")))

        val after = SpaceListReducer.joined(state, space("A", "가족 여행"))

        assertEquals(1, (after.spaces as SpacesUi.Ready).items.size)
    }

    /**
     * 기본이 혼자여야 하는 이유: 잘못 골라도 사진이 폰 밖으로 나가지 않습니다.
     * 반대로 두면 무심코 넘긴 사람의 사진이 서버로 갑니다.
     */
    @Test
    fun `만들기 시트를 열면 혼자가 기본으로 잡힌다`() {
        val opened = SpaceListReducer.sheetOpened(SpaceListState(), SpaceListSheet.Create)

        assertEquals(SpaceKind.Personal, opened.pendingKind)
    }

    @Test
    fun `지난번에 같이를 골랐어도 시트를 다시 열면 혼자로 돌아온다`() {
        val chose = SpaceListReducer.kindSelected(SpaceListState(), SpaceKind.Shared)

        val reopened = SpaceListReducer.sheetOpened(chose, SpaceListSheet.Create)

        assertEquals(SpaceKind.Personal, reopened.pendingKind)
    }

    /** 혼자 짜국에는 초대 코드가 없습니다 — 보여 줄 것이 없으니 시트를 닫습니다. */
    @Test
    fun `혼자 짜국을 만들면 초대 코드 시트가 뜨지 않는다`() {
        val state = SpaceListReducer.spacesLoaded(SpaceListState(), emptyList())

        val after = SpaceListReducer.created(state, space("A1B2C3"), null)

        assertEquals(SpaceListSheet.None, after.sheet)
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
