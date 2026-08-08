import CoreCommon
import Foundation

/// 구글 로그인으로 받은 ID 토큰을 **Firebase 토큰으로 바꿉니다.** REST 로만 씁니다.
///
/// Firebase SDK 를 통째로 넣지 않는 이유는 `FirebaseStorage` 와 같습니다 —
/// 받을 것이 적고, 두 앱이 같은 방식으로 움직입니다 (`docs/app/AUTH.md`).
///
/// ```
/// 구글 로그인 SDK  →  구글 ID 토큰
///       ↓  signInWithIdp
/// Firebase ID 토큰(1시간) + refresh 토큰(안 만료)
///       ↓  securetoken
/// 새 ID 토큰
/// ```
///
/// `apiKey` 는 비밀이 아닙니다(`GoogleService-Info.plist` 에 들어 있는 그 값). 실제 보안은
/// 규칙이 합니다. 다만 **앱마다 값이 다릅니다** — 조립하는 곳에서 넣어 줍니다.
/// 안드로이드 `FirebaseAuth` 와 같은 구조입니다.
public struct FirebaseAuth: Sendable {

    /// 로그인한 사람. `displayName` 은 구글 계정에 이름이 없으면 비어 옵니다.
    public struct Session: Sendable, Equatable {
        public let uid: String
        public let email: String?
        public let displayName: String?
        public let tokens: Tokens
    }

    /// ID 토큰은 한 시간이면 만료됩니다. `expiresAt` 을 들고 다니는 이유는 요청을 보내기
    /// **전에** 만료를 알아채기 위해서입니다 — 401 을 받고 나서 고치면 사진 올리다
    /// 실패한 것처럼 보입니다.
    public struct Tokens: Sendable, Equatable {
        public let idToken: String
        public let refreshToken: String
        public let expiresAt: Int

        public init(idToken: String, refreshToken: String, expiresAt: Int) {
            self.idToken = idToken
            self.refreshToken = refreshToken
            self.expiresAt = expiresAt
        }

        /// 만료 **1분 전**부터 낡은 것으로 봅니다. 요청이 날아가는 동안 만료되면
        /// 결국 401 이라, 여유를 두고 미리 바꿉니다.
        public func isStale(now: Int) -> Bool { now >= expiresAt - 60 }
    }

    private let apiKey: String
    private let session: URLSession
    private let now: @Sendable () -> Int

    public init(
        apiKey: String,
        session: URLSession = .shared,
        now: @escaping @Sendable () -> Int = { Int(Date().timeIntervalSince1970) }
    ) {
        self.apiKey = apiKey
        self.session = session
        self.now = now
    }

    /// 구글 ID 토큰 → Firebase 세션.
    public func signInWithGoogle(idToken: String) async -> Outcome<Session> {
        await signInWithIdp(postBody: "id_token=\(idToken)&providerId=google.com")
    }

    /// 애플 ID 토큰 → Firebase 세션.
    ///
    /// [nonce] 는 로그인 요청 때 만든 **원문**입니다. 애플에는 SHA-256 을 보내고 토큰에
    /// 그 해시가 박혀 오는데, 여기에 원문을 실어야 서버가 둘을 맞춰 보고 **다른 데서
    /// 가로챈 토큰이 아님**을 확인합니다. 구글 갈래에는 없는 재료라 따로 받습니다.
    public func signInWithApple(idToken: String, nonce: String) async -> Outcome<Session> {
        await signInWithIdp(postBody: "id_token=\(idToken)&providerId=apple.com&nonce=\(nonce)")
    }

    /// `postBody` 가 폼 형식인 것은 이 API 가 원래 OAuth 응답을 그대로 받도록
    /// 만들어져서입니다. `requestUri` 는 웹 리다이렉트용이라 앱에서는 아무 값이나 됩니다.
    private func signInWithIdp(postBody: String) async -> Outcome<Session> {
        let body: [String: Any] = [
            "postBody": postBody,
            "requestUri": "http://localhost",
            "returnSecureToken": true,
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: body),
              let url = URL(string: "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=\(encoded(apiKey))")
        else { return .fail(.unknown) }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = data

        switch await send(request) {
        case .fail(let reason):
            return .fail(reason)
        case .ok(let data):
            guard let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return .fail(.unknown)
            }
            let name = object["displayName"] as? String
            return .ok(Session(
                uid: object["localId"] as? String ?? "",
                email: object["email"] as? String,
                displayName: (name?.isEmpty ?? true) ? nil : name,
                tokens: Tokens(
                    idToken: object["idToken"] as? String ?? "",
                    refreshToken: object["refreshToken"] as? String ?? "",
                    expiresAt: now() + seconds(object["expiresIn"])
                )
            ))
        }
    }

    /// 낡은 ID 토큰을 새로 받습니다. **주소가 다릅니다** — 이쪽은 `securetoken` 이고
    /// 본문도 JSON 이 아니라 폼입니다.
    ///
    /// 응답의 `refresh_token` 은 보통 같은 값이지만 **바뀔 수도 있어서** 그대로 받아 씁니다.
    public func refresh(refreshToken: String) async -> Outcome<Tokens> {
        guard let url = URL(string: "https://securetoken.googleapis.com/v1/token?key=\(encoded(apiKey))")
        else { return .fail(.unknown) }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = "grant_type=refresh_token&refresh_token=\(encoded(refreshToken))"
            .data(using: .utf8)

        switch await send(request) {
        case .fail(let reason):
            return .fail(reason)
        case .ok(let data):
            guard let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return .fail(.unknown)
            }
            return .ok(Tokens(
                idToken: object["id_token"] as? String ?? "",
                refreshToken: object["refresh_token"] as? String ?? refreshToken,
                expiresAt: now() + seconds(object["expires_in"])
            ))
        }
    }

    /// `expiresIn` 은 문자열로 옵니다. 숫자로 오는 경우도 있어 둘 다 받습니다.
    private func seconds(_ value: Any?) -> Int {
        if let text = value as? String, let parsed = Int(text) { return parsed }
        if let number = value as? Int { return number }
        return 3600
    }

    private func encoded(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? value
    }

    /// `[String: Any]` 를 그대로 돌려주지 않는 이유: Swift 6 에서 `Any` 는 `Sendable` 이
    /// 아니라 `Outcome` 에 담기지 않습니다. 바이트로 넘기고 파싱은 부르는 쪽에서 합니다.
    private func send(_ request: URLRequest) async -> Outcome<Data> {
        do {
            let (data, response) = try await session.data(for: request)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            switch code {
            case 200..<300:
                return .ok(data)
            // 400 은 토큰이 틀렸거나 만료된 것 — 다시 로그인해야 합니다.
            case 400, 401, 403:
                return .fail(.denied)
            default:
                return .fail(.unknown)
            }
        } catch let error as URLError where error.code == .timedOut {
            return .fail(.timeout)
        } catch {
            return .fail(.network)
        }
    }
}
