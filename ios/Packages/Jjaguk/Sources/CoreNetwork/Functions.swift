import CoreCommon
import Foundation

/// Cloud Functions callable 호출. SDK 없이 규약대로 보냅니다 —
/// `POST {origin}/{이름}` 에 `{"data": ...}`, 응답은 `{"result": ...}`.
///
/// 지금 쓰는 함수는 `joinSpace` 하나입니다. 참여 검증을 클라이언트에 맡기면
/// 코드 없이도 들어와져서, 그 한 걸음만 서버가 합니다 (`functions/index.js`).
///
/// 값은 `Firestore` 처럼 **문자열만** 받고 돌려줍니다 — 우리가 주고받는 것이
/// 코드·ID 뿐이라 타입 지도를 만들 이유가 없습니다. 안드로이드 `Functions` 와 같은 구조.
public struct Functions: Sendable {

    /// `https://asia-northeast3-{프로젝트}.cloudfunctions.net`
    private let origin: String
    private let session: URLSession
    /// 함수가 `request.auth` 로 받는 ID 토큰. 없으면 UNAUTHENTICATED 로 거절됩니다.
    private let token: @Sendable () async -> String?
    private let appCheck: @Sendable () async -> String?

    public init(
        origin: String,
        session: URLSession = .shared,
        token: @escaping @Sendable () async -> String? = { nil },
        appCheck: @escaping @Sendable () async -> String? = { nil }
    ) {
        self.origin = origin
        self.session = session
        self.token = token
        self.appCheck = appCheck
    }

    public func call(_ name: String, data: [String: String]) async -> Outcome<[String: String]> {
        guard let url = URL(string: "\(origin)/\(name)"),
              let body = try? JSONSerialization.data(withJSONObject: ["data": data])
        else { return .fail(.unknown) }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = Limits.listTimeout
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = body
        if let token = await token() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let proof = await appCheck() {
            request.setValue(proof, forHTTPHeaderField: "X-Firebase-AppCheck")
        }

        do {
            let (payload, response) = try await session.data(for: request)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            switch code {
            case 200..<300:
                let raw = try? JSONSerialization.jsonObject(with: payload) as? [String: Any]
                let result = raw?["result"] as? [String: Any] ?? [:]
                return .ok(result.compactMapValues { $0 as? String })
            // callable 의 NOT_FOUND(틀린 코드)가 404 로 옵니다.
            case 404: return .fail(.notFound)
            case 401, 403: return .fail(.denied)
            default: return .fail(.unknown)
            }
        } catch let error as URLError where error.code == .timedOut {
            return .fail(.timeout)
        } catch {
            return .fail(.network)
        }
    }
}
