import CoreCommon
import CoreModel
import Foundation

/// 아직 올리지 않은 사진. `data` 는 이미 760px / 품질 0.72 로 줄인 JPEG 입니다.
public struct NewPhoto: Sendable {
    public let localId: String
    public let data: Data
    public let regionCode: RegionCode
    public let takenOn: CalendarDate

    public init(localId: String, data: Data, regionCode: RegionCode, takenOn: CalendarDate) {
        self.localId = localId
        self.data = data
        self.regionCode = regionCode
        self.takenOn = takenOn
    }
}

public protocol PhotoRepository: Sendable {
    func board(for spaceId: SpaceId) async -> PhotoBoard
    func refresh(spaceId: SpaceId) async -> Outcome<Void>
    func upload(spaceId: SpaceId, photos: [NewPhoto]) async -> Outcome<[Photo]>
    func delete(spaceId: SpaceId, id: PhotoId) async -> Outcome<Void>
    func setCover(spaceId: SpaceId, key: CoverKey, id: PhotoId) async -> Outcome<Void>
}

public protocol SpaceRepository: Sendable {
    func spaces() async -> [Space]
    func refresh() async -> Outcome<Void>
    /// 이름을 정하는 순간 초대 코드도 같이 나옵니다 — **같이 쓰는 짜국만**.
    /// 혼자 쓰는 짜국은 초대할 사람이 없어 코드가 `nil` 입니다.
    func create(name: String, kind: SpaceKind) async -> Outcome<(Space, Invite?)>
    func join(code: String) async -> Outcome<Space>
    /// 초대 코드를 하나 더 만듭니다. 코드는 여럿이어도 모두 같은 짜국을 가리킵니다.
    func newInvite(spaceId: SpaceId) async -> Outcome<Invite>
    func rename(spaceId: SpaceId, name: String) async -> Outcome<Void>
}

/// 로그인. **같이 쓰는 짜국에서만** 필요합니다 (`docs/app/AUTH.md`).
///
/// 구글 로그인 SDK 는 이 뒤에 숨어 있습니다 — 도메인은 "구글 ID 토큰을 받아 왔다" 까지만
/// 알고, 그것을 Firebase 토큰으로 바꾸는 일은 데이터 계층이 합니다.
public protocol AuthRepository: Sendable {
    /// 지금 로그인한 사람. 로그인 전에는 `nil`.
    func account() async -> Account?

    /// 구글 로그인 SDK 가 받아 온 ID 토큰으로 Firebase 세션을 엽니다.
    func signInWithGoogle(idToken: String) async -> Outcome<Account>

    /// 애플 로그인이 받아 온 ID 토큰으로 세션을 엽니다. [nonce] 는 요청 때 만든 원문.
    /// [fallbackName] 은 애플이 **첫 로그인에만** 주는 이름입니다 — 토큰에는 이름이
    /// 없어서, 이때 안 받아 두면 멤버 목록에 이메일 조각이 뜹니다.
    func signInWithApple(idToken: String, nonce: String, fallbackName: String?) async -> Outcome<Account>

    func signOut() async

    /// 요청 헤더에 얹을 Firebase ID 토큰. 낡았으면 **여기서 알아서 새로 받습니다.**
    /// 로그인 전이거나 갱신이 실패하면 `nil` — 부르는 쪽은 헤더를 빼고 보냅니다.
    func idToken() async -> String?
}

public protocol RegionCatalog: Sendable {
    func all() async -> [Region]
    func find(_ codeValue: String) async -> Region?
    /// 좌표 → 지역. **기기 안에서만** 판정합니다.
    func regionAt(latitude: Double, longitude: Double) async -> Region?
    /// 지도에 표시할 자리 (위도, 경도)
    func center(of code: RegionCode) async -> (Double, Double)?

    /// 지역의 **면**. 테두리를 그릴 때도, 사진으로 칠할 때도 이걸 씁니다.
    /// `폴리곤 → 고리 → 점` 세 겹입니다. 섬이 많으면 폴리곤이 여럿, 안이 뚫렸으면
    /// 한 폴리곤에 고리가 여럿입니다.
    func shape(of code: RegionCode) async -> [[[GeoPoint]]]
}
