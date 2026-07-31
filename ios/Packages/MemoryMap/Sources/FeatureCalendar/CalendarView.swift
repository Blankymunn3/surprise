import CoreModel
import DesignSystem
import Domain
import Foundation
import SwiftUI

private let weekdays = ["일", "월", "화", "수", "목", "금", "토"]

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

    public init(store: CalendarStore) {
        self._store = State(initialValue: store)
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                if !store.state.collapsed {
                    weekdayRow
                    grid
                }
                collapseBar
                ForEach(store.state.visibleDays) { group in
                    daySection(group)
                }
            }
            .padding(.bottom, 110)
        }
        .background(MemoryColor.paper)
        .task { await store.refresh() }
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: MemorySpace.s) {
            // "3월" 크게, "2026" 작게 — 대부분 올해를 보고 연도는 확인용입니다
            Text("\(store.state.month)월").memoryDisplay()
            Text(String(store.state.year)).memoryBody().foregroundStyle(MemoryColor.ink3)
            Spacer()
            Button { store.move(by: -1) } label: {
                Image(systemName: "chevron.left").foregroundStyle(MemoryColor.ink2)
            }
            .accessibilityLabel("이전 달")
            Button { store.move(by: 1) } label: {
                Image(systemName: "chevron.right").foregroundStyle(MemoryColor.ink2)
            }
            .accessibilityLabel("다음 달")
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 22)
        .padding(.top, MemorySpace.l)
    }

    private var weekdayRow: some View {
        HStack(spacing: MemorySpace.xs) {
            ForEach(weekdays.indices, id: \.self) { index in
                Text(weekdays[index])
                    .memoryMicro()
                    .foregroundStyle(index == 0 ? MemoryColor.accent : MemoryColor.ink3)
                    .frame(maxWidth: .infinity)
            }
        }
        // 격자 칸 크기를 여기서 잽니다 — 같은 여백을 쓰므로 폭이 같습니다.
        //
        // ⚠️ **여백을 주기 전에** 재야 합니다. `.padding()` 뒤에 `.background()` 를 붙이면
        // 여백까지 포함한 바깥 폭이 잡혀서 칸이 36pt 만큼 넓어지고, 그만큼 격자가 넘쳐
        // 첫 칸(일요일)이 잘려 나갑니다.
        .background(
            GeometryReader { proxy in
                Color.clear
                    .onAppear { gridWidth = proxy.size.width }
                    .onChange(of: proxy.size.width) { _, value in gridWidth = value }
            }
        )
        .padding(.horizontal, 18)
        .padding(.vertical, MemorySpace.s)
    }

    /**
     옆으로 넘겨 달을 바꿉니다. 안드로이드의 `HorizontalPager` 와 같은 방식입니다.

     페이지 수를 아주 크게 잡고 가운데에서 시작합니다 — 무한히 넘기는 것처럼 보이게 하는
     흔한 방법입니다. **보이는 달만** 사진을 채워 그립니다. 옆 페이지는 상태에 없는 달이라
     사진을 모르는데, 빈 격자를 잠깐 보여 주는 편이 엉뚱한 달의 사진을 보여 주는 것보다 낫습니다.

     높이를 미리 정하는 이유: 달마다 줄 수가 달라 그때그때 재면 넘길 때 화면이 출렁입니다.
     */
    private var grid: some View {
        let gap = MemorySpace.xs
        let cell = gridWidth > 0 ? (gridWidth - gap * 6) / 7 : 0

        return TabView(selection: $page) {
            ForEach(0..<Self.pageCount, id: \.self) { index in
                monthGrid(at: index, cell: cell, gap: gap).tag(index)
            }
        }
        .modifier(PageStyle())
        .frame(height: cell * CGFloat(Self.weekRows) + gap * CGFloat(Self.weekRows - 1))
        .padding(.horizontal, 18)
        .onAppear {
            // 기준 달은 한 번만 잡습니다. 매번 다시 잡으면 넘긴 자리가 흔들립니다.
            if anchor == nil { anchor = (store.state.year, store.state.month) }
        }
        .onChange(of: page) { _, value in
            let target = month(at: value)
            store.setMonth(year: target.year, month: target.month)
        }
        .onChange(of: store.state.month) { _, _ in syncPage() }
        .onChange(of: store.state.year) { _, _ in syncPage() }
    }

    @ViewBuilder
    private func monthGrid(at index: Int, cell: CGFloat, gap: CGFloat) -> some View {
        let target = month(at: index)
        let isCurrent = target.year == store.state.year && target.month == store.state.month
        let cells: [DayCellUi] = isCurrent
            ? store.state.cells
            : CalendarMonth.grid(year: target.year, month: target.month)
                .enumerated()
                .map { slot, date in
                    DayCellUi(
                        date: date, coverURL: nil, isToday: false,
                        isSunday: date.map(CalendarMonth.isSunday) ?? false, slot: slot
                    )
                }

        VStack(spacing: 0) {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(minimum: 0), spacing: gap), count: 7),
                      spacing: gap) {
                ForEach(cells) { item in
                    if let date = item.date {
                        DayCell(
                            day: date.day, photoURL: item.coverURL,
                            isToday: item.isToday, isSunday: item.isSunday
                        )
                        .onTapGesture { store.select(date) }
                    } else {
                        Color.clear.frame(height: cell)
                    }
                }
            }
            Spacer(minLength: 0)
        }
    }

    /// 페이지 번호 → 달. 기준 달로부터 몇 칸 떨어졌는지로 셈합니다.
    private func month(at index: Int) -> (year: Int, month: Int) {
        let base = anchor ?? (store.state.year, store.state.month)
        let total = base.year * 12 + (base.month - 1) + (index - Self.pageCenter)
        return (total / 12, total % 12 + 1)
    }

    /// 위 화살표로 달을 바꿨을 때 페이지도 따라오게.
    private func syncPage() {
        guard let base = anchor else { return }
        let diff = (store.state.year - base.year) * 12 + (store.state.month - base.month)
        let target = Self.pageCenter + diff
        if target != page, (0..<Self.pageCount).contains(target) {
            withAnimation { page = target }
        }
    }

    private var collapseBar: some View {
        Button { store.toggleCollapse() } label: {
            HStack(spacing: 6) {
                Text(store.state.collapsed ? "달력 펼치기" : "달력 접기")
                    .memoryLabel().foregroundStyle(MemoryColor.ink2)
                Image(systemName: store.state.collapsed ? "chevron.down" : "chevron.up")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(MemoryColor.ink2)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
        }
        .buttonStyle(.plain)
        .overlay(alignment: .top) { Rectangle().fill(MemoryColor.line).frame(height: 1) }
        .padding(.horizontal, 18)
        .padding(.top, MemorySpace.l)
    }

    private func daySection(_ group: DayGroup) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: MemorySpace.s) {
                Text("\(group.date.month)월 \(group.date.day)일").memoryHeadline()
                Spacer()
                // 달력은 "언제" 를 보는 화면이지만 그 사진이 어디였는지가 늘 따라옵니다
                if let place = group.placeName {
                    Text(place)
                        .memoryMicro()
                        .foregroundStyle(MemoryColor.accent)
                        .padding(.horizontal, 11).padding(.vertical, 4)
                        .background(Capsule().fill(MemoryColor.accentTint))
                }
            }

            // 한 줄에 3장 — 4장을 넣으면 썸네일이 작아져 얼굴이 안 보입니다
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: MemorySpace.xs), count: 3),
                      spacing: MemorySpace.xs) {
                ForEach(group.photos) { photo in
                    PhotoThumb(url: photo.downloadURL, isCover: photo.id == group.coverId)
                        .onLongPressGesture {
                            Task { await store.setCover(photo.id, on: group.date) }
                        }
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, MemorySpace.s)
    }
}

/// 페이지처럼 넘기는 `TabView` 스타일은 **iOS 에만** 있습니다.
/// 패키지가 맥에서도 빌드돼야 `swift test` 가 돌기 때문에 여기서 갈라 둡니다.
private struct PageStyle: ViewModifier {
    func body(content: Content) -> some View {
        #if os(iOS)
        content.tabViewStyle(.page(indexDisplayMode: .never))
        #else
        content
        #endif
    }
}
