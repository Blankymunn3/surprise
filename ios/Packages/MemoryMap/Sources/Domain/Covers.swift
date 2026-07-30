import CoreModel

/// 대표사진을 고르는 규칙. 안드로이드 `Covers` 와 같습니다.
public enum Covers {

    /// 정해진 대표가 없으면 **가장 최근에 올린 사진**.
    public static func fallback(_ photos: [Photo]) -> PhotoId? {
        photos.max(by: { $0.uploadedAt < $1.uploadedAt })?.id
    }

    /// 정해 둔 대표가 이미 지워졌으면 없는 셈 치고 fallback 으로 내려갑니다.
    public static func resolve(_ photos: [Photo], chosen: PhotoId?) -> PhotoId? {
        guard !photos.isEmpty else { return nil }
        if let chosen, photos.contains(where: { $0.id == chosen }) { return chosen }
        return fallback(photos)
    }
}
