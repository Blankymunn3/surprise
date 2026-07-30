import CoreCommon
import CoreModel
import Domain
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

    public static func created(_ state: SpaceListState, _ space: Space, _ code: String) -> SpaceListState {
        var next = state
        next.spaces = .ready(state.items + [space])
        next.sheet = .invited(spaceName: space.name, code: code)
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
    private let createSpace: CreateSpace
    private let joinSpace: JoinSpace

    public init(observeSpaces: ObserveSpaces, createSpace: CreateSpace, joinSpace: JoinSpace) {
        self.observeSpaces = observeSpaces
        self.createSpace = createSpace
        self.joinSpace = joinSpace
    }

    public func send(_ intent: SpaceListIntent) async {
        switch intent {
        case .appeared:
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
            switch await createSpace(state.pendingName) {
            case .ok(let (space, invite)):
                state = SpaceListReducer.created(state, space, invite.code)
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
