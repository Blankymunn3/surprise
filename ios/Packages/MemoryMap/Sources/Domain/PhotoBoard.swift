import CoreModel

/// 한 공간의 사진 전부를 지도용·달력용으로 갈라 둔 것.
/// 지도 Store 와 달력 Store 가 **같은 이것**을 봅니다 — 탭을 옮길 때 다시 받지 않으려고.
public struct PhotoBoard: Sendable, Equatable {
    public let photos: [Photo]
    private let chosenCovers: [String: PhotoId]

    public let byRegion: [RegionCode: [Photo]]
    public let byDay: [CalendarDate: [Photo]]

    public init(photos: [Photo], covers: [Cover]) {
        self.photos = photos
        self.chosenCovers = Dictionary(
            covers.map { ($0.key.documentId, $0.photoId) },
            uniquingKeysWith: { _, last in last }
        )
        self.byRegion = Dictionary(grouping: photos, by: \.regionCode)
            .mapValues { $0.sorted { $0.uploadedAt > $1.uploadedAt } }
        self.byDay = Dictionary(grouping: photos, by: \.takenOn)
            .mapValues { $0.sorted { $0.uploadedAt > $1.uploadedAt } }
    }

    public static let empty = PhotoBoard(photos: [], covers: [])

    public var regionCount: Int { byRegion.count }

    public func photos(in code: RegionCode) -> [Photo] { byRegion[code] ?? [] }
    public func photos(on date: CalendarDate) -> [Photo] { byDay[date] ?? [] }

    /// 지도에서 그 지역을 칠할 사진
    public func regionCover(_ code: RegionCode) -> Photo? {
        cover(key: .region(code), candidates: photos(in: code))
    }

    /// 달력 칸에 놓을 사진
    public func dayCover(_ date: CalendarDate) -> Photo? {
        cover(key: .day(date), candidates: photos(on: date))
    }

    private func cover(key: CoverKey, candidates: [Photo]) -> Photo? {
        let chosen = chosenCovers[key.documentId]
        guard let id = Covers.resolve(candidates, chosen: chosen) else { return nil }
        return candidates.first { $0.id == id }
    }
}
