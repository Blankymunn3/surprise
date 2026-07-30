import CoreCommon
import Foundation

/// Firebase Storage 를 **REST 로** 씁니다. 웹(`assets/firebase.js`)·안드로이드와 같은 방식입니다.
///
/// ⚠️ 목록 조회에 **`delimiter=/` 를 반드시 붙여야** 합니다. 없으면 게시된 규칙에서 403 이 납니다.
public actor FirebaseStorage {

    public struct Item: Sendable {
        public let fullPath: String
        public let name: String
    }

    private let bucket: String
    private let session: URLSession

    public init(bucket: String, session: URLSession = .shared) {
        self.bucket = bucket
        self.session = session
    }

    public func list(prefix: String) async -> Outcome<[Item]> {
        var components = URLComponents(string: base)!
        components.queryItems = [
            .init(name: "prefix", value: prefix),
            .init(name: "delimiter", value: "/"),
            .init(name: "maxResults", value: "1000"),
        ]
        guard let url = components.url else { return .fail(.unknown) }

        switch await send(URLRequest(url: url), timeout: Limits.listTimeout) {
        case .fail(let reason):
            return .fail(reason)
        case .ok(let data):
            guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let items = root["items"] as? [[String: Any]] else { return .ok([]) }
            return .ok(items.compactMap { item in
                guard let path = item["name"] as? String else { return nil }
                return Item(fullPath: path, name: String(path.split(separator: "/").last ?? ""))
            })
        }
    }

    public func upload(path: String, data: Data, contentType: String) async -> Outcome<Void> {
        guard data.count <= Limits.maxUploadBytes else { return .fail(.tooLarge) }

        var components = URLComponents(string: base)!
        components.queryItems = [
            .init(name: "uploadType", value: "media"),
            .init(name: "name", value: path),
        ]
        guard let url = components.url else { return .fail(.unknown) }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = data

        switch await send(request, timeout: Limits.uploadTimeout) {
        case .ok: return .ok(())
        case .fail(let reason): return .fail(reason)
        }
    }

    public func delete(path: String) async -> Outcome<Void> {
        guard let url = URL(string: "\(base)/\(escape(path))") else { return .fail(.unknown) }
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        switch await send(request, timeout: Limits.listTimeout) {
        case .ok: return .ok(())
        case .fail(let reason): return .fail(reason)
        }
    }

    public func download(path: String) async -> Outcome<Data> {
        guard let url = URL(string: downloadURL(path)) else { return .fail(.unknown) }
        return await send(URLRequest(url: url), timeout: Limits.listTimeout)
    }

    public nonisolated func downloadURL(_ path: String) -> String {
        "https://firebasestorage.googleapis.com/v0/b/\(bucket)/o/\(escape(path))?alt=media"
    }

    private var base: String { "https://firebasestorage.googleapis.com/v0/b/\(bucket)/o" }

    private nonisolated func escape(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? value
    }

    private func send(_ request: URLRequest, timeout: TimeInterval) async -> Outcome<Data> {
        var request = request
        request.timeoutInterval = timeout
        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { return .fail(.unknown) }
            switch http.statusCode {
            case 200..<300: return .ok(data)
            case 401, 403: return .fail(.denied)
            case 404: return .fail(.notFound)
            default: return .fail(.unknown)
            }
        } catch let error as URLError where error.code == .timedOut {
            return .fail(.timeout)
        } catch {
            return .fail(.network)
        }
    }
}
