import Foundation

/**
 로그인이 없어서 **기기에서 한 번 만들어 두고 계속 쓰는** 내 식별자.

 공간 문서의 멤버(`space.json`)와 사진의 올린이가 **같은 값**이어야 "이 사진 내가 올렸나"
 를 판단할 수 있습니다. 두 곳에서 따로 만들면 조용히 어긋나므로 한 곳에 둡니다.

 로그인이 붙으면 통째로 사라집니다 — 그때는 계정의 uid 를 씁니다.
 */
public enum DeviceIdentity {
    static let uidKey = "memorymap.uid"
    static let nameKey = "memorymap.displayName"

    public static var uid: String {
        if let existing = UserDefaults.standard.string(forKey: uidKey) { return existing }
        let fresh = String(UUID().uuidString.prefix(12))
        UserDefaults.standard.set(fresh, forKey: uidKey)
        return fresh
    }

    public static var displayName: String {
        UserDefaults.standard.string(forKey: nameKey) ?? "나"
    }

    public static func rename(_ value: String) {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        UserDefaults.standard.set(trimmed.isEmpty ? "나" : trimmed, forKey: nameKey)
    }
}
