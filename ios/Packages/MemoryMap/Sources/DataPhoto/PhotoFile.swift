import CoreModel
import Foundation
import ImageIO
import UniformTypeIdentifiers

/**
 사진 파일에서 **찍은 날짜·좌표를 읽고**, 올릴 크기로 **줄입니다.**

 안드로이드 `ExifReader` · `PhotoScaler` 와 **같은 값**을 씁니다. 두 앱이 다르게 줄이면
 같은 사진이 폰마다 다른 크기로 올라갑니다.

 - 긴 변 **760px** (그보다 작으면 그대로 둡니다 — 늘리면 화질만 나빠집니다)
 - JPEG 품질 **0.72**

 `UIImage` 를 쓰지 않고 ImageIO 만 씁니다. 패키지가 맥에서도 빌드돼야 `swift test` 가
 돌기 때문입니다 (`Package.swift` 참고).
 */
public enum PhotoFile {

    public static let maxEdge = 760
    public static let quality = 0.72

    /// EXIF 에서 읽어낸 것. 좌표를 지역으로 바꾸는 일은 **여기서 하지 않습니다** —
    /// 지역 판정은 도메인(RegionCatalog)의 몫이고, 여기는 파일만 봅니다.
    public struct Hint: Sendable, Equatable {
        public let takenOn: CalendarDate?
        public let latitude: Double?
        public let longitude: Double?

        public var coordinate: (Double, Double)? {
            guard let latitude, let longitude else { return nil }
            return (latitude, longitude)
        }
    }

    public static func hint(at path: String) -> Hint {
        guard let source = imageSource(path),
              let all = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any]
        else { return Hint(takenOn: nil, latitude: nil, longitude: nil) }

        return Hint(
            takenOn: takenOn(from: all),
            latitude: latitude(from: all),
            longitude: longitude(from: all)
        )
    }

    /// 줄여서 JPEG 로. 못 읽으면 nil — 부르는 쪽에서 그 장만 건너뜁니다.
    public static func jpeg(at path: String) -> Data? {
        guard let source = imageSource(path) else { return nil }

        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            // 세워 찍은 사진이 눕지 않게. 이걸 빼면 EXIF 회전이 무시됩니다.
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxEdge,
        ]
        guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
        else { return nil }

        let out = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(
            out, UTType.jpeg.identifier as CFString, 1, nil
        ) else { return nil }

        CGImageDestinationAddImage(
            destination, image,
            [kCGImageDestinationLossyCompressionQuality: quality] as CFDictionary
        )
        guard CGImageDestinationFinalize(destination) else { return nil }
        return out as Data
    }

    // MARK: - 안쪽

    private static func imageSource(_ path: String) -> CGImageSource? {
        let url = URL(fileURLWithPath: path) as CFURL
        return CGImageSourceCreateWithURL(url, nil)
    }

    /// EXIF 날짜는 `2026:03:05 14:22:01` 꼴입니다. 앞 10글자만 쓰고 `:` 를 `-` 로 바꿉니다.
    static func takenOn(from all: [CFString: Any]) -> CalendarDate? {
        guard let exif = all[kCGImagePropertyExifDictionary] as? [CFString: Any] else { return nil }
        let raw = (exif[kCGImagePropertyExifDateTimeOriginal] as? String)
            ?? (exif[kCGImagePropertyExifDateTimeDigitized] as? String)
        guard let raw, raw.count >= 10 else { return nil }
        return CalendarDate(iso: String(raw.prefix(10)).replacingOccurrences(of: ":", with: "-"))
    }

    /// GPS 는 항상 양수로 적히고 남/서인지는 따로 적힙니다. 부호를 여기서 붙입니다.
    static func latitude(from all: [CFString: Any]) -> Double? {
        guard let gps = all[kCGImagePropertyGPSDictionary] as? [CFString: Any],
              let value = gps[kCGImagePropertyGPSLatitude] as? Double else { return nil }
        let south = (gps[kCGImagePropertyGPSLatitudeRef] as? String) == "S"
        return south ? -value : value
    }

    static func longitude(from all: [CFString: Any]) -> Double? {
        guard let gps = all[kCGImagePropertyGPSDictionary] as? [CFString: Any],
              let value = gps[kCGImagePropertyGPSLongitude] as? Double else { return nil }
        let west = (gps[kCGImagePropertyGPSLongitudeRef] as? String) == "W"
        return west ? -value : value
    }
}
