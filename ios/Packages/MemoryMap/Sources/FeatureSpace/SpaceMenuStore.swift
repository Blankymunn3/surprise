import CoreCommon
import CoreModel
import Domain
import Foundation
import Observation

/// ⋯ 메뉴가 보여 주는 것 — 멤버 · 초대 코드 · 이름.
public struct SpaceMenuState: Equatable, Sendable {
    public let spaceId: SpaceId
    public var space: Space?
    /// 초대 코드. 같이 쓰는 짜국에서만, 메뉴를 처음 열 때 한 번 만듭니다.
    public var code: String?
    public var renaming = false
    public var pendingName = ""
    public var working = false

    public init(spaceId: SpaceId) { self.spaceId = spaceId }
}

/**
 ⋯ 메뉴.

 초대 코드를 **메뉴를 열 때 만듭니다.** 저장소가 코드→짜국만 들고 있어서 이미 쓰던
 코드를 되찾을 길이 없기 때문입니다 (`invites/{code}` 한 방향). 코드는 여러 개여도
 모두 같은 짜국을 가리키므로 문제는 없고, 한 번 만들면 이 화면이 사는 동안 그대로 씁니다.
 */
@MainActor
@Observable
public final class SpaceMenuStore {
    public private(set) var state: SpaceMenuState

    private let observeSpaces: ObserveSpaces
    private let newInvite: NewInvite
    private let renameSpace: RenameSpace

    public init(
        spaceId: SpaceId,
        observeSpaces: ObserveSpaces,
        newInvite: NewInvite,
        renameSpace: RenameSpace
    ) {
        self.state = SpaceMenuState(spaceId: spaceId)
        self.observeSpaces = observeSpaces
        self.newInvite = newInvite
        self.renameSpace = renameSpace
    }

    public func appeared() async {
        state.space = await observeSpaces().first { $0.spaceId == state.spaceId }
        await ensureCode()
    }

    /// 혼자 쓰는 짜국에는 초대할 사람이 없습니다.
    private func ensureCode() async {
        guard state.code == nil, state.space?.kind == .shared else { return }
        // 실패하면 코드 칸만 비워 둡니다 — 메뉴의 나머지는 쓸 수 있습니다.
        if case .ok(let invite) = await newInvite(state.spaceId) { state.code = invite.code }
    }

    public func startRenaming() {
        state.renaming = true
        state.pendingName = state.space?.name ?? ""
    }

    public func typeName(_ value: String) { state.pendingName = value }

    /// 이름을 바꾸고 나면 목록에서 다시 읽어 화면의 이름도 따라갑니다.
    public func confirmRename() async -> Bool {
        let name = state.pendingName.trimmingCharacters(in: .whitespaces)
        guard !name.isEmpty, !state.working else { return false }

        state.working = true
        defer { state.working = false }

        switch await renameSpace(state.spaceId, name) {
        case .ok:
            state.renaming = false
            state.space = await observeSpaces().first { $0.spaceId == state.spaceId }
            return true
        case .fail:
            return false
        }
    }
}
