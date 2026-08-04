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
    /// 만들자마자 뜨는 초대 코드 — 다시 찾게 하지 않으려고.
    /// 짜국을 통째로 드는 이유: 이 화면의 '짜국 열기' 가 바로 들어가야 해서입니다.
    case invited(space: Space, code: String)

    /// 로그인이 필요해 **잠깐 끼어든** 화면. 앱을 켤 때가 아니라 여기서만 뜹니다
    /// (`docs/app/design.html` 의 '로그인').
    ///
    /// `next` 를 들고 있는 이유: 로그인이 끝나면 **하던 일을 이어서** 해야 합니다.
    /// 로그인만 하고 멈추면 사용자가 방금 뭘 하려던 건지 다시 찾아야 합니다.
    case signIn(next: SignInNext)
}

/// 로그인 뒤에 이어서 할 일
public enum SignInNext: Equatable, Sendable { case create, join }

public struct SpaceListState: Equatable, Sendable {
    public var spaces: SpacesUi = .loading
    public var sheet: SpaceListSheet = .none
    public var pendingName: String = ""
    public var pendingCode: String = ""
    /// 만들기 시트에서 고른 종류. 기본이 **혼자**인 이유는 잘못 골라도 사진이 폰 밖으로
    /// 나가지 않기 때문입니다. 반대로 두면 무심코 넘긴 사람의 사진이 서버로 갑니다.
    public var pendingKind: SpaceKind = .personal
    /// 로그인했는지. 같이 쓰는 짜국을 만들거나 참여할 때만 봅니다.
    public var signedIn: Bool = false
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
    case kindSelected(SpaceKind)
    case codeTyped(String)
    case createConfirmed
    case joinConfirmed
    /// '구글로 계속하기'. 창을 띄우는 일은 앱 껍데기가 하고, 받아 온 토큰이 돌아옵니다.
    case signInTapped
    case googleTokenReceived(String)
    /// '그냥 혼자 쓸래요' — 만들기에서 왔을 때만 있습니다.
    case signInGaveUp
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

    /// 시트를 열 때마다 종류도 기본(혼자)으로 되돌립니다 — 지난번에 고른 것이 남아 있으면 안 됩니다.
    public static func sheetOpened(_ state: SpaceListState, _ sheet: SpaceListSheet) -> SpaceListState {
        var next = state
        next.sheet = sheet
        next.pendingName = ""
        next.pendingCode = ""
        next.pendingKind = .personal
        return next
    }

    public static func sheetDismissed(_ state: SpaceListState) -> SpaceListState {
        var next = state
        next.sheet = .none
        next.pendingName = ""
        next.pendingCode = ""
        next.pendingKind = .personal
        next.working = false
        return next
    }

    public static func kindSelected(_ state: SpaceListState, _ kind: SpaceKind) -> SpaceListState {
        var next = state
        next.pendingKind = kind
        return next
    }

    public static func accountChanged(_ state: SpaceListState, _ signedIn: Bool) -> SpaceListState {
        var next = state
        next.signedIn = signedIn
        return next
    }

    /// 로그인 창이 끼어듭니다. **입력해 둔 것을 지우지 않습니다** — 로그인이 끝나면
    /// 그대로 이어서 만들거나 참여해야 합니다.
    public static func signInNeeded(_ state: SpaceListState, _ next: SignInNext) -> SpaceListState {
        var result = state
        result.sheet = .signIn(next: next)
        result.working = false
        return result
    }

    /// '그냥 혼자 쓸래요'. 만들기 시트로 **되돌아가되 혼자로 바꿔 둡니다** —
    /// 곧장 만들어 버리면 방금 무엇이 만들어졌는지 모른 채 목록이 하나 늘어납니다.
    /// 이름은 그대로 두어 다시 입력하지 않게 합니다.
    public static func signInGaveUp(_ state: SpaceListState) -> SpaceListState {
        var next = state
        next.sheet = .create
        next.pendingKind = .personal
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
        next.sheet = code.map { SpaceListSheet.invited(space: space, code: $0) } ?? SpaceListSheet.none
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
    private let accounts: any AuthRepository
    /// 구글 창을 띄우는 일은 **앱 껍데기**가 합니다 — UIViewController 가 필요해서요.
    /// 창을 닫으면 `nil` 이 돌아옵니다(실패가 아닙니다).
    private let presentGoogleSignIn: @MainActor @Sendable () async -> String?

    public init(
        observeSpaces: ObserveSpaces,
        refreshSpaces: RefreshSpaces,
        createSpace: CreateSpace,
        joinSpace: JoinSpace,
        accounts: any AuthRepository,
        presentGoogleSignIn: @escaping @MainActor @Sendable () async -> String?
    ) {
        self.observeSpaces = observeSpaces
        self.refreshSpaces = refreshSpaces
        self.createSpace = createSpace
        self.joinSpace = joinSpace
        self.accounts = accounts
        self.presentGoogleSignIn = presentGoogleSignIn
    }

    public func send(_ intent: SpaceListIntent) async {
        switch intent {
        case .appeared:
            // 저장해 둔 로그인을 먼저 읽습니다. 안 읽으면 켤 때마다 로그아웃으로 보입니다.
            state = SpaceListReducer.accountChanged(state, await accounts.account() != nil)
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

        case .kindSelected(let kind):
            state = SpaceListReducer.kindSelected(state, kind)

        case .codeTyped(let value):
            state.pendingCode = value

        // 같이 쓰는 짜국은 **로그인이 있어야** 만들어집니다. 없으면 로그인 창이 끼어들고,
        // 끝나면 여기로 돌아옵니다.
        case .createConfirmed:
            guard state.canCreate else { return }
            guard state.pendingKind == .personal || state.signedIn else {
                state = SpaceListReducer.signInNeeded(state, .create)
                return
            }
            await create()

        // 참여는 늘 같이 쓰는 짜국이라 로그인이 필요합니다.
        case .joinConfirmed:
            guard state.canJoin else { return }
            guard state.signedIn else {
                state = SpaceListReducer.signInNeeded(state, .join)
                return
            }
            await join()

        case .signInTapped:
            // 창을 닫으면 nil — 스스로 그만둔 것을 '실패했어요' 로 알리지 않습니다.
            guard let idToken = await presentGoogleSignIn() else { return }
            await send(.googleTokenReceived(idToken))

        case .googleTokenReceived(let idToken):
            let next: SignInNext? = if case .signIn(let value) = state.sheet { value } else { nil }
            state = SpaceListReducer.working(state)

            switch await accounts.signInWithGoogle(idToken: idToken) {
            case .ok:
                state = SpaceListReducer.accountChanged(state, true)
                // 로그인만 하고 멈추지 않습니다 — 하던 일을 이어서 합니다.
                switch next {
                case .create:
                    state.sheet = .create
                    await create()
                case .join:
                    state.sheet = .join
                    await join()
                case nil:
                    state = SpaceListReducer.sheetDismissed(state)
                }
            case .fail(let reason):
                state = SpaceListReducer.loadFailed(state, reason)
            }

        case .signInGaveUp:
            state = SpaceListReducer.signInGaveUp(state)
        }
    }

    private func create() async {
        state = SpaceListReducer.working(state)
        switch await createSpace(state.pendingName, state.pendingKind) {
        case .ok(let (space, invite)):
            state = SpaceListReducer.created(state, space, invite?.code)
        case .fail(let reason):
            state = SpaceListReducer.loadFailed(state, reason)
        }
    }

    private func join() async {
        state = SpaceListReducer.working(state)
        switch await joinSpace(state.pendingCode) {
        case .ok(let space):
            state = SpaceListReducer.joined(state, space)
        case .fail(let reason):
            state = SpaceListReducer.loadFailed(state, reason)
        }
    }
}
