import Foundation

/// 공간 하나. 사진이 이 단위로 모입니다.
public struct SpaceId: Hashable, Sendable {
    public let value: String
    public init(_ value: String) { self.value = value }
}

/// 사진 하나. 파일 이름이기도 합니다 — `spaces/<공간ID>/photos/<사진ID>.jpg`
public struct PhotoId: Hashable, Sendable {
    public let value: String
    public init(_ value: String) { self.value = value }
}

/// 경로에 쓰이는 값이라 `[A-Za-z0-9_-]` 만 허용합니다.
/// 그 외 문자가 오면 상위 디렉터리로 빠져나갈 수 있습니다. 안드로이드 `PathSafe` 와 같은 규칙.
public enum PathSafe {
    public static func isSafe(_ value: String) -> Bool {
        guard !value.isEmpty else { return false }
        return value.allSatisfy { $0.isASCII && ($0.isLetter || $0.isNumber || $0 == "_" || $0 == "-") }
    }
}
