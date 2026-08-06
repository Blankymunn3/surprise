import CoreModel
import DesignSystem
import Domain
import Foundation
import SwiftUI

/// 패미컴 스타일 달력(`CalendarPlastic.swift`)도 같은 요일 이름을 씁니다.
/// 스위프트의 `private` 은 **파일 안까지**라, 같은 모듈의 다른 파일에서 보려면 열어야 합니다.
let weekdays = ["일", "월", "화", "수", "목", "금", "토"]

/// 격자 칸 사이. 3 이면 칸끼리 붙지 않으면서도 달력이 한 덩어리로 보입니다.
private let cellGap: CGFloat = 3

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
        // 패미컴 스타일 시험 중에는 격자가 몸통에 끼운 화면 안으로 들어가고
        // 조작은 화면 밖에 섭니다. 스위치는 DesignSystem 에 하나뿐입니다.
        //
        // 넘김 상태(기준 달·페이지·격자 폭)는 **여기 그대로 둡니다.** 시험 화면으로
        // 옮기면 스위치를 껐다 켤 때 보던 달을 잃고, 두 벌을 따로 관리하게 됩니다.
        if plasticTrial {
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
        } else {
            standard
        }
    }

    private var standard: some View {
        // 격자와 '달력 접기' 는 **붙박이**고 아래 목록만 구릅니다. 목록을 내리는데
        // 달력까지 같이 밀려 올라가면, 지금 무슨 달을 보고 있는지가 사라집니다.
        VStack(alignment: .leading, spacing: 0) {
            header
            if !store.state.collapsed {
                weekdayRow
                grid
            }
            collapseBar
            MemoryColor.line2
                .frame(height: MemoryStroke.divider)
                .padding(.horizontal, MemorySpace.xl)

            let groups = store.state.visibleDays
            if groups.isEmpty {
                emptyScene
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(groups) { group in
                            daySection(group)
                        }
                    }
                    .padding(.horizontal, MemorySpace.xl)
                    .padding(.top, MemorySpace.m)
                    .padding(.bottom, 90)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(MemoryColor.paper)
        .task { await store.refresh() }
    }

    private var header: some View {
        HStack(spacing: MemorySpace.s) {
            // 연·월을 **한 덩어리**로 씁니다. 월만 크고 연도가 작으면 연도가 딸린
            // 주석처럼 보이는데, 지난 해를 넘겨 볼 때는 연도가 더 중요합니다.
            Text("\(String(store.state.year))년 \(store.state.month)월")
                .memoryTitle()
            Spacer()
            navButton("chevron.left", "이전 달") { store.move(by: -1) }
            navButton("chevron.right", "다음 달") { store.move(by: 1) }
        }
        .padding(.horizontal, MemorySpace.xl)
        .padding(.top, MemorySpace.s)
        .padding(.bottom, 10)
    }

    private func navButton(
        _ symbol: String, _ label: String, action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(MemoryColor.ink)
                .frame(width: 34, height: 34)
                .background(MemoryColor.surface)
                .overlay(Rectangle().strokeBorder(MemoryColor.line, lineWidth: MemoryStroke.border))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private var emptyScene: some View {
        VStack(alignment: .leading, spacing: 14) {
            PhotoFramesScene()
                .aspectRatio(PhotoFramesScene.ratio, contentMode: .fit)
                .frame(maxWidth: 150)
            Text("이 달엔 아직 사진이 없어요").memoryTitle()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(.horizontal, 28)
    }

    private var weekdayRow: some View {
        HStack(spacing: cellGap) {
            ForEach(weekdays.indices, id: \.self) { index in
                Text(weekdays[index])
                    .memoryMicro()
                    // 일요일만 딥레드입니다. 레드는 주 동작에 쓰는 색이라, 눌러야 할 것이
                    // 아닌 자리에는 한 단계 어두운 쪽을 씁니다.
                    .foregroundStyle(index == 0 ? MemoryColor.accentDeep : MemoryColor.ink2)
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
        .padding(.horizontal, MemorySpace.xl)
    }

    /**
     옆으로 넘겨 달을 바꿉니다. 안드로이드의 `HorizontalPager` 와 같은 방식입니다.

     페이지 수를 아주 크게 잡고 가운데에서 시작합니다 — 무한히 넘기는 것처럼 보이게 하는
     흔한 방법입니다. **보이는 달만** 사진을 채워 그립니다. 옆 페이지는 상태에 없는 달이라
     사진을 모르는데, 빈 격자를 잠깐 보여 주는 편이 엉뚱한 달의 사진을 보여 주는 것보다 낫습니다.

     높이를 미리 정하는 이유: 달마다 줄 수가 달라 그때그때 재면 넘길 때 화면이 출렁입니다.
     */
    private var grid: some View {
        let gap = cellGap
        let cell = gridWidth > 0 ? (gridWidth - gap * 6) / 7 : 0

        return TabView(selection: $page) {
            ForEach(0..<Self.pageCount, id: \.self) { index in
                monthGrid(at: index, cell: cell, gap: gap).tag(index)
            }
        }
        .modifier(PageStyle())
        .frame(height: cell * CGFloat(Self.weekRows) + gap * CGFloat(Self.weekRows - 1))
        .padding(.horizontal, MemorySpace.xl)
        .padding(.top, 6)
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
                            isToday: item.isToday, isSunday: item.isSunday,
                            isSelected: store.state.selected == date
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
        // 가운데가 아니라 **왼끝**입니다. 위의 연·월, 아래 날짜 묶음과 왼쪽 선이 맞아야
        // 세 덩어리가 한 화면으로 읽힙니다.
        Button { store.toggleCollapse() } label: {
            HStack(spacing: MemorySpace.xs) {
                Text(store.state.collapsed ? "달력 펴기" : "달력 접기")
                    .memoryMicro().foregroundStyle(MemoryColor.ink2)
                Image(systemName: store.state.collapsed ? "chevron.down" : "chevron.up")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(MemoryColor.ink2)
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
            .padding(.horizontal, MemorySpace.xl)
            .padding(.vertical, 7)
        }
        .buttonStyle(.plain)
    }

    /**
     날짜 하나와 그날 사진들.

     고른 날은 **왼쪽에 레드 선**이 섭니다. 칸을 통째로 칠하거나 테두리를 두르면
     사진들이 상자에 갇힌 것처럼 보이는데, 선 하나면 "여기" 만 짚어 줍니다.
     */
    private func daySection(_ group: DayGroup) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Rectangle()
                .fill(store.state.selected == group.date ? MemoryColor.accent : Color.clear)
                .frame(width: 3)

            VStack(alignment: .leading, spacing: 7) {
                HStack(alignment: .lastTextBaseline, spacing: MemorySpace.s) {
                    Text("\(group.date.month)월 \(group.date.day)일").memoryBody()
                    // 달력은 "언제" 를 보는 화면이지만, 그 사진이 어디였는지가 늘 따라옵니다.
                    Text([group.placeName, "\(group.photos.count)장"]
                        .compactMap { $0 }.joined(separator: " · "))
                        .memoryMicro()
                        .foregroundStyle(MemoryColor.ink2)
                }

                // 한 줄에 몇 장인지 정하지 않고 **칸 크기를 고정**해 흘려 담습니다.
                // 셋으로 나누면 폰이 넓어질수록 사진만 커지는데, 크기가 같아야 눈이 훑기 쉽습니다.
                FlowPhotos(photos: group.photos, coverId: group.coverId) { id in
                    Task { await store.setCover(id, on: group.date) }
                }
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(.bottom, MemorySpace.l)
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

/// 한 줄에 담기는 만큼 담고 넘치면 다음 줄로. `LazyVGrid` 는 열 수를 미리 정해야 해서
/// 칸 크기를 고정하려면 이렇게 직접 흘려 담아야 합니다.
private struct FlowPhotos: View {
    let photos: [Photo]
    let coverId: PhotoId?
    let onPick: (PhotoId) -> Void

    /// 시안이 정한 칸 크기.
    private static let side: CGFloat = 76
    private static let gap: CGFloat = 6

    var body: some View {
        FlowStack(gap: Self.gap) {
            ForEach(photos) { photo in
                PhotoThumb(url: photo.downloadURL, isCover: photo.id == coverId)
                    .frame(width: Self.side, height: Self.side)
                    .onTapGesture { onPick(photo.id) }
            }
        }
    }
}

/// 자식들을 왼쪽부터 놓고, 안 들어가면 다음 줄로 내리는 레이아웃.
/// 높이를 스스로 계산하므로 밖에서 줄 수를 셀 필요가 없습니다.
private struct FlowStack: Layout {
    let gap: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        arrange(in: proposal.width ?? .infinity, subviews: subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        for (index, at) in arrange(in: bounds.width, subviews: subviews).points.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + at.x, y: bounds.minY + at.y),
                proposal: .unspecified
            )
        }
    }

    private func arrange(in width: CGFloat, subviews: Subviews) -> (points: [CGPoint], size: CGSize) {
        var points: [CGPoint] = []
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0, widest: CGFloat = 0

        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width {
                x = 0
                y += rowHeight + gap
                rowHeight = 0
            }
            points.append(CGPoint(x: x, y: y))
            x += size.width + gap
            rowHeight = max(rowHeight, size.height)
            widest = max(widest, x - gap)
        }
        return (points, CGSize(width: widest, height: y + rowHeight))
    }
}
