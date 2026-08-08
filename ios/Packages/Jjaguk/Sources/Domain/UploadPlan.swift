import CoreModel
import Foundation

/// 여러 장을 한 번에 올릴 때 지역·날짜 기본값을 정합니다.
/// 안드로이드 `UploadPlan` 과 같은 규칙 — 두 앱이 다르게 채우면 안 됩니다.
public enum UploadPlan {

    public struct ExifHint: Sendable, Equatable {
        public let takenOn: CalendarDate?
        public let regionCode: RegionCode?
        public init(takenOn: CalendarDate?, regionCode: RegionCode?) {
            self.takenOn = takenOn
            self.regionCode = regionCode
        }
    }

    public struct Defaults: Sendable, Equatable {
        public let regionCode: RegionCode?
        public let takenOn: CalendarDate
        public let regionMismatch: Int
        public let dateMismatch: Int
        public let regionFromExif: Bool
        public let dateFromExif: Bool
    }

    public static func defaults(hints: [ExifHint], today: CalendarDate) -> Defaults {
        let regions = hints.compactMap(\.regionCode)
        let dates = hints.compactMap(\.takenOn)

        let region = majority(regions)
        let date = majority(dates)

        return Defaults(
            regionCode: region,
            // 날짜를 하나도 못 읽으면 오늘. 오늘 찍은 사진을 오늘 올리는 게 가장 흔합니다.
            takenOn: date ?? today,
            regionMismatch: region.map { picked in regions.filter { $0 != picked }.count } ?? 0,
            dateMismatch: date.map { picked in dates.filter { $0 != picked }.count } ?? 0,
            regionFromExif: region != nil,
            dateFromExif: date != nil
        )
    }

    /// 가장 많이 나온 값. 같은 수면 **먼저 나온 쪽** — 사용자가 고른 순서가 보통
    /// 사진 순서라 첫 사진 기준이 덜 놀랍습니다.
    static func majority<T: Hashable>(_ values: [T]) -> T? {
        guard !values.isEmpty else { return nil }
        var counts: [T: Int] = [:]
        var best: T = values[0]
        var bestCount = 0
        for value in values {
            let next = (counts[value] ?? 0) + 1
            counts[value] = next
            if next > bestCount {
                best = value
                bestCount = next
            }
        }
        return best
    }
}
