import CoreCommon
import CoreModel
import CoreNetwork
import Domain
import Foundation

/// 짜국 목록.
///
/// | 종류 | 어디에 |
/// |---|---|
/// | 혼자 | 기기 안 (UserDefaults) — 서버를 아예 안 씁니다 |
/// | 같이 | **Firestore** |
///
/// Firestore 로 옮긴 이유는 규칙 때문입니다. Storage 규칙은 다른 파일의 내용을 못 읽어서
/// "이 사람이 멤버인가" 를 판단할 수 없습니다. 멤버 문서가 Firestore 에 있어야
/// `firestore.exists(...)` 로 물어볼 수 있습니다 (`docs/app/AUTH.md`).
///
/// ```
/// spaces/{짜국ID}                 이름 · 주인
/// spaces/{짜국ID}/members/{uid}   누가 멤버인가  ← 규칙이 보는 곳
/// invites/{코드}                  코드 → 짜국ID
/// users/{uid}/spaces/{짜국ID}     내가 어느 짜국에 속하나 (기기를 바꿔도 따라옵니다)
/// ```
///
/// **초대 코드가 더 이상 짜국 ID 가 아닙니다.** 코드를 알아도 경로를 모르고, 경로를 알아도
/// 멤버가 아니면 못 읽습니다. 안드로이드 `SharedSpaceRepository` 와 같은 구조입니다.
public actor SharedSpaceRepository: SpaceRepository {

    private let firestore: Firestore
    private let accounts: any AuthRepository
    private let now: @Sendable () -> Int
    /// 짜국 ID 는 **초대 코드보다 깁니다** — 찍어서 맞힐 수 없어야 합니다.
    private let newSpaceId: @Sendable () -> String

    private var cached: [Space] = []

    /// UserDefaults 를 밖에서 받지 않고 여기서 씁니다 — actor 로 넘기면
    /// Swift 6 에서 Sendable 검사에 걸립니다.
    private var defaults: UserDefaults { .standard }

    /// 혼자 쓰는 짜국. 서버에 문서가 없어 **이름도 여기에** 둡니다.
    private static let personalKey = "memorymap.personalSpaces"

    public init(
        firestore: Firestore,
        accounts: any AuthRepository,
        now: @escaping @Sendable () -> Int = { Int(Date().timeIntervalSince1970) },
        newSpaceId: @escaping @Sendable () -> String = {
            UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased()
        }
    ) {
        self.firestore = firestore
        self.accounts = accounts
        self.now = now
        self.newSpaceId = newSpaceId
    }

    public func spaces() async -> [Space] { cached }

    /// 혼자 짜국이 먼저입니다 — 네트워크를 기다리지 않고 바로 보여 줄 수 있습니다.
    public func refresh() async -> Outcome<Void> {
        let mine = personalSpaces().map { personalSpace(id: SpaceId($0.id), name: $0.name) }

        guard let account = await accounts.account() else {
            // 로그인 전에는 같이 쓰는 짜국을 볼 길이 없습니다. 혼자 것만 보여 줍니다.
            cached = mine
            return .ok(())
        }

        let ids: [String]
        switch await firestore.list("users/\(account.uid)/spaces") {
        case .fail(let reason):
            cached = mine
            return .fail(reason)
        case .ok(let documents):
            ids = documents.map(\.id)
        }

        var loaded = mine
        for id in ids {
            if let space = await fetchSpace(SpaceId(id)) { loaded.append(space) }
        }
        cached = loaded
        return .ok(())
    }

    public func create(name: String, kind: SpaceKind) async -> Outcome<(Space, Invite?)> {
        // 혼자 쓰는 짜국은 여기서 끝입니다. 서버에 아무것도 안 만듭니다.
        // 초대 코드도 없습니다 — 초대할 사람이 없으니까요.
        if kind == .personal {
            let id = SpaceId(newSpaceId())
            rememberPersonal(id: id, name: name)
            let created = personalSpace(id: id, name: name)
            cached.append(created)
            return .ok((created, nil))
        }

        guard let account = await accounts.account() else { return .fail(.denied) }
        let id = SpaceId(newSpaceId())

        // 순서가 중요합니다: **멤버 문서를 먼저** 만들어야 그다음 쓰기가 규칙을 통과합니다.
        if case .fail(let reason) = await firestore.set("spaces/\(id.value)", fields: [
            "name": .text(name),
            "ownerUid": .text(account.uid),
            "createdAt": .number(now()),
        ]) { return .fail(reason) }

        if case .fail(let reason) = await firestore.set(
            "spaces/\(id.value)/members/\(account.uid)",
            fields: memberFields(account, owner: true)
        ) { return .fail(reason) }

        // 코드는 **문 앞까지만** 데려다줍니다. 이 문서를 봐도 멤버가 되기 전에는 못 읽습니다.
        let code = InviteCode.generate()
        if case .fail(let reason) = await firestore.set(
            "invites/\(code)", fields: ["spaceId": .text(id.value)]
        ) { return .fail(reason) }

        await rememberMembership(uid: account.uid, id: id)

        let made = Space(
            spaceId: id, name: name,
            members: [Member(uid: account.uid, displayName: account.displayName, role: .owner)],
            kind: .shared
        )
        cached.append(made)
        return .ok((made, Invite(code: code, spaceId: id)))
    }

    public func join(code: String) async -> Outcome<Space> {
        guard let normalized = InviteCode.normalize(code) else { return .fail(.notFound) }
        guard let account = await accounts.account() else { return .fail(.denied) }

        let invite: Firestore.Document
        switch await firestore.get("invites/\(normalized)") {
        case .fail(let reason): return .fail(reason)
        case .ok(let document):
            guard let document else { return .fail(.notFound) }
            invite = document
        }
        guard let rawId = invite.text("spaceId") else { return .fail(.notFound) }
        let id = SpaceId(rawId)

        // 나를 멤버로 넣습니다. 규칙이 **자기 자신만** 넣게 해 둬서 남을 끌어들일 수 없습니다.
        if case .fail(let reason) = await firestore.set(
            "spaces/\(id.value)/members/\(account.uid)",
            fields: memberFields(account, owner: false)
        ) { return .fail(reason) }

        await rememberMembership(uid: account.uid, id: id)

        guard let space = await fetchSpace(id) else { return .fail(.notFound) }
        cached.removeAll { $0.spaceId == id }
        cached.append(space)
        return .ok(space)
    }

    private func rememberMembership(uid: String, id: SpaceId) async {
        _ = await firestore.set(
            "users/\(uid)/spaces/\(id.value)", fields: ["joinedAt": .number(now())]
        )
    }

    private func memberFields(_ account: Account, owner: Bool) -> [String: Firestore.Value] {
        ["displayName": .text(account.displayName), "owner": .flag(owner)]
    }

    private func fetchSpace(_ id: SpaceId) async -> Space? {
        guard PathSafe.isSafe(id.value) else { return nil }

        let document: Firestore.Document
        switch await firestore.get("spaces/\(id.value)") {
        case .fail: return nil
        case .ok(let found):
            guard let found else { return nil }
            document = found
        }

        var members: [Member] = []
        if case .ok(let documents) = await firestore.list("spaces/\(id.value)/members") {
            members = documents.map {
                Member(
                    uid: $0.id,
                    displayName: $0.text("displayName").flatMap { $0.isEmpty ? nil : $0 } ?? "?",
                    role: $0.flag("owner") == true ? .owner : .member
                )
            }
        }

        return Space(
            spaceId: id, name: document.text("name") ?? "", members: members, kind: .shared
        )
    }

    /// 혼자 짜국의 멤버는 늘 나 하나입니다. 서버에 문서가 없어 여기서 만들어 줍니다.
    private func personalSpace(id: SpaceId, name: String) -> Space {
        Space(
            spaceId: id, name: name,
            members: [Member(uid: DeviceIdentity.uid, displayName: DeviceIdentity.displayName, role: .owner)],
            kind: .personal
        )
    }

    private func personalSpaces() -> [(id: String, name: String)] {
        let raw = defaults.array(forKey: Self.personalKey) as? [[String: String]] ?? []
        return raw.compactMap { entry in
            guard let id = entry["id"], let name = entry["name"] else { return nil }
            return (id, name)
        }
    }

    private func rememberPersonal(id: SpaceId, name: String) {
        var raw = defaults.array(forKey: Self.personalKey) as? [[String: String]] ?? []
        raw.append(["id": id.value, "name": name])
        defaults.set(raw, forKey: Self.personalKey)
    }
}
