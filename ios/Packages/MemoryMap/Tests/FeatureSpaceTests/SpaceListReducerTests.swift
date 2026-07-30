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

    @Test("이미 들어가 있는 공간에 다시 참여해도 목록에 두 번 나오지 않는다")
    func joinIsIdempotent() {
        let state = SpaceListReducer.loaded(SpaceListState(), [space("A", "가족 여행")])
        let after = SpaceListReducer.joined(state, space("A", "가족 여행"))
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
