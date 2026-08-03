import CoreCommon
import CoreModel
import CoreNetwork
import Domain
import Foundation

/// 로그인 상태를 들고 있는 곳. 토큰을 **키체인**에 두고, 낡으면 알아서 새로 받습니다.
/// 안드로이드 `FirebaseAuthRepository` 와 같은 구조입니다.
public actor FirebaseAuthRepository: AuthRepository {

    private let auth: FirebaseAuth
    private let now: @Sendable () -> Int
    private static let key = "memorymap.account"

    private var stored: Stored?
    private var loaded = false

    public init(auth: FirebaseAuth, now: @escaping @Sendable () -> Int = { Int(Date().timeIntervalSince1970) }) {
        self.auth = auth
        self.now = now
    }

    public func account() async -> Account? {
        load()
        return stored?.account
    }

    public func signInWithGoogle(idToken: String) async -> Outcome<Account> {
        switch await auth.signInWithGoogle(idToken: idToken) {
        case .fail(let reason):
            return .fail(reason)
        case .ok(let session):
            let record = Stored(
                uid: session.uid,
                // 구글 계정에 이름이 없으면 이메일 앞부분, 그것도 없으면 '나'.
                // 멤버 목록에 빈칸이 뜨면 누구인지 알 수 없습니다.
                displayName: session.displayName
                    ?? session.email.map { String($0.prefix(while: { $0 != "@" })) }
                    ?? "나",
                email: session.email,
                idToken: session.tokens.idToken,
                refreshToken: session.tokens.refreshToken,
                expiresAt: session.tokens.expiresAt
            )
            save(record)
            return .ok(record.account)
        }
    }

    public func signOut() async {
        Keychain.delete(key: Self.key)
        stored = nil
        loaded = true
    }

    /// 낡았으면 새로 받아서 돌려줍니다. 갱신이 **거부되면 로그아웃**시킵니다 —
    /// 못 쓰는 토큰을 들고 계속 실패하는 것보다 다시 로그인하는 편이 빠릅니다.
    public func idToken() async -> String? {
        load()
        guard let current = stored else { return nil }
        if now() < current.expiresAt - 60 { return current.idToken }

        switch await auth.refresh(refreshToken: current.refreshToken) {
        case .fail:
            await signOut()
            return nil
        case .ok(let tokens):
            var next = current
            next.idToken = tokens.idToken
            next.refreshToken = tokens.refreshToken
            next.expiresAt = tokens.expiresAt
            save(next)
            return next.idToken
        }
    }

    private func load() {
        guard !loaded else { return }
        loaded = true
        guard let data = Keychain.load(key: Self.key),
              let decoded = try? JSONDecoder().decode(Stored.self, from: data)
        else { return }   // 형식이 바뀌었거나 깨졌으면 로그아웃으로 봅니다
        stored = decoded
    }

    private func save(_ value: Stored) {
        stored = value
        loaded = true
        if let data = try? JSONEncoder().encode(value) {
            Keychain.save(data, key: Self.key)
        }
    }

    struct Stored: Codable, Sendable {
        let uid: String
        let displayName: String
        let email: String?
        var idToken: String
        var refreshToken: String
        var expiresAt: Int

        var account: Account { Account(uid: uid, displayName: displayName, email: email) }
    }
}
