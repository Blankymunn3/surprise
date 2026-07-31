import CoreModel
import Foundation

/// `covers.json` 의 모양. `{"region_11140":"a1b2c3", "day_2026-03-05":"d4e5f6"}`
///
/// 서버에 두든 기기에 두든 **같은 형식**입니다. 혼자 쓰던 짜국을 나중에 둘이로 바꿀 때
/// 파일을 그대로 올리면 되도록 하려는 것입니다 (`docs/app/AUTH.md`).
/// 안드로이드 `CoversFile` 과 같은 규칙입니다.
enum CoversFile {

    static func parse(_ data: Data) -> [Cover] {
        guard let raw = try? JSONSerialization.jsonObject(with: data) as? [String: String] else {
            return []
        }
        return raw.compactMap { documentId, photoId in
            guard let key = key(from: documentId) else { return nil }
            return Cover(key: key, photoId: PhotoId(photoId))
        }
    }

    static func data(_ covers: [Cover]) -> Data? {
        let dictionary = Dictionary(covers.map { ($0.key.documentId, $0.photoId.value) },
                                    uniquingKeysWith: { _, last in last })
        return try? JSONSerialization.data(withJSONObject: dictionary)
    }

    static func key(from documentId: String) -> CoverKey? {
        if documentId.hasPrefix("region_") {
            return .region(RegionCode(String(documentId.dropFirst(7))))
        }
        if documentId.hasPrefix("day_"), let date = CalendarDate(iso: String(documentId.dropFirst(4))) {
            return .day(date)
        }
        return nil
    }
}
