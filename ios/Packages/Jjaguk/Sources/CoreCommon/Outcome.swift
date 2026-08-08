import Foundation

/// 도메인은 예외를 던지지 않고 이걸 돌려줍니다 (`docs/app/CONVENTIONS.md`).
public enum Outcome<Success: Sendable>: Sendable {
    case ok(Success)
    case fail(Failure)

    public var value: Success? {
        if case .ok(let v) = self { return v }
        return nil
    }
}

public enum Failure: Sendable, Equatable {
    case network
    case timeout
    case notFound
    case denied
    case tooLarge
    case unknown
}

/// 웹·안드로이드와 **똑같이** 유지해야 하는 값들입니다. 한쪽만 바꾸면 기기마다 다르게 동작합니다.
public enum Limits {
    public static let listTimeout: TimeInterval = 15
    public static let uploadTimeout: TimeInterval = 25
    public static let maxEdgePx = 760
    public static let jpegQuality = 0.72
    public static let maxUploadBytes = 5 * 1024 * 1024
}
