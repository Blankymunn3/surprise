import CoreCommon
import CoreModel
import Domain
import Foundation
import Observation

/// 고른 사진 한 장. 화면 상태에 플랫폼 타입(PHAsset/URL)을 넣지 않습니다 —
/// 넣으면 테스트에서 만들 수가 없습니다.
public struct PickedPhoto: Hashable, Sendable, Identifiable {
    public let uri: String
    public var id: String { uri }
    public init(uri: String) { self.uri = uri }
}

public enum UploadStep: Equatable, Sendable {
    case editing
    case reading
    case uploading
    case done
    case failed(savedLocally: Bool)
}

public struct UploadState: Equatable, Sendable {
    public let spaceId: SpaceId
    public var picked: [PickedPhoto] = []
    public var region: Region?
    public var takenOn: CalendarDate?
    public var regionFromExif = false
    public var dateFromExif = false
    public var regionMismatch = 0
    public var dateMismatch = 0
    public var step: UploadStep = .editing
    public var regionQuery = ""
    public var regionResults: [Region] = []
    public var pickingRegion = false

    public init(spaceId: SpaceId) { self.spaceId = spaceId }

    /// **지역은 비울 수 없습니다** — 지도에 올라갈 자리가 없어집니다.
    public var canUpload: Bool {
        !picked.isEmpty && region != nil && takenOn != nil && step == .editing
    }

    /// "3장 중 2장은 다른 곳이에요" — 나눠 올리라고 알려 주는 문구.
    public var mismatchNotice: String? {
        if regionMismatch > 0 { return "\(picked.count)장 중 \(regionMismatch)장은 다른 곳이에요" }
        if dateMismatch > 0 { return "\(picked.count)장 중 \(dateMismatch)장은 다른 날이에요" }
        return nil
    }
}

public enum UploadReducer {

    public static func picked(_ state: UploadState, _ items: [PickedPhoto]) -> UploadState {
        var next = state
        next.picked = items
        next.step = items.isEmpty ? .editing : .reading
        return next
    }

    public static func hintsRead(
        _ state: UploadState, _ defaults: UploadPlan.Defaults, region: Region?
    ) -> UploadState {
        var next = state
        next.region = region ?? state.region
        next.takenOn = defaults.takenOn
        next.regionFromExif = defaults.regionFromExif && region != nil
        next.dateFromExif = defaults.dateFromExif
        next.regionMismatch = defaults.regionMismatch
        next.dateMismatch = defaults.dateMismatch
        next.step = .editing
        return next
    }

    public static func regionChosen(_ state: UploadState, _ region: Region) -> UploadState {
        var next = state
        next.region = region
        // 사용자가 직접 고른 값이므로 '자동' 딱지를 뗍니다
        next.regionFromExif = false
        next.regionMismatch = 0
        next.pickingRegion = false
        next.regionQuery = ""
        next.regionResults = []
        return next
    }

    /// 올리기가 실패하면 **기기에 저장하고 알립니다.** 사진을 잃지 않습니다.
    public static func failed(_ state: UploadState, savedLocally: Bool) -> UploadState {
        var next = state
        next.step = .failed(savedLocally: savedLocally)
        return next
    }
}

@MainActor
@Observable
public final class UploadStore {
    public private(set) var state: UploadState

    private let readHints: @Sendable (String) async -> UploadPlan.ExifHint
    private let toJpeg: @Sendable (String) async -> Data?
    private let uploadPhotos: UploadPhotos
    private let searchRegions: SearchRegions
    private let catalog: any RegionCatalog
    private let today: CalendarDate

    public init(
        spaceId: SpaceId,
        readHints: @escaping @Sendable (String) async -> UploadPlan.ExifHint,
        toJpeg: @escaping @Sendable (String) async -> Data?,
        uploadPhotos: UploadPhotos,
        searchRegions: SearchRegions,
        catalog: any RegionCatalog,
        today: CalendarDate
    ) {
        self.state = UploadState(spaceId: spaceId)
        self.readHints = readHints
        self.toJpeg = toJpeg
        self.uploadPhotos = uploadPhotos
        self.searchRegions = searchRegions
        self.catalog = catalog
        self.today = today
    }

    public func pick(_ items: [PickedPhoto]) async {
        state = UploadReducer.picked(state, items)
        guard !items.isEmpty else { return }

        var hints: [UploadPlan.ExifHint] = []
        for item in items { hints.append(await readHints(item.uri)) }

        let defaults = UploadPlan.defaults(hints: hints, today: today)
        var region: Region?
        if let code = defaults.regionCode {
            region = await catalog.find(code.value)
        }
        state = UploadReducer.hintsRead(state, defaults, region: region)
    }

    public func search(_ query: String) async {
        state.regionQuery = query
        state.regionResults = query.isEmpty ? [] : await searchRegions(query)
    }

    public func choose(_ region: Region) {
        state = UploadReducer.regionChosen(state, region)
    }

    public func startPickingRegion() { state.pickingRegion = true }

    /// 고르다 말고 닫아도 **원래 값은 그대로** 둡니다. 검색어만 지웁니다.
    public func cancelPickingRegion() {
        state.pickingRegion = false
        state.regionQuery = ""
        state.regionResults = []
    }

    /// 날짜를 직접 고르면 '자동' 딱지를 뗍니다 — 지역과 같은 규칙입니다.
    public func setDate(_ date: CalendarDate) {
        state.takenOn = date
        state.dateFromExif = false
        state.dateMismatch = 0
    }

    public func confirm() async {
        guard state.canUpload, let region = state.region, let takenOn = state.takenOn else { return }
        state.step = .uploading

        var drafts: [NewPhoto] = []
        for (index, picked) in state.picked.enumerated() {
            guard let data = await toJpeg(picked.uri) else { continue }
            drafts.append(NewPhoto(
                localId: "\(picked.uri)#\(index)",
                data: data, regionCode: region.code, takenOn: takenOn
            ))
        }

        guard !drafts.isEmpty else {
            state = UploadReducer.failed(state, savedLocally: true)
            return
        }

        switch await uploadPhotos(state.spaceId, drafts) {
        case .ok:
            state.step = .done
        case .fail:
            // 사진을 잃지 않는 것이 먼저입니다
            state = UploadReducer.failed(state, savedLocally: true)
        }
    }
}
