import CoreModel
import Domain
import Foundation
import Testing
@testable import FeatureUpload

@Suite("사진 올리기 상태")
struct UploadReducerTests {
    let seoul = Region(code: RegionCode("11140"), name: "중구", parentName: "서울")
    let day = CalendarDate(year: 2026, month: 3, day: 5)

    func state(picked: Int = 0) -> UploadState {
        var s = UploadState(spaceId: SpaceId("ABC123"))
        s.picked = (0..<picked).map { PickedPhoto(uri: "photo://\($0)") }
        return s
    }

    @Test("지역이 비어 있으면 올릴 수 없다 - 지도에 올라갈 자리가 없다")
    func regionRequired() {
        var s = state(picked: 2)
        s.takenOn = day
        #expect(!s.canUpload)
        s.region = seoul
        #expect(s.canUpload)
    }

    @Test("올리는 중에는 버튼을 다시 누를 수 없다")
    func noDoubleTap() {
        var s = state(picked: 1)
        s.takenOn = day
        s.region = seoul
        s.step = .uploading
        #expect(!s.canUpload)
    }

    @Test("직접 고르면 자동 딱지가 떨어진다")
    func manualClearsAuto() {
        var s = state(picked: 3)
        s.regionFromExif = true
        s.regionMismatch = 2

        let after = UploadReducer.regionChosen(s, seoul)
        #expect(!after.regionFromExif)
        #expect(after.regionMismatch == 0)
        #expect(!after.pickingRegion)
    }

    @Test("사진마다 지역이 다르면 몇 장이 다른지 알려 준다")
    func mismatchNotice() {
        var s = state(picked: 3)
        s.regionMismatch = 2
        #expect(s.mismatchNotice == "3장 중 2장은 다른 곳이에요")
    }

    @Test("알릴 것이 없으면 문구도 없다")
    func noNotice() {
        #expect(state(picked: 3).mismatchNotice == nil)
    }

    @Test("올리기가 실패하면 기기에 저장했다는 상태로 간다")
    func failureKeepsPhotos() {
        let after = UploadReducer.failed(state(picked: 1), savedLocally: true)
        #expect(after.step == .failed(savedLocally: true))
    }
}
