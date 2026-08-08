import CoreModel
import Domain
import Foundation
import Observation

public struct DayCellUi: Equatable, Sendable, Identifiable {
    public let date: CalendarDate?
    public let coverURL: String?
    public let isToday: Bool
    public let isSunday: Bool
    public let slot: Int

    public var id: Int { slot }
}

public struct DayGroup: Equatable, Sendable, Identifiable {
    public let date: CalendarDate
    public let placeName: String?
    public let photos: [Photo]
    public let coverId: PhotoId?

    public var id: String { date.iso }
}

public struct CalendarState: Equatable, Sendable {
    public let spaceId: SpaceId
    public var year: Int
    public var month: Int
    public let today: CalendarDate
    public var cells: [DayCellUi] = []
    public var days: [DayGroup] = []
    /// 접힘은 **기억합니다.** 다른 탭에 갔다 와도 접힌 채로 돌아와야 접는 의미가 있습니다.
    public var collapsed = false
    public var selected: CalendarDate?

    public init(spaceId: SpaceId, today: CalendarDate) {
        self.spaceId = spaceId
        self.today = today
        self.year = today.year
        self.month = today.month
    }

    /// 고른 날이 있으면 그 날만, 없으면 그달 전부.
    public var visibleDays: [DayGroup] {
        guard let selected else { return days }
        let picked = days.filter { $0.date == selected }
        return picked.isEmpty ? days : picked
    }
}

public enum CalendarReducer {

    public static func rebuild(
        _ state: CalendarState, board: PhotoBoard, regionNames: [String: Region]
    ) -> CalendarState {
        var next = state

        next.cells = CalendarMonth.grid(year: state.year, month: state.month)
            .enumerated()
            .map { slot, date in
                DayCellUi(
                    date: date,
                    coverURL: date.flatMap { board.dayCover($0)?.downloadURL },
                    isToday: date == state.today,
                    isSunday: date.map(CalendarMonth.isSunday) ?? false,
                    slot: slot
                )
            }

        // 그달에 사진이 있는 날만, 최근 날짜부터
        next.days = board.byDay
            .filter { $0.key.year == state.year && $0.key.month == state.month }
            .sorted { $0.key > $1.key }
            .map { date, photos in
                DayGroup(
                    date: date,
                    placeName: photos.first.flatMap { regionNames[$0.regionCode.value]?.displayName },
                    photos: photos,
                    coverId: board.dayCover(date)?.id
                )
            }

        return next
    }

    public static func monthChanged(_ state: CalendarState, by delta: Int) -> CalendarState {
        var next = state
        var month = state.month + delta
        var year = state.year
        while month < 1 { month += 12; year -= 1 }
        while month > 12 { month -= 12; year += 1 }
        next.month = month
        next.year = year
        next.selected = nil
        return next
    }

    public static func collapseToggled(_ state: CalendarState) -> CalendarState {
        var next = state
        next.collapsed.toggle()
        return next
    }
}

@MainActor
@Observable
public final class CalendarStore {
    public private(set) var state: CalendarState

    private let observeBoard: ObservePhotoBoard
    private let refreshPhotos: RefreshPhotos
    private let setCoverPhoto: SetCoverPhoto
    private let catalog: any RegionCatalog
    /// 무슨 일이 있었는지 남기는 클로저. 어디로 가는지는 앱 껍데기가 정한다
    /// (`AppContainer.track` — Analytics). 기본은 아무것도 안 하는 것 — 테스트가 조용하다.
    private let track: @Sendable (String, [String: String]) -> Void

    private var board: PhotoBoard = .empty
    private var names: [String: Region] = [:]

    public init(
        spaceId: SpaceId, today: CalendarDate,
        observeBoard: ObservePhotoBoard, refreshPhotos: RefreshPhotos,
        setCoverPhoto: SetCoverPhoto, catalog: any RegionCatalog,
        track: @escaping @Sendable (String, [String: String]) -> Void = { _, _ in }
    ) {
        self.state = CalendarState(spaceId: spaceId, today: today)
        self.observeBoard = observeBoard
        self.refreshPhotos = refreshPhotos
        self.setCoverPhoto = setCoverPhoto
        self.catalog = catalog
        self.track = track
    }

    public func refresh() async {
        // 받아 둔 것만 읽으면 앱을 켰을 때 늘 비어 있습니다. 먼저 받아옵니다.
        _ = await refreshPhotos(state.spaceId)
        if names.isEmpty {
            names = Dictionary(
                (await catalog.all()).map { ($0.code.value, $0) },
                uniquingKeysWith: { first, _ in first }
            )
        }
        board = await observeBoard(state.spaceId)
        state = CalendarReducer.rebuild(state, board: board, regionNames: names)
    }

    public func move(by delta: Int) {
        state = CalendarReducer.monthChanged(state, by: delta)
        state = CalendarReducer.rebuild(state, board: board, regionNames: names)
    }

    /// 옆으로 넘겨서 고른 달. 몇 칸을 건너뛰었는지 모르므로 달을 그대로 받습니다.
    /// 안드로이드 `CalendarIntent.MonthSelected` 와 같은 자리입니다.
    public func setMonth(year: Int, month: Int) {
        guard year != state.year || month != state.month else { return }
        state.year = year
        state.month = month
        state.selected = nil
        state = CalendarReducer.rebuild(state, board: board, regionNames: names)
    }

    public func select(_ date: CalendarDate) { state.selected = date }

    public func toggleCollapse() { state = CalendarReducer.collapseToggled(state) }

    public func setCover(_ id: PhotoId, on date: CalendarDate) async {
        switch await setCoverPhoto(state.spaceId, .day(date), id) {
        case .ok: track("cover_set_day", [:])
        case .fail: track("cover_set_day_failed", [:])
        }
        await refresh()
    }
}
