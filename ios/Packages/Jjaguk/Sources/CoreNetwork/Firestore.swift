import CoreCommon
import Foundation

/// Firestore 를 **REST 로** 씁니다. SDK 를 안 넣는 이유는 `FirebaseStorage` 와 같습니다 —
/// 받을 것이 적고, 두 앱이 같은 방식으로 움직입니다 (`docs/app/AUTH.md`).
///
/// **여기가 "이 사람이 이 짜국의 멤버인가" 를 답하는 자리입니다.** Storage 규칙이
/// `firestore.exists(...)` 로 이 문서들을 건너다봅니다 (`firestore.rules`).
///
/// Firestore 의 값에는 **타입 이름이 붙어 옵니다** (`{"stringValue":"우리 지도"}`).
/// 그 모양을 화면까지 들고 가지 않으려고 `Value` 로 감싸 둡니다.
/// 안드로이드 `Firestore` 와 같은 구조입니다.
public struct Firestore: Sendable {

    /// 우리가 쓰는 값은 이 셋뿐입니다. 필요해지면 그때 늘립니다.
    public enum Value: Sendable, Equatable {
        case text(String)
        case number(Int)
        case flag(Bool)
    }

    /// 문서 하나. `id` 는 경로의 마지막 조각입니다 — Firestore 가 돌려주는 `name` 은
    /// `projects/../documents/spaces/ABC` 처럼 전체 경로라 그대로 쓰면 길기만 합니다.
    public struct Document: Sendable, Equatable {
        public let id: String
        public let fields: [String: Value]

        public func text(_ key: String) -> String? {
            if case .text(let value) = fields[key] { return value }
            return nil
        }

        public func number(_ key: String) -> Int? {
            if case .number(let value) = fields[key] { return value }
            return nil
        }

        public func flag(_ key: String) -> Bool? {
            if case .flag(let value) = fields[key] { return value }
            return nil
        }
    }

    private let projectId: String
    private let session: URLSession
    private let token: @Sendable () async -> String?
    /// App Check 토큰. "진짜 우리 앱에서 온 요청인가" 를 서버가 가릴 수 있게 얹습니다.
    /// `nil` 이면 없이 보냅니다 — 못 받았다고 데이터 길이 막히면 안 됩니다
    /// (콘솔에서 강제를 켜기 전까지는 지표만 쌓입니다).
    private let appCheck: @Sendable () async -> String?

    public init(
        projectId: String,
        session: URLSession = .shared,
        token: @escaping @Sendable () async -> String? = { nil },
        appCheck: @escaping @Sendable () async -> String? = { nil }
    ) {
        self.projectId = projectId
        self.session = session
        self.token = token
        self.appCheck = appCheck
    }

    /// 없는 문서는 실패가 아니라 `nil` 입니다 — 처음 들어가는 짜국이 그렇습니다.
    public func get(_ path: String) async -> Outcome<Document?> {
        guard let url = URL(string: base + encoded(path)) else { return .fail(.unknown) }
        switch await send(URLRequest(url: url)) {
        case .fail(let reason):
            return reason == .notFound ? .ok(nil) : .fail(reason)
        case .ok(let data):
            guard let raw = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return .fail(.unknown)
            }
            return .ok(Self.document(from: raw))
        }
    }

    /// 문서를 통째로 씁니다. 없으면 만들고 있으면 덮습니다.
    ///
    /// `updateMask` 를 안 붙이는 이유: 우리 문서는 필드가 몇 개뿐이라 통째로 쓰는 편이
    /// 단순하고, 일부만 고치다 옛 필드가 남는 일도 없습니다.
    public func set(_ path: String, fields: [String: Value]) async -> Outcome<Void> {
        guard let url = URL(string: base + encoded(path)),
              let body = try? JSONSerialization.data(
                  withJSONObject: ["fields": fields.mapValues(Self.encode)]
              )
        else { return .fail(.unknown) }

        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = body

        switch await send(request) {
        case .fail(let reason): return .fail(reason)
        case .ok: return .ok(())
        }
    }

    public func delete(_ path: String) async -> Outcome<Void> {
        guard let url = URL(string: base + encoded(path)) else { return .fail(.unknown) }
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"

        switch await send(request) {
        case .fail(let reason): return .fail(reason)
        case .ok: return .ok(())
        }
    }

    /// 컬렉션 안의 문서들. 비어 있으면 빈 목록입니다.
    public func list(_ collection: String) async -> Outcome<[Document]> {
        guard let url = URL(string: base + encoded(collection) + "?pageSize=300") else {
            return .fail(.unknown)
        }
        switch await send(URLRequest(url: url)) {
        case .fail(let reason):
            return reason == .notFound ? .ok([]) : .fail(reason)
        case .ok(let data):
            guard let raw = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return .fail(.unknown)
            }
            let documents = raw["documents"] as? [[String: Any]] ?? []
            return .ok(documents.map(Self.document(from:)))
        }
    }

    private var base: String {
        "https://firestore.googleapis.com/v1/projects/\(projectId)/databases/(default)/documents/"
    }

    private func encoded(_ path: String) -> String {
        path.split(separator: "/")
            .map { $0.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? String($0) }
            .joined(separator: "/")
    }

    static func encode(_ value: Value) -> [String: Any] {
        switch value {
        case .text(let text): return ["stringValue": text]
        // integerValue 는 **문자열로** 보냅니다. Firestore 가 그렇게 받습니다.
        case .number(let number): return ["integerValue": String(number)]
        case .flag(let flag): return ["booleanValue": flag]
        }
    }

    /// 응답을 `Document` 로. 테스트에서도 쓰려고 밖에 둡니다.
    static func document(from raw: [String: Any]) -> Document {
        let name = raw["name"] as? String ?? ""
        let rawFields = raw["fields"] as? [String: [String: Any]] ?? [:]

        var fields: [String: Value] = [:]
        for (key, holder) in rawFields {
            if let text = holder["stringValue"] as? String {
                fields[key] = .text(text)
            } else if let number = holder["integerValue"] as? String, let parsed = Int(number) {
                fields[key] = .number(parsed)
            } else if let flag = holder["booleanValue"] as? Bool {
                fields[key] = .flag(flag)
            }
            // 우리가 안 쓰는 타입(지도·배열 등)은 조용히 건너뜁니다.
        }

        return Document(id: String(name.split(separator: "/").last ?? ""), fields: fields)
    }

    private func send(_ request: URLRequest) async -> Outcome<Data> {
        var request = request
        request.timeoutInterval = Limits.listTimeout
        if let token = await token() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let proof = await appCheck() {
            request.setValue(proof, forHTTPHeaderField: "X-Firebase-AppCheck")
        }
        do {
            let (data, response) = try await session.data(for: request)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            switch code {
            case 200..<300: return .ok(data)
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
