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
    /// 이름을 정하는 순간 초대 코드도 같이 나옵니다.
    func create(name: String) async -> Outcome<(Space, Invite)>
    func join(code: String) async -> Outcome<Space>
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
