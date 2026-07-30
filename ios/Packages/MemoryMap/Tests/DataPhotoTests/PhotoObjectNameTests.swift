import CoreModel
import Testing
@testable import DataPhoto

@Suite("사진 파일 이름")
struct PhotoObjectNameTests {

    @Test("이름을 만들고 다시 읽으면 그대로다")
    func roundTrip() {
        let name = PhotoObjectName.build(
            id: PhotoId("abc123"), regionCode: RegionCode("11140"),
            takenOn: CalendarDate(year: 2026, month: 3, day: 5)
        )
        #expect(name == "2026-03-05_11140_abc123.jpg")

        let back = PhotoObjectName.parse(name)
        #expect(back?.id == PhotoId("abc123"))
        #expect(back?.regionCode == RegionCode("11140"))
        #expect(back?.takenOn == CalendarDate(year: 2026, month: 3, day: 5))
    }

    @Test("해외 시도 코드처럼 하이픈이 있어도 읽힌다")
    func hyphenRegion() {
        let name = PhotoObjectName.build(
            id: PhotoId("x1"), regionCode: RegionCode("P-JPN-27"),
            takenOn: CalendarDate(year: 2026, month: 1, day: 14)
        )
        #expect(PhotoObjectName.parse(name)?.regionCode == RegionCode("P-JPN-27"))
    }

    @Test("지역 코드에 밑줄이 있어도 자리로 잘라 읽는다")
    func underscoreRegion() {
        let name = PhotoObjectName.build(
            id: PhotoId("id9"), regionCode: RegionCode("some_place"),
            takenOn: CalendarDate(year: 2026, month: 2, day: 2)
        )
        let back = PhotoObjectName.parse(name)
        #expect(back?.regionCode == RegionCode("some_place"))
        #expect(back?.id == PhotoId("id9"))
    }

    @Test("규칙에 안 맞는 이름은 건너뛴다")
    func rejectsJunk() {
        #expect(PhotoObjectName.parse("IMG_0001.jpg") == nil)
        #expect(PhotoObjectName.parse("2026-03-05_11140_abc123.png") == nil)
        #expect(PhotoObjectName.parse("not-a-date_11140_abc.jpg") == nil)
        #expect(PhotoObjectName.parse("") == nil)
    }

    @Test("안드로이드가 만든 이름과 형식이 같다")
    func sameAsAndroid() {
        // 안드로이드 PhotoObjectNameTest 와 같은 기대값입니다.
        #expect(PhotoObjectName.build(
            id: PhotoId("abc123"), regionCode: RegionCode("11140"),
            takenOn: CalendarDate(year: 2026, month: 3, day: 5)
        ) == "2026-03-05_11140_abc123.jpg")
    }
}
