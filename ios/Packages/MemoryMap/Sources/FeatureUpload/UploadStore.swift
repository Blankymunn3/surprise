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

/// 올릴 사진 한 장과 거기 붙은 값들.
public struct UploadItem: Equatable, Sendable, Identifiable {
    public let uri: String
    public var region: Region?
    public var takenOn: CalendarDate
    /// '자동' 딱지 — 사진에서 읽어 채웠다는 표시. 사용자가 고치면 떨어집니다.
    public var regionAuto: Bool
    public var dateAuto: Bool

    public var id: String { uri }

    public init(
        uri: String, region: Region?, takenOn: CalendarDate,
        regionAuto: Bool = false, dateAuto: Bool = false
    ) {
        self.uri = uri
        self.region = region
        self.takenOn = takenOn
        self.regionAuto = regionAuto
        self.dateAuto = dateAuto
    }
}

/// 몇 곳 · 며칠에 걸쳐 있는지.
public struct SplitCounts: Equatable, Sendable {
    public let places: Int
    public let days: Int

    public init(places: Int, days: Int) {
        self.places = places
        self.days = days
    }
}

public struct UploadState: Equatable, Sendable {
    public let spaceId: SpaceId
    /**
     고른 사진들. **한 장 한 장이 제 지역·날짜를 듭니다.**

     예전에는 화면 전체에 지역 하나·날짜 하나였습니다. 그러면 강릉에서 찍은 것과
     속초에서 찍은 것을 한 번에 고른 사람은, 많은 쪽으로 뭉뚱그려 올리거나
     두 번에 나눠 올려야 했습니다. 사진마다 들고 있으면 그냥 한 번에 올라갑니다.
     */
    public var items: [UploadItem] = []
    public var step: UploadStep = .editing
    /// 지역을 고르는 중인 사진. `nil` 이면 목록 화면입니다.
    public var editingRegionOf: String?
    public var regionQuery = ""
    public var regionResults: [Region] = []

    public init(spaceId: SpaceId) { self.spaceId = spaceId }

    /// **지역이 빈 사진이 하나라도 있으면 안 됩니다** — 지도에 올라갈 자리가 없어집니다.
    public var canUpload: Bool {
        guard !items.isEmpty, items.allSatisfy({ $0.region != nil }) else { return false }
        if case .failed = step { return true }
        return step == .editing
    }

    /**
     한 번에 고른 사진이 여러 곳·여러 날에 걸쳐 있는지. 한 곳 한 날이면 `nil` —
     알릴 것이 없습니다.

     **수만 돌려주고 문구는 화면이 짓습니다.** Store 가 글을 만들면 그 글이 테스트에
     박혀서, 문구를 고칠 때마다 멀쩡한 테스트가 깨집니다. 안드로이드 `splitCounts()` 와 같습니다.
     */
    public var splitCounts: SplitCounts? {
        let places = Set(items.compactMap { $0.region?.code }).count
        let days = Set(items.map(\.takenOn)).count
        guard places > 1 || days > 1 else { return nil }
        return SplitCounts(places: places, days: days)
    }
}

public enum UploadReducer {

    public static func picked(_ state: UploadState, _ items: [PickedPhoto]) -> UploadState {
        var next = state
        next.items = items.map {
            UploadItem(uri: $0.uri, region: nil, takenOn: CalendarDate(year: 1, month: 1, day: 1))
        }
        next.step = items.isEmpty ? .editing : .reading
        return next
    }

    /// 사진에서 읽은 값을 **한 장씩** 채웁니다.
    public static func hintsRead(_ state: UploadState, _ items: [UploadItem]) -> UploadState {
        var next = state
        next.items = items
        next.step = .editing
        return next
    }

    public static func regionPickerOpened(_ state: UploadState, _ uri: String) -> UploadState {
        var next = state
        next.editingRegionOf = uri
        next.regionQuery = ""
        next.regionResults = []
        return next
    }

    public static func regionPickerDismissed(_ state: UploadState) -> UploadState {
        var next = state
        next.editingRegionOf = nil
        next.regionQuery = ""
        next.regionResults = []
        return next
    }

    public static func regionChosen(_ state: UploadState, _ region: Region) -> UploadState {
        guard let uri = state.editingRegionOf else { return state }
        var next = state
        next.items = state.items.map {
            // 사용자가 직접 고른 값이므로 '자동' 딱지를 뗍니다
            guard $0.uri == uri else { return $0 }
            var item = $0
            item.region = region
            item.regionAuto = false
            return item
        }
        return regionPickerDismissed(next)
    }

    public static func dateChosen(
        _ state: UploadState, _ uri: String, _ date: CalendarDate
    ) -> UploadState {
        var next = state
        next.items = state.items.map {
            guard $0.uri == uri else { return $0 }
            var item = $0
            item.takenOn = date
            item.dateAuto = false
            return item
        }
        return next
    }

    /// 올리기가 실패하면 **기기에 저장하고 알립니다.** 사진을 잃지 않습니다.
    public static func failed(_ state: UploadState, savedLocally: Bool) -> UploadState {
        var next = state
        next.step = .failed(savedLocally: savedLocally)
        return next
    }

    /// '다시 시도' — 고친 값은 그대로 두고 올릴 수 있는 상태로만 되돌립니다.
    public static func retry(_ state: UploadState) -> UploadState {
        var next = state
        next.step = .editing
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
    /// 지역 시트에서 열었을 때 미리 정해 둔 지역.
    private var preselected: Region?

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

        // 제 값이 없는 사진은 **여럿의 값**으로 메웁니다. 한 장만 위치가 안 찍혀
        // 있다고 그 한 장만 빈칸으로 두면, 사용자가 그것만 따로 찾아 채워야 합니다.
        let fallback = UploadPlan.defaults(hints: hints, today: today)
        var fallbackRegion: Region?
        if let code = fallback.regionCode { fallbackRegion = await catalog.find(code.value) }

        var built: [UploadItem] = []
        for (index, item) in items.enumerated() {
            let hint = hints[index]
            var own: Region?
            if let code = hint.regionCode { own = await catalog.find(code.value) }
            // 지도에서 콕 집어 고른 곳이 있으면 그것이 먼저입니다.
            let region = preselected ?? own ?? fallbackRegion
            built.append(UploadItem(
                uri: item.uri,
                region: region,
                takenOn: hint.takenOn ?? fallback.takenOn,
                // 사진에서 왔든 여럿에서 메웠든 사용자가 고른 값은 아닙니다.
                regionAuto: preselected == nil && region != nil,
                dateAuto: hint.takenOn != nil || fallback.dateFromExif
            ))
        }
        state = UploadReducer.hintsRead(state, built)
    }

    public func search(_ query: String) async {
        state.regionQuery = query
        state.regionResults = query.isEmpty ? [] : await searchRegions(query)
    }

    public func choose(_ region: Region) {
        state = UploadReducer.regionChosen(state, region)
    }

    /**
     지역 시트에서 열었을 때 — 그 지역을 **미리 정해 둡니다.**

     사진을 고르기 **전에** 불리므로 여기서 바로 넣을 수는 없습니다. 들고 있다가
     사진이 들어온 뒤 EXIF 값 대신 이 값을 씁니다 — 사용자가 지도에서 콕 집어
     고른 곳이 사진에 찍힌 좌표보다 정확합니다.
     */
    public func preselect(_ region: Region) { preselected = region }

    public func startPickingRegion(_ uri: String) {
        state = UploadReducer.regionPickerOpened(state, uri)
    }

    /// 고르다 말고 닫아도 **원래 값은 그대로** 둡니다. 검색어만 지웁니다.
    public func cancelPickingRegion() {
        state = UploadReducer.regionPickerDismissed(state)
    }

    /// 날짜를 직접 고르면 '자동' 딱지를 뗍니다 — 지역과 같은 규칙입니다.
    public func setDate(_ uri: String, _ date: CalendarDate) {
        state = UploadReducer.dateChosen(state, uri, date)
    }

    public func retry() { state = UploadReducer.retry(state) }

    public func confirm() async {
        guard state.canUpload else { return }
        state.step = .uploading

        var drafts: [NewPhoto] = []
        for (index, item) in state.items.enumerated() {
            guard let region = item.region, let data = await toJpeg(item.uri) else { continue }
            // 사진마다 **제 지역·제 날짜**로 올라갑니다.
            drafts.append(NewPhoto(
                localId: "\(item.uri)#\(index)",
                data: data, regionCode: region.code, takenOn: item.takenOn
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
