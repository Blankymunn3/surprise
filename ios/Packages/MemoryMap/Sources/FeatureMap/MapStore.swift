import CoreModel
import Domain
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
    public var selected: PhotoId?
}

public struct MapState: Equatable, Sendable {
    public let spaceId: SpaceId
    public var pins: [RegionPin] = []
    public var query = ""
    public var results: [Region] = []
    public var sheet: RegionSheetUi?
    public var focus: CoordinatePair?

    public init(spaceId: SpaceId) { self.spaceId = spaceId }

    /// 대표로 지정할 수 있는가 — 사진을 하나 골랐고 그게 이미 대표가 아닐 때만.
    public var canSetCover: Bool {
        guard let sheet, let picked = sheet.selected else { return false }
        return picked != sheet.coverId
    }
}

public struct CoordinatePair: Equatable, Sendable {
    public let latitude: Double
    public let longitude: Double
    public init(latitude: Double, longitude: Double) {
        self.latitude = latitude
        self.longitude = longitude
    }
}

@MainActor
@Observable
public final class MapStore {
    public private(set) var state: MapState

    private let observeBoard: ObservePhotoBoard
    private let searchRegions: SearchRegions
    private let setCoverPhoto: SetCoverPhoto
    private let catalog: any RegionCatalog

    private var board: PhotoBoard = .empty

    public init(
        spaceId: SpaceId, observeBoard: ObservePhotoBoard, searchRegions: SearchRegions,
        setCoverPhoto: SetCoverPhoto, catalog: any RegionCatalog
    ) {
        self.state = MapState(spaceId: spaceId)
        self.observeBoard = observeBoard
        self.searchRegions = searchRegions
        self.setCoverPhoto = setCoverPhoto
        self.catalog = catalog
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
    }

    public func search(_ query: String) async {
        state.query = query
        state.results = query.isEmpty ? [] : await searchRegions(query)
    }

    public func open(_ region: Region) async {
        let center = await catalog.center(of: region.code)
        state.query = region.displayName
        state.results = []
        state.focus = center.map { CoordinatePair(latitude: $0.0, longitude: $0.1) }
        state.sheet = RegionSheetUi(
            region: region,
            photos: board.photos(in: region.code),
            coverId: board.regionCover(region.code)?.id,
            selected: nil
        )
    }

    /// 지도를 누르면 그 좌표가 어느 지역인지 **기기 안에서** 판정합니다 (사진 EXIF 와 같은 길).
    public func tapMap(latitude: Double, longitude: Double) async {
        guard let region = await catalog.regionAt(latitude: latitude, longitude: longitude) else { return }
        await open(region)
    }

    public func select(_ id: PhotoId) {
        guard var sheet = state.sheet else { return }
        sheet.selected = sheet.selected == id ? nil : id
        state.sheet = sheet
    }

    public func setCover() async {
        guard state.canSetCover, let sheet = state.sheet, let picked = sheet.selected else { return }
        _ = await setCoverPhoto(state.spaceId, .region(sheet.region.code), picked)
        var next = sheet
        next.coverId = picked
        next.selected = nil
        state.sheet = next
    }

    public func dismissSheet() {
        state.sheet = nil
        state.query = ""
        state.results = []
    }
}
