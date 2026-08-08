import CoreModel
import Domain
import Foundation
import Observation

public struct RegionPin: Equatable, Sendable, Identifiable {
    public let region: Region
    public let latitude: Double
    public let longitude: Double
    public let coverURL: String?
    public let photoCount: Int

    public var id: String { region.code.value }
}

public struct RegionSheetUi: Equatable, Sendable {
    public let region: Region
    public let photos: [Photo]
    public var coverId: PhotoId?
}

public struct MapState: Equatable, Sendable {
    public let spaceId: SpaceId
    public var pins: [RegionPin] = []
    public var query = ""
    public var results: [Region] = []
    public var sheet: RegionSheetUi?
    public var focus: MapFocus?
    /// 몇 번째 맞춤인지. 같은 지역을 다시 골라도 화면을 다시 맞추려고 셉니다 —
    /// 맞출 곳만 보면 값이 그대로라 지도가 꿈쩍도 안 합니다.
    public var focusCount = 0
    /// 고른 지역의 테두리. 고리 하나가 닫힌 선 하나입니다.
    public var outline: [[GeoPoint]] = []
    /// 다녀온 지역들 — 각자의 대표사진으로 칠할 면.
    public var fills: [RegionFill] = []

    public init(spaceId: SpaceId) { self.spaceId = spaceId }

}

/**
 사진으로 칠할 지역 하나.

 같은지 비교할 때 **코드와 사진 주소만** 봅니다 — 경계 점이 수천 개라 매번 견주면
 화면을 다시 그릴 때마다 그 값을 통째로 훑게 됩니다. 안드로이드 `RegionFill` 과 같습니다.
 */
public struct RegionFill: Equatable, Sendable, Identifiable {
    public let code: String
    public let coverURL: String
    public let polygons: [[[GeoPoint]]]

    public var id: String { code }

    public static func == (lhs: RegionFill, rhs: RegionFill) -> Bool {
        lhs.code == rhs.code && lhs.coverURL == rhs.coverURL
    }
}

/**
 지역을 골랐을 때 지도를 **어디에 맞출 것인가**. 안드로이드 `MapFocus` 와 같습니다.

 배율 하나로 고정하면 안 됩니다 — 시군구에 맞춘 배율로 나라를 열면 나라 한복판만 크게
 보이고 정작 고른 곳이 어디까지인지는 안 보입니다. 경계가 있으면 **그 경계가 통째로
 들어오게** 맞춥니다.
 */
public enum MapFocus: Equatable, Sendable {
    /// 경계가 있는 지역 — 이 네모가 화면 안에 다 들어오게.
    case area(south: Double, west: Double, north: Double, east: Double)
    /// 경계 없이 좌표만 있는 장소 — 맞출 넓이가 없어 배율을 정해 줍니다.
    case spot(latitude: Double, longitude: Double)
}

/**
 경계를 감싸는 가장 작은 네모.

 경도는 **짧은 쪽으로** 감쌉니다. 러시아·피지처럼 날짜변경선을 넘는 나라는 경도가
 -180 과 180 양쪽에 흩어져 있어서, 그냥 최솟값과 최댓값을 쓰면 지구 한 바퀴가 됩니다.
 대신 경도들 사이에서 **가장 넓게 빈 구간**을 찾아 그 반대쪽을 씁니다 — 아무 점도 없는
 그 구간이 곧 지역의 바깥입니다.

 선을 넘는 경우 `east` 가 180 을 넘어갑니다 (러시아는 서 19°, 동 191°).
 지도가 그대로 받아 씁니다 — 180 안으로 접으면 다시 지구 한 바퀴가 되니까요.

 안드로이드 `boundsOf` 와 같은 규칙입니다.
 */
public func boundsOf(_ polygons: [[[GeoPoint]]]) -> MapFocus? {
    var south = Double.greatestFiniteMagnitude
    var north = -Double.greatestFiniteMagnitude
    var longitudes: [Double] = []

    for polygon in polygons {
        for ring in polygon {
            for point in ring {
                south = min(south, point.latitude)
                north = max(north, point.latitude)
                longitudes.append(point.longitude)
            }
        }
    }
    guard !longitudes.isEmpty else { return nil }
    longitudes.sort()

    // 선을 넘지 않는 경우: 가장 넓게 빈 구간은 최댓값에서 최솟값으로 되돌아가는 바깥쪽입니다.
    var west = longitudes[0]
    var east = longitudes[longitudes.count - 1]
    var widest = longitudes[0] + fullTurn - longitudes[longitudes.count - 1]

    for i in 1..<longitudes.count {
        let hole = longitudes[i] - longitudes[i - 1]
        if hole > widest {
            widest = hole
            west = longitudes[i]
            east = longitudes[i - 1] + fullTurn
        }
    }

    // 한 점으로 뭉친 경계는 맞출 넓이가 없습니다. 배율을 정해 주는 쪽에 맡깁니다.
    if north - south < hair && east - west < hair { return nil }

    return .area(south: south, west: west, north: north, east: east)
}

private let fullTurn = 360.0

/// 100m 남짓. 이보다 좁은 네모는 넓이가 있다고 보지 않습니다.
private let hair = 0.001

@MainActor
@Observable
public final class MapStore {
    public private(set) var state: MapState

    private let observeBoard: ObservePhotoBoard
    private let searchRegions: SearchRegions
    private let setCoverPhoto: SetCoverPhoto
    private let catalog: any RegionCatalog
    /// 무슨 일이 있었는지 남기는 클로저. 어디로 가는지는 앱 껍데기가 정한다
    /// (`AppContainer.track` — Analytics). 기본은 아무것도 안 하는 것 — 테스트가 조용하다.
    private let track: @Sendable (String, [String: String]) -> Void

    private var board: PhotoBoard = .empty

    public init(
        spaceId: SpaceId, observeBoard: ObservePhotoBoard, searchRegions: SearchRegions,
        setCoverPhoto: SetCoverPhoto, catalog: any RegionCatalog,
        track: @escaping @Sendable (String, [String: String]) -> Void = { _, _ in }
    ) {
        self.state = MapState(spaceId: spaceId)
        self.observeBoard = observeBoard
        self.searchRegions = searchRegions
        self.setCoverPhoto = setCoverPhoto
        self.catalog = catalog
        self.track = track
    }

    public func refresh() async {
        board = await observeBoard(state.spaceId)
        var pins: [RegionPin] = []
        for (code, photos) in board.byRegion {
            guard let region = await catalog.find(code.value),
                  let center = await catalog.center(of: code) else { continue }
            pins.append(RegionPin(
                region: region, latitude: center.0, longitude: center.1,
                coverURL: board.regionCover(code)?.downloadURL, photoCount: photos.count
            ))
        }
        state.pins = pins.sorted { $0.region.displayName < $1.region.displayName }

        // 다녀온 지역을 그 지역의 대표사진으로 칠합니다.
        // 사진이 없는 지역은 칠할 것이 없으니 건너뜁니다 — 표시만 찍힙니다.
        var painted: [RegionFill] = []
        for pin in state.pins {
            guard let cover = pin.coverURL else { continue }
            let polygons = await catalog.shape(of: pin.region.code)
            guard !polygons.isEmpty else { continue }
            painted.append(RegionFill(code: pin.region.code.value, coverURL: cover, polygons: polygons))
        }
        state.fills = painted
    }

    public func search(_ query: String) async {
        state.query = query
        state.results = query.isEmpty ? [] : await searchRegions(query)
    }

    /**
     경계선을 받아 와 테두리로도 쓰고, 지도를 맞출 범위로도 씁니다.

     경계가 있으면 **그 경계가 다 들어오게** 맞춥니다. 경계가 없는 장소만 가운데 좌표에
     배율을 정해 세웁니다 — 맞출 넓이가 없으니까요.
     */
    public func open(_ region: Region) async {
        track("region_open", ["code": region.code.value])
        let polygons = await catalog.shape(of: region.code)
        state.query = region.displayName
        state.results = []
        if let bounds = boundsOf(polygons) {
            state.focus = bounds
        } else if let center = await catalog.center(of: region.code) {
            state.focus = .spot(latitude: center.0, longitude: center.1)
        } else {
            state.focus = nil
        }
        state.focusCount += 1
        state.outline = polygons.flatMap { $0 }
        state.sheet = RegionSheetUi(
            region: region,
            photos: board.photos(in: region.code),
            coverId: board.regionCover(region.code)?.id
        )
    }

    /// 지도를 누르면 그 좌표가 어느 지역인지 **기기 안에서** 판정합니다 (사진 EXIF 와 같은 길).
    public func tapMap(latitude: Double, longitude: Double) async {
        guard let region = await catalog.regionAt(latitude: latitude, longitude: longitude) else { return }
        await open(region)
    }

    /// 시트에서 사진을 누름 = **그 사진을 대표로 지정.**
    ///
    /// 고르고 나서 '대표로 지정' 을 또 누르는 두 단계였는데, 시트에서 사진을 누르는
    /// 일이 그것 말고는 없어서 한 단계로 합쳤습니다. 이미 대표인 것을 또 누르면
    /// 아무 일도 없습니다 — 같은 값을 서버에 다시 쓸 까닭이 없습니다.
    public func setCover(_ id: PhotoId) async {
        guard let sheet = state.sheet, sheet.coverId != id else { return }
        switch await setCoverPhoto(state.spaceId, .region(sheet.region.code), id) {
        case .ok:
            track("cover_set_region", [:])
        case .fail:
            // 안드로이드는 실패를 알리는데 여기는 조용했다 — 우선 기록만 남긴다.
            track("cover_set_region_failed", [:])
        }
        var next = sheet
        next.coverId = id
        state.sheet = next
    }

    public func dismissSheet() {
        state.sheet = nil
        state.query = ""
        state.results = []
        // 테두리도 같이 지웁니다. 남겨 두면 닫은 지역이 계속 표시된 채로 남습니다.
        state.outline = []
        // focus 도 비웁니다 — 닫기는 "그 지역 보기"의 끝이라, 남겨 두면 어떤
        // 경로로든 카메라가 옛 지역으로 되돌아갈 여지가 됩니다(안드와 같은 규칙).
        state.focus = nil
    }
}
