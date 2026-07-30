import CoreModel
import Testing
@testable import Domain

@Suite("사진 판")
struct PhotoBoardTests {
    let mar5 = CalendarDate(year: 2026, month: 3, day: 5)
    let mar21 = CalendarDate(year: 2026, month: 3, day: 21)
    let seoul = RegionCode("11140")
    let osaka = RegionCode("P-JPN-27")

    func photo(_ id: String, _ region: RegionCode, _ day: CalendarDate, _ at: Int) -> Photo {
        Photo(
            id: PhotoId(id), regionCode: region, takenOn: day,
            storagePath: "spaces/s1/photos/\(id).jpg",
            downloadURL: "https://example.invalid/\(id).jpg",
            uploadedBy: "u1", uploadedAt: at
        )
    }

    var photos: [Photo] {
        [photo("a", seoul, mar5, 10), photo("b", seoul, mar5, 30), photo("c", osaka, mar21, 20)]
    }

    @Test("같은 사진을 지역별과 날짜별로 함께 본다")
    func bothViews() {
        let board = PhotoBoard(photos: photos, covers: [])
        #expect(board.photos(in: seoul).count == 2)
        #expect(board.photos(on: mar21).count == 1)
        #expect(board.regionCount == 2)
    }

    @Test("대표를 안 정하면 가장 최근 사진이 지역을 칠한다")
    func defaultCover() {
        let board = PhotoBoard(photos: photos, covers: [])
        #expect(board.regionCover(seoul)?.id == PhotoId("b"))
    }

    @Test("정해둔 대표가 지도와 달력에 각각 따로 적용된다")
    func separateCovers() {
        let board = PhotoBoard(photos: photos, covers: [
            Cover(key: .region(seoul), photoId: PhotoId("a")),
            Cover(key: .day(mar5), photoId: PhotoId("b")),
        ])
        #expect(board.regionCover(seoul)?.id == PhotoId("a"))
        #expect(board.dayCover(mar5)?.id == PhotoId("b"))
    }

    @Test("대표사진이 지워지면 그다음으로 최근 사진이 대표가 된다")
    func coverGone() {
        let left = [photo("b", seoul, mar5, 30), photo("c", seoul, mar5, 20)]
        #expect(Covers.resolve(left, chosen: PhotoId("a")) == PhotoId("b"))
    }

    @Test("사진이 없는 지역과 날짜는 대표도 없다")
    func noCover() {
        let board = PhotoBoard(photos: photos, covers: [])
        #expect(board.regionCover(RegionCode("99999")) == nil)
        #expect(board.dayCover(CalendarDate(year: 2026, month: 1, day: 1)) == nil)
    }
}
