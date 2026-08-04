import CoreModel
import Domain
import Foundation
import Testing
@testable import FeatureUpload

@Suite("사진 올리기 상태")
struct UploadReducerTests {
    let seoul = Region(code: RegionCode("11140"), name: "중구", parentName: "서울")
    let busan = Region(code: RegionCode("21110"), name: "중구", parentName: "부산")
    let someDay = CalendarDate(year: 2026, month: 3, day: 5)
    let nextDay = CalendarDate(year: 2026, month: 3, day: 6)

    func item(_ index: Int, region: Region? = nil, day: CalendarDate? = nil) -> UploadItem {
        UploadItem(
            uri: "file://photo/\(index)",
            region: region ?? seoul,
            takenOn: day ?? someDay
        )
    }

    func state(_ items: UploadItem...) -> UploadState {
        var s = UploadState(spaceId: SpaceId("ABC123"))
        s.items = items
        return s
    }

    @Test("지역이 빈 사진이 하나라도 있으면 올릴 수 없다 — 지도에 올라갈 자리가 없다")
    func needsRegion() {
        var missing = item(1)
        missing.region = nil
        #expect(!state(item(0), missing).canUpload)
    }

    @Test("사진마다 지역이 있으면 올릴 수 있다")
    func canUpload() {
        #expect(state(item(0), item(1, region: busan)).canUpload)
    }

    @Test("올리는 중에는 버튼을 다시 누를 수 없다")
    func notWhileUploading() {
        var s = state(item(0))
        s.step = .uploading
        #expect(!s.canUpload)
    }

    /// 실패한 뒤에는 다시 눌러야 하므로 버튼이 살아 있어야 합니다.
    @Test("실패한 뒤에는 다시 올릴 수 있다")
    func canRetryAfterFailure() {
        var s = state(item(0))
        s.step = .failed(savedLocally: true)
        #expect(s.canUpload)
    }

    @Test("직접 고르면 그 사진의 자동 딱지만 떨어진다")
    func autoBadgeDropsForOne() {
        var first = item(0)
        first.regionAuto = true
        var second = item(1)
        second.regionAuto = true

        var s = state(first, second)
        s.editingRegionOf = "file://photo/0"

        let after = UploadReducer.regionChosen(s, busan)

        #expect(after.items[0].region == busan)
        #expect(!after.items[0].regionAuto)
        // 옆 사진은 건드리지 않습니다
        #expect(after.items[1].region == seoul)
        #expect(after.items[1].regionAuto)
        #expect(after.editingRegionOf == nil)
    }

    @Test("날짜를 고치면 그 사진만 바뀐다")
    func dateChangesOne() {
        let after = UploadReducer.dateChosen(state(item(0), item(1)), "file://photo/1", nextDay)

        #expect(after.items[0].takenOn == someDay)
        #expect(after.items[1].takenOn == nextDay)
        #expect(!after.items[1].dateAuto)
    }

    @Test("여러 곳 여러 날에 걸치면 나눠 올라간다고 알려 준다")
    func splitNotice() {
        let s = state(item(0), item(1, region: busan, day: nextDay))
        #expect(s.splitNotice == "지역 2곳 · 날짜 2일로 나눠 올라가요")
    }

    @Test("한 곳 한 날이면 알릴 것이 없다")
    func noSplitNotice() {
        #expect(state(item(0), item(1)).splitNotice == nil)
    }

    @Test("올리기가 실패하면 기기에 저장했다는 상태로 간다")
    func failureKeepsPhotos() {
        let after = UploadReducer.failed(state(item(0)), savedLocally: true)
        #expect(after.step == .failed(savedLocally: true))
    }

    /// '다시 시도' 는 고쳐 둔 값을 지우지 않습니다.
    @Test("다시 시도해도 고친 값은 남는다")
    func retryKeepsEdits() {
        var s = state(item(0, region: busan))
        s.step = .failed(savedLocally: true)

        let after = UploadReducer.retry(s)

        #expect(after.step == .editing)
        #expect(after.items[0].region == busan)
    }
}
