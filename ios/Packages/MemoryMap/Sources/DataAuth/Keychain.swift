import Foundation
import Security

/// 토큰을 두는 자리. **키체인**입니다 — OS 가 주는 표준 자리라 의존성이 없고,
/// 앱을 지워도 남지 않게 `kSecAttrAccessibleAfterFirstUnlock` 로 둡니다.
///
/// (안드로이드는 앱 전용 폴더에 파일로 둡니다. 그쪽은 키체인 같은 표준 자리가 없고,
///  암호화 저장소를 쓰려면 의존성이 하나 더 붙습니다 — `FirebaseAuthRepository` 주석 참고)
enum Keychain {

    static func save(_ data: Data, key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
        ]
        // 있으면 지우고 다시 넣습니다. update 로 하면 없을 때를 또 갈라야 합니다.
        SecItemDelete(query as CFDictionary)

        var insert = query
        insert[kSecValueData as String] = data
        insert[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        SecItemAdd(insert as CFDictionary, nil)
    }

    static func load(key: String) -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess else { return nil }
        return item as? Data
    }

    static func delete(key: String) {
        SecItemDelete([
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
        ] as CFDictionary)
    }
}
