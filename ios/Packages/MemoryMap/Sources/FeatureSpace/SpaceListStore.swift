import CoreCommon
import CoreModel
import Domain
import Foundation
import Observation

public enum SpacesUi: Equatable, Sendable {
    case loading
    case ready([Space])
    case failed(Failure)
}

public enum SpaceListSheet: Equatable, Sendable {
    case none
    case create
    case join
    /// 만들자마자 뜨는 초대 코드 — 다시 찾게 하지 않으려고
    case invited(spaceName: String, code: String)
}

public struct SpaceListState: Equatable, Sendable {
    public var spaces: SpacesUi = .loading
    public var sheet: SpaceListSheet = .none
    public var pendingName: String = ""
    public var pendingCode: String = ""
    public var working: Bool = false

    public init() {}

    /// 화면의 버튼이 꺼지는 조건과 같아야 합니다.
    public var canCreate: Bool { !pendingName.trimmingCharacters(in: .whitespaces).isEmpty && !working }
    public var canJoin: Bool { pendingCode.trimmingCharacters(in: .whitespaces).count >= 6 && !working }

    var items: [Space] {
        if case .ready(let list) = spaces { return list }
        return []
    }
}

public enum SpaceListIntent: Sendable {
    case appeared
    case createTapped
    case joinTapped
    case sheetDismissed
    case nameTyped(String)
    case codeTyped(String)
    case createConfirmed
    case joinConfirmed
}

/// 상태 계산은 **순수 함수**로 떼어 둡니다. 여기부터 테스트합니다 (`docs/app/MVI.md`).
public enum SpaceListReducer {

    public static func loaded(_ state: SpaceListState, _ items: [Space]) -> SpaceListState {
        var next = state
        next.spaces = .ready(items)
        next.working = false
        return next
    }

    public static func loadFailed(_ state: SpaceListState, _ reason: Failure) -> SpaceListState {
        var next = state
        // 이미 보여 주던 목록은 지우지 않습니다. 새로고침이 실패했다고 눈앞의 목록이
        // 사라지면 더 나쁩니다.
        if case .ready = state.spaces {} else { next.spaces = .failed(reason) }
        next.working = false
        return next
    }

    public static func sheetOpened(_ state: SpaceListState, _ sheet: SpaceListSheet) -> SpaceListState {
        var next = state
        next.sheet = sheet
        next.pendingName = ""
        next.pendingCode = ""
        return next
    }

    public static func sheetDismissed(_ state: SpaceListState) -> SpaceListState {
        var next = state
        next.sheet = .none
        next.pendingName = ""
        next.pendingCode = ""
        next.working = false
        return next
    }

    /// 저장소도 목록에 넣고 여기서도 넣기 때문에 **같은 공간이 두 번 들어갈 수 있습니다.**
    /// 목록은 `id` 를 키로 그리므로 그러면 화면이 죽습니다. `joined` 와 같은 방식으로
    /// 같은 id 를 먼저 걷어냅니다.
    public static func created(_ state: SpaceListState, _ space: Space, _ code: String?) -> SpaceListState {
        var next = state
        next.spaces = .ready(state.items.filter { $0.spaceId != space.spaceId } + [space])
        // 혼자 쓰는 짜국은 초대 코드가 없습니다. 보여 줄 것이 없으니 시트를 닫습니다.
        next.sheet = code.map { SpaceListSheet.invited(spaceName: space.name, code: $0) } ?? SpaceListSheet.none
        next.pendingName = ""
        next.working = false
        return next
    }

    public static func joined(_ state: SpaceListState, _ space: Space) -> SpaceListState {
        var next = state
        next.spaces = .ready(state.items.filter { $0.spaceId != space.spaceId } + [space])
        next.sheet = .none
        next.pendingCode = ""
        next.working = false
        return next
    }

    public static func working(_ state: SpaceListState) -> SpaceListState {
        var next = state
        next.working = true
        return next
    }
}

/// `View` 는 `store.state` 를 읽고 `store.send(...)` 만 호출합니다.
@MainActor
@Observable
public final class SpaceListStore {
    public private(set) var state = SpaceListState()

    private let observeSpaces: ObserveSpaces
    private let refreshSpaces: RefreshSpaces
    private let createSpace: CreateSpace
    private let joinSpace: JoinSpace

    public init(
        observeSpaces: ObserveSpaces,
        refreshSpaces: RefreshSpaces,
        createSpace: CreateSpace,
        joinSpace: JoinSpace
    ) {
        self.observeSpaces = observeSpaces
        self.refreshSpaces = refreshSpaces
        self.createSpace = createSpace
        self.joinSpace = joinSpace
    }

    public func send(_ intent: SpaceListIntent) async {
        switch intent {
        case .appeared:
            // 먼저 받아오고 그 다음에 읽습니다. 안드로이드 `SpaceListViewModel` 과 같은 순서입니다.
            if case .fail(let reason) = await refreshSpaces() {
                state = SpaceListReducer.loadFailed(state, reason)
                return
            }
            state = SpaceListReducer.loaded(state, await observeSpaces())

        case .createTapped:
            state = SpaceListReducer.sheetOpened(state, .create)

        case .joinTapped:
            state = SpaceListReducer.sheetOpened(state, .join)

        case .sheetDismissed:
            state = SpaceListReducer.sheetDismissed(state)

        case .nameTyped(let value):
            state.pendingName = value

        case .codeTyped(let value):
            state.pendingCode = value

        case .createConfirmed:
            guard state.canCreate else { return }
            state = SpaceListReducer.working(state)
            // 혼자/둘이를 고르는 화면은 아직 없습니다(`docs/app/AUTH.md` 0번의 5).
            // 그때까지는 지금까지처럼 둘이 쓰는 짜국을 만듭니다 — 여기 한 줄만 바꾸면 됩니다.
            switch await createSpace(state.pendingName, .shared) {
            case .ok(let (space, invite)):
                state = SpaceListReducer.created(state, space, invite?.code)
            case .fail(let reason):
                state = SpaceListReducer.loadFailed(state, reason)
            }

        case .joinConfirmed:
            guard state.canJoin else { return }
            state = SpaceListReducer.working(state)
            switch await joinSpace(state.pendingCode) {
            case .ok(let space):
                state = SpaceListReducer.joined(state, space)
            case .fail(let reason):
                state = SpaceListReducer.loadFailed(state, reason)
            }
        }
    }
}
