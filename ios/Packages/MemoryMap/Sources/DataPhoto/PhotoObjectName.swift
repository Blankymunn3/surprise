import CoreModel
import Foundation

/// 사진 파일 이름에 지역과 날짜를 함께 적습니다 — `2026-03-05_11140_a1b2c3.jpg`
///
/// 지금은 로그인·Firestore 가 없어 사진 문서를 둘 곳이 없습니다. 목록 조회 한 번으로
/// 지역·날짜까지 알 수 있게 이름에 적었습니다. 안드로이드 `PhotoObjectName` 과 **같은 규칙**이라
/// 두 앱이 서로의 사진을 읽습니다. 로그인이 붙으면 문서로 옮기고 이 파서는 지웁니다.
public enum PhotoObjectName {

    public struct Parsed: Equatable, Sendable {
        public let id: PhotoId
        public let regionCode: RegionCode
        public let takenOn: CalendarDate

        /// 목록만 보고 만드는 **정렬용 값**. 찍은 날짜가 먼저, 같은 날이면 사진 ID 순입니다.
        ///
        /// 목록 API 도 파일 시스템도 '올린 시각' 을 주지 않습니다. 그때그때 다른 값을 쓰면
        /// 대표사진 기본값("가장 최근")이 실행할 때마다 흔들립니다. Swift 의 `hashValue` 는
        /// 실행마다 씨앗이 달라 쓸 수 없어 직접 셈합니다. 안드로이드와 같은 규칙입니다.
        public var stableOrder: Int {
            let day = takenOn.year * 10_000 + takenOn.month * 100 + takenOn.day
            let tail = id.value.unicodeScalars.reduce(0) { ($0 &* 31 &+ Int($1.value)) % 9_973 }
            return day * 10_000 + tail
        }
    }

    public static func build(id: PhotoId, regionCode: RegionCode, takenOn: CalendarDate) -> String {
        "\(takenOn.iso)_\(regionCode.value)_\(id.value).jpg"
    }

    /// 규칙에 안 맞으면 nil. 사람이 손으로 올린 파일이 섞여도 앱이 죽지 않게.
    /// 파싱은 **자리로** 합니다 — 지역 코드에 `_` 가 있어도 깨지지 않아야 합니다.
    public static func parse(_ fileName: String) -> Parsed? {
        guard fileName.hasSuffix(".jpg") else { return nil }
        let body = String(fileName.dropLast(4))
        guard body.count >= 13 else { return nil }

        let dateText = String(body.prefix(10))
        guard let date = CalendarDate(iso: dateText) else { return nil }

        let afterDate = body.index(body.startIndex, offsetBy: 10)
        guard body[afterDate] == "_" else { return nil }

        let rest = String(body[body.index(after: afterDate)...])
        guard let cut = rest.lastIndex(of: "_"), cut != rest.startIndex else { return nil }

        let region = String(rest[rest.startIndex..<cut])
        let id = String(rest[rest.index(after: cut)...])
        guard !region.isEmpty, PathSafe.isSafe(id) else { return nil }

        return Parsed(id: PhotoId(id), regionCode: RegionCode(region), takenOn: date)
    }
}
