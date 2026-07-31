import CoreCommon
import CoreModel
import Foundation
import Testing
@testable import FeatureSpace

@Suite("공간 목록 상태")
struct SpaceListReducerTests {

    func space(_ id: String, _ name: String = "우리 추억 지도") -> Space {
        Space(
            spaceId: SpaceId(id), name: name,
            members: [Member(uid: "u1", displayName: "나", role: .owner)]
        )
    }

    @Test("새로고침이 실패해도 보고 있던 목록은 지우지 않는다")
    func keepsShownList() {
        let shown = SpaceListReducer.loaded(SpaceListState(), [space("A")])
        let after = SpaceListReducer.loadFailed(shown, .network)
        #expect(after.spaces == .ready([space("A")]))
    }

    @Test("처음부터 실패하면 실패 화면을 보여 준다")
    func showsFailure() {
        let after = SpaceListReducer.loadFailed(SpaceListState(), .network)
        #expect(after.spaces == .failed(.network))
    }

    @Test("공간을 만들면 초대 코드 시트가 바로 뜬다")
    func inviteSheetAppears() {
        let state = SpaceListReducer.loaded(SpaceListState(), [])
        let after = SpaceListReducer.created(state, space("K7QF2M"), "K7QF2M")
        #expect(after.sheet == .invited(spaceName: "우리 추억 지도", code: "K7QF2M"))
        #expect(!after.working)
    }

    /// 저장소가 먼저 목록에 넣고 `created` 가 또 넣어서 같은 공간이 두 번 들어갔습니다.
    /// 안드로이드에서는 그 상태로 목록이 죽었습니다 (`Key "7M8FRY" was already used`).
    @Test("저장소가 먼저 넣어 둔 공간을 만들어도 목록에 두 번 나오지 않는다")
    func createIsIdempotent() {
        let made = space("7M8FRY")
        let alreadyInList = SpaceListReducer.loaded(SpaceListState(), [made])
        let after = SpaceListReducer.created(alreadyInList, made, "7M8FRY")
        #expect(after.items.count == 1)
    }

    @Test("이미 들어가 있는 공간에 다시 참여해도 목록에 두 번 나오지 않는다")
    func joinIsIdempotent() {
        let state = SpaceListReducer.loaded(SpaceListState(), [space("A", "가족 여행")])
        let after = SpaceListReducer.joined(state, space("A", "가족 여행"))
        #expect(after.items.count == 1)
    }

    /// 기본이 혼자여야 하는 이유: 잘못 골라도 사진이 폰 밖으로 나가지 않습니다.
    /// 반대로 두면 무심코 넘긴 사람의 사진이 서버로 갑니다.
    @Test("만들기 시트를 열면 혼자가 기본으로 잡힌다")
    func personalIsDefault() {
        let opened = SpaceListReducer.sheetOpened(SpaceListState(), .create)
        #expect(opened.pendingKind == .personal)
    }

    @Test("지난번에 같이를 골랐어도 시트를 다시 열면 혼자로 돌아온다")
    func kindResetsOnReopen() {
        let chose = SpaceListReducer.kindSelected(SpaceListState(), .shared)
        let reopened = SpaceListReducer.sheetOpened(chose, .create)
        #expect(reopened.pendingKind == .personal)
    }

    /// 혼자 짜국에는 초대 코드가 없습니다 — 보여 줄 것이 없으니 시트를 닫습니다.
    @Test("혼자 짜국을 만들면 초대 코드 시트가 뜨지 않는다")
    func personalHasNoInviteSheet() {
        let state = SpaceListReducer.loaded(SpaceListState(), [])
        let after = SpaceListReducer.created(state, space("A1B2C3"), nil)
        #expect(after.sheet == .none)
        #expect(after.items.count == 1)
    }

    @Test("이름이 비면 만들기 버튼이 꺼진다")
    func createDisabled() {
        var s = SpaceListState()
        s.pendingName = "   "
        #expect(!s.canCreate)
        s.pendingName = "우리"
        #expect(s.canCreate)
        s.working = true
        #expect(!s.canCreate)
    }

    @Test("시트를 닫으면 입력하던 값이 남지 않는다")
    func sheetClears() {
        var dirty = SpaceListState()
        dirty.pendingName = "쓰다 말았음"
        dirty.pendingCode = "ABC"
        dirty.working = true

        let after = SpaceListReducer.sheetDismissed(dirty)
        #expect(after.pendingName.isEmpty)
        #expect(after.pendingCode.isEmpty)
        #expect(!after.working)
    }
}
