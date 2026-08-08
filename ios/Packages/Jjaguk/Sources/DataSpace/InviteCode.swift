import CoreModel
import Foundation

/// 초대 코드가 곧 **공간 ID** 입니다. 안드로이드 `InviteCode` 와 같은 규칙 —
/// 한쪽에서 만든 코드를 다른 쪽에서 그대로 씁니다.
///
/// ⚠️ 코드를 아는 사람은 누구나 그 공간을 보고 넣을 수 있습니다. 지금 웹과 같은 약점이고
/// 로그인이 붙어야 해결됩니다 (`docs/app/SPACES.md`).
public enum InviteCode {
    /// 0/O, 1/I 처럼 헷갈리는 글자는 뺍니다 — 코드를 말로 불러 줄 일이 있습니다.
    static let alphabet = Array("ABCDEFGHJKMNPQRSTUVWXYZ23456789")
    static let length = 6

    public static func generate(using generator: inout some RandomNumberGenerator) -> String {
        String((0..<length).map { _ in alphabet.randomElement(using: &generator)! })
    }

    public static func generate() -> String {
        var generator = SystemRandomNumberGenerator()
        return generate(using: &generator)
    }

    /// 소문자·공백·하이픈으로 쳐도 받아 줍니다.
    public static func normalize(_ raw: String) -> String? {
        let cleaned = raw.uppercased().filter { alphabet.contains($0) }
        return cleaned.count == length ? cleaned : nil
    }
}
