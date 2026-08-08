import CoreModel
import DesignSystem
import Domain
import Foundation
import SwiftUI

/**
 요일 이름. **순서가 뜻을 가집니다** — 코드가 요일 번호로 꺼내 씁니다. 일요일부터.

 패미컴 스타일 달력(`CalendarPlastic.swift`)도 같은 것을 씁니다. 스위프트의
 `private` 은 **파일 안까지**라 여기서는 열어 둡니다.

 ⚠️ **키를 보간으로 만들면 안 됩니다.** `localized("calendar_weekday_\(i)")` 는
 `String.LocalizationValue` 를 서식 문자열로 만들어서, 찾는 키가 `calendar_weekday_%lld`
 같은 것이 됩니다 — 번역 파일에 그런 키가 없으니 **키를 그대로 화면에 뿌립니다.**
 키는 하나씩 글자 그대로 적어야 합니다.
 */
private let weekdayKeys: [String.LocalizationValue] = [
    "calendar_weekday_0", "calendar_weekday_1", "calendar_weekday_2", "calendar_weekday_3",
    "calendar_weekday_4", "calendar_weekday_5", "calendar_weekday_6",
]

var weekdays: [String] { weekdayKeys.map { localized($0) } }

/// 달력 탭. 지도 탭과 **같은 밝은 바탕**입니다 — 탭 하나 옮겼다고 앱이 뒤집히면 안 됩니다.
public struct CalendarView: View {
    @State private var store: CalendarStore

    /// 넘김의 기준이 되는 달. 처음 보인 달을 가운데 페이지로 잡습니다.
    @State private var anchor: (year: Int, month: Int)?
    @State private var page = CalendarView.pageCenter
    @State private var gridWidth: CGFloat = 0

    /// ±100년. 넘기다 끝에 닿을 일은 없습니다. 안드로이드와 같은 값입니다.
    private static let pageCount = 2401
    private static let pageCenter = pageCount / 2
    /// 6줄이면 어떤 달이든 들어갑니다. 늘 6줄로 그려야 넘길 때 높이가 안 바뀝니다.
    private static let weekRows = 6

    /// 사진 올리기를 엽니다.
    ///
    /// 기준 화면에서는 이 버튼이 화면 **밖**(`SpaceDetailView` 의 떠 있는 ＋)에 있어서
    /// 달력이 알 필요가 없었습니다. 패미컴 스타일에서는 조작이 전부 몸통 위에 모이므로
    /// 달력이 그 버튼을 직접 그려야 하고, 그래서 받습니다.
    private let onAddPhoto: () -> Void

    public init(store: CalendarStore, onAddPhoto: @escaping () -> Void = {}) {
        self._store = State(initialValue: store)
        self.onAddPhoto = onAddPhoto
    }

    public var body: some View {
        // 격자는 몸통에 끼운 화면 안에 들어가고 조작은 화면 밖에 섭니다.
        //
        // 넘김 상태(기준 달·페이지·격자 폭)는 **여기 그대로 둡니다.**
        // 그리는 쪽으로 옮기면 화면이 다시 그려질 때마다 보던 달을 잃습니다.
        PlasticCalendarBody(
            store: store,
            onAddPhoto: onAddPhoto,
            anchor: $anchor,
            page: $page,
            gridWidth: $gridWidth,
            pageCount: Self.pageCount,
            pageCenter: Self.pageCenter,
            weekRows: Self.weekRows
        )
        .task { await store.refresh() }
    }

    /// 페이지 번호 → 달. 기준 달로부터 몇 칸 떨어졌는지로 셈합니다.
    private func month(at index: Int) -> (year: Int, month: Int) {
        let base = anchor ?? (store.state.year, store.state.month)
        let total = base.year * 12 + (base.month - 1) + (index - Self.pageCenter)
        return (total / 12, total % 12 + 1)
    }

}
