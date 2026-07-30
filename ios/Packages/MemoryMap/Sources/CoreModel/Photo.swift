import Foundation

/// 사진 한 장.
///
/// `takenOn` 은 **날짜만** 입니다. 시각까지 저장하면 시간대 때문에 밤 11시에 찍은 사진이
/// 다음 날 칸으로 밀립니다. (`docs/app/SCREENS.md`)
public struct Photo: Hashable, Sendable, Identifiable {
    public let id: PhotoId
    public let regionCode: RegionCode
    public let takenOn: CalendarDate
    public let storagePath: String
    public let downloadURL: String
    public let uploadedBy: String
    public let uploadedAt: Int

    public init(
        id: PhotoId, regionCode: RegionCode, takenOn: CalendarDate,
        storagePath: String, downloadURL: String, uploadedBy: String, uploadedAt: Int
    ) {
        self.id = id
        self.regionCode = regionCode
        self.takenOn = takenOn
        self.storagePath = storagePath
        self.downloadURL = downloadURL
        self.uploadedBy = uploadedBy
        self.uploadedAt = uploadedAt
    }
}

/// 시각 없는 날짜. `Date` 를 쓰면 시간대에 따라 날짜가 밀려서 직접 만들었습니다.
public struct CalendarDate: Hashable, Sendable, Comparable, CustomStringConvertible {
    public let year: Int
    public let month: Int
    public let day: Int

    public init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    public init?(iso: String) {
        let parts = iso.split(separator: "-")
        guard parts.count == 3,
              let y = Int(parts[0]), let m = Int(parts[1]), let d = Int(parts[2]),
              (1...12).contains(m), (1...31).contains(d) else { return nil }
        self.init(year: y, month: m, day: d)
    }

    public var iso: String {
        String(format: "%04d-%02d-%02d", year, month, day)
    }

    public var description: String { iso }

    public static func < (lhs: CalendarDate, rhs: CalendarDate) -> Bool {
        (lhs.year, lhs.month, lhs.day) < (rhs.year, rhs.month, rhs.day)
    }
}

public enum CoverKey: Hashable, Sendable {
    case region(RegionCode)
    case day(CalendarDate)

    /// 문서 ID. `region_11140`, `day_2026-03-05` — 안드로이드와 같은 형식입니다.
    public var documentId: String {
        switch self {
        case .region(let code): return "region_\(code.value)"
        case .day(let date): return "day_\(date.iso)"
        }
    }
}

public struct Cover: Hashable, Sendable {
    public let key: CoverKey
    public let photoId: PhotoId
    public init(key: CoverKey, photoId: PhotoId) {
        self.key = key
        self.photoId = photoId
    }
}
