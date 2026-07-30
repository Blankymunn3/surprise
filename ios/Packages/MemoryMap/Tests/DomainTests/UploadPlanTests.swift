import CoreModel
import Testing
@testable import Domain

@Suite("올리기 기본값")
struct UploadPlanTests {
    let today = CalendarDate(year: 2026, month: 7, day: 30)
    let seoul = RegionCode("11140")
    let osaka = RegionCode("P-JPN-27")

    @Test("사진에서 읽은 지역과 날짜를 기본값으로 채운다")
    func fromExif() {
        let day = CalendarDate(year: 2026, month: 3, day: 5)
        let d = UploadPlan.defaults(
            hints: [.init(takenOn: day, regionCode: seoul), .init(takenOn: day, regionCode: seoul)],
            today: today
        )
        #expect(d.regionCode == seoul)
        #expect(d.takenOn == day)
        #expect(d.regionFromExif && d.dateFromExif)
        #expect(d.regionMismatch == 0)
    }

    @Test("사진마다 지역이 다르면 많은 쪽을 기본으로 두고 몇 장이 다른지 알린다")
    func mismatch() {
        let d = UploadPlan.defaults(
            hints: [
                .init(takenOn: nil, regionCode: seoul),
                .init(takenOn: nil, regionCode: seoul),
                .init(takenOn: nil, regionCode: osaka),
            ],
            today: today
        )
        #expect(d.regionCode == seoul)
        #expect(d.regionMismatch == 1)
    }

    @Test("날짜를 하나도 못 읽으면 오늘로 둔다")
    func fallsBackToToday() {
        let d = UploadPlan.defaults(hints: [.init(takenOn: nil, regionCode: nil)], today: today)
        #expect(d.takenOn == today)
        #expect(!d.dateFromExif)
    }

    @Test("지역을 못 읽으면 비워 둔다 - 사용자가 반드시 골라야 한다")
    func regionStaysEmpty() {
        let d = UploadPlan.defaults(
            hints: [.init(takenOn: CalendarDate(year: 2026, month: 3, day: 5), regionCode: nil)],
            today: today
        )
        #expect(d.regionCode == nil)
    }

    @Test("같은 수로 갈리면 먼저 나온 쪽을 쓴다")
    func firstWinsOnTie() {
        let d = UploadPlan.defaults(
            hints: [.init(takenOn: nil, regionCode: osaka), .init(takenOn: nil, regionCode: seoul)],
            today: today
        )
        #expect(d.regionCode == osaka)
    }
}
