import CoreCommon
import CoreModel
import CoreNetwork
import Domain
import Foundation

struct SpaceDocument: Codable, Sendable {
    var name: String
    var members: [MemberDocument]
}

struct MemberDocument: Codable, Sendable {
    var uid: String
    var displayName: String
    var owner: Bool
}

/// 내가 속한 공간 목록은 **기기에**, 공간의 이름·멤버는 **저장소에** 둡니다.
/// 로그인이 없어 "누가 어느 공간의 멤버인가" 를 서버가 판단할 수 없기 때문입니다.
/// 안드로이드 `SharedSpaceRepository` 와 같은 구조·같은 파일 형식입니다.
public actor SharedSpaceRepository: SpaceRepository {

    private let storage: FirebaseStorage
    private let defaults: UserDefaults
    private var cached: [Space] = []

    private static let idsKey = "memorymap.spaceIds"
    private static let uidKey = "memorymap.uid"
    private static let nameKey = "memorymap.displayName"

    public init(storage: FirebaseStorage, defaults: UserDefaults = .standard) {
        self.storage = storage
        self.defaults = defaults
    }

    public func spaces() async -> [Space] { cached }

    public func refresh() async -> Outcome<Void> {
        var loaded: [Space] = []
        for id in spaceIds() {
            if let document = await read(SpaceId(id)) {
                loaded.append(space(from: document, id: SpaceId(id)))
            }
        }
        cached = loaded
        return .ok(())
    }

    public func create(name: String) async -> Outcome<(Space, Invite)> {
        let id = SpaceId(InviteCode.generate())
        let document = SpaceDocument(
            name: name,
            members: [MemberDocument(uid: uid(), displayName: displayName(), owner: true)]
        )

        if case .fail(let reason) = await write(id, document) { return .fail(reason) }

        remember(id)
        let created = space(from: document, id: id)
        cached.append(created)
        // 이름을 정하는 순간 초대 코드가 함께 나옵니다. 코드가 곧 공간 ID 입니다.
        return .ok((created, Invite(code: id.value, spaceId: id)))
    }

    public func join(code: String) async -> Outcome<Space> {
        guard let normalized = InviteCode.normalize(code) else { return .fail(.notFound) }
        let id = SpaceId(normalized)
        guard var document = await read(id) else { return .fail(.notFound) }

        if spaceIds().contains(id.value) {
            return .ok(space(from: document, id: id))   // 이미 들어가 있는 공간
        }

        document.members.removeAll { $0.uid == uid() }
        document.members.append(MemberDocument(uid: uid(), displayName: displayName(), owner: false))

        if case .fail(let reason) = await write(id, document) { return .fail(reason) }

        remember(id)
        let joined = space(from: document, id: id)
        cached.append(joined)
        return .ok(joined)
    }

    private func read(_ id: SpaceId) async -> SpaceDocument? {
        guard PathSafe.isSafe(id.value),
              case .ok(let data) = await storage.download(path: path(id)),
              let document = try? JSONDecoder().decode(SpaceDocument.self, from: data)
        else { return nil }
        return document
    }

    private func write(_ id: SpaceId, _ document: SpaceDocument) async -> Outcome<Void> {
        guard let data = try? JSONEncoder().encode(document) else { return .fail(.unknown) }
        return await storage.upload(path: path(id), data: data, contentType: "application/json")
    }

    private func path(_ id: SpaceId) -> String { "spaces/\(id.value)/space.json" }

    private func space(from document: SpaceDocument, id: SpaceId) -> Space {
        Space(
            spaceId: id, name: document.name,
            members: document.members.map {
                Member(uid: $0.uid, displayName: $0.displayName, role: $0.owner ? .owner : .member)
            }
        )
    }

    private func spaceIds() -> [String] { defaults.stringArray(forKey: Self.idsKey) ?? [] }

    private func remember(_ id: SpaceId) {
        defaults.set(spaceIds() + [id.value], forKey: Self.idsKey)
    }

    /// 로그인이 없으니 기기에서 한 번 만들어 두고 계속 씁니다.
    private func uid() -> String {
        if let existing = defaults.string(forKey: Self.uidKey) { return existing }
        let fresh = String(UUID().uuidString.prefix(12))
        defaults.set(fresh, forKey: Self.uidKey)
        return fresh
    }

    private func displayName() -> String { defaults.string(forKey: Self.nameKey) ?? "나" }
}
