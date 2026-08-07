import CoreModel
import DesignSystem
import Domain
import Foundation
import SwiftUI

/**
 **시험용 화면 — 패미컴 컨트롤러 스타일의 달력.**

 안드로이드 `CalendarPlastic.kt` 와 같은 짜임새입니다. 지도와도 같습니다 —
 몸통 위에 화면을 끼우고, 조작은 화면 밖입니다.

 다만 달력은 **화면 안이 두 층**입니다: 위는 격자, 아래는 그날 사진들.
 두 층 사이에 홈을 파서 나눕니다 (선을 긋지 않습니다 — 이 판에서는 선보다 홈입니다).

 **칸마다 베벨을 주지 않습니다.** 42칸에 전부 두르면 화면이 자글자글해져서
 정작 사진이 안 보입니다. 베벨은 기기와 화면의 경계에만 둡니다.
 */
struct PlasticCalendarBody: View {
    let store: CalendarStore
    let onAddPhoto: () -> Void

    @Binding var anchor: (year: Int, month: Int)?
    @Binding var page: Int
    @Binding var gridWidth: CGFloat

    let pageCount: Int
    let pageCenter: Int
    let weekRows: Int

    var body: some View {
        VStack(spacing: 0) {
            monthBar

            screen
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .sunken(PlasticRadius.screen)

            bottom
        }
        .padding(.horizontal, MemorySpace.s)
        .background(PlasticColor.body)
    }

    private var screen: some View {
        VStack(alignment: .leading, spacing: 0) {
            if !store.state.collapsed {
                weekdayStrip
                Spacer().frame(height: MemorySpace.xs)
                grid

                // 격자와 목록 사이의 홈. 판을 파낸 자국이라 두 층이 갈립니다.
                RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous)
                    .fill(PlasticColor.plateLo)
                    .frame(height: 2)
                    .padding(.vertical, MemorySpace.s)
            }

            let groups = store.state.visibleDays
            if groups.isEmpty {
                emptyPlate
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(groups) { group in
                            daySection(group)
                        }
                    }
                    .padding(.bottom, MemorySpace.m)
                }
            }
        }
        .padding(MemorySpace.s)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    // MARK: 몸통 위 — 연·월과 달 넘김

    /**
     화살표 두 개는 **몸통에 앉힌 작은 버튼**입니다. 컨트롤러의 SELECT·START 자리에
     있던 그 모양인데, 여기서는 무엇을 하는지가 화살표로 드러나므로 라벨이 필요 없습니다
     (목록 화면에서 SELECT·START 글자를 뺀 것과 같은 이유입니다).
     */
    private var monthBar: some View {
        HStack(spacing: MemorySpace.s) {
            Text("\(String(store.state.year))년 \(store.state.month)월")
                .font(MemoryFont.font(17, .bold))
                .foregroundStyle(PlasticColor.ink)
            Spacer(minLength: 0)
            monthNav("‹", "이전 달") { store.move(by: -1) }
            monthNav("›", "다음 달") { store.move(by: 1) }
        }
        .padding(.horizontal, MemorySpace.xs)
        .padding(.vertical, MemorySpace.s)
    }

    private func monthNav(_ glyph: String, _ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(glyph)
                .font(MemoryFont.font(17, .bold))
                .foregroundStyle(PlasticColor.onRubber)
                .frame(width: PlasticSize.monthNav, height: PlasticSize.monthNav)
                .background(
                    RoundedRectangle(cornerRadius: PlasticRadius.knob, style: .continuous)
                        .fill(PlasticColor.rubber)
                )
        }
        .buttonStyle(.plasticPress)
        .accessibilityLabel(label)
    }

    // MARK: 화면 안 — 격자

    private var weekdayStrip: some View {
        HStack(spacing: 0) {
            ForEach(weekdays.indices, id: \.self) { index in
                Text(weekdays[index])
                    .font(MemoryFont.font(11, .bold))
                    // 일요일만 빨강입니다. 이 판에서 빨강은 주 동작 색이 아니라
                    // 몸통 위에서만 그렇고, 검정 화면 안에서는 그냥 잘 보이는 색입니다.
                    .foregroundStyle(index == 0 ? PlasticColor.redHi : PlasticColor.onPlateDim)
                    .frame(maxWidth: .infinity)
            }
        }
        // ⚠️ 격자 폭은 **여백을 주기 전에** 재야 합니다. 기준 화면에서 겪은 것과 같은
        // 함정입니다 — 여백 뒤에 재면 그만큼 넓게 잡혀 첫 칸이 잘려 나갑니다.
        .background(
            GeometryReader { proxy in
                Color.clear
                    .onAppear { gridWidth = proxy.size.width }
                    .onChange(of: proxy.size.width) { _, value in gridWidth = value }
            }
        )
    }

    /**
     넘기는 방식은 기준 화면(`CalendarView.grid`)의 것과 같습니다 —
     페이지를 아주 많이 잡고 가운데에서 시작해 무한히 넘기는 것처럼 보이게 합니다.

     **칸 사이를 띄우지 않습니다.** 띄우면 42개가 흩어져 보이는데,
     붙이면 판에 새긴 격자 하나로 읽힙니다. 칸끼리는 밝기로 갈립니다.
     */
    private var grid: some View {
        let cell = gridWidth > 0 ? max(gridWidth / 7, PlasticSize.dayCell) : 0

        return TabView(selection: $page) {
            ForEach(0..<pageCount, id: \.self) { index in
                monthGrid(at: index, cell: cell).tag(index)
            }
        }
        .modifier(PlasticPageStyle())
        .frame(height: cell * CGFloat(weekRows))
        .onAppear {
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
    private func monthGrid(at index: Int, cell: CGFloat) -> some View {
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
            LazyVGrid(
                columns: Array(repeating: GridItem(.flexible(minimum: 0), spacing: 0), count: 7),
                spacing: 0
            ) {
                ForEach(cells) { item in
                    if let date = item.date {
                        PlasticDayCell(
                            item: item,
                            date: date,
                            isSelected: store.state.selected == date
                        )
                        .frame(height: cell)
                        .onTapGesture { store.select(date) }
                    } else {
                        Color.clear.frame(height: cell)
                    }
                }
            }
            Spacer(minLength: 0)
        }
    }

    /// 페이지 번호 → 달. 기준 화면과 같은 셈입니다.
    private func month(at index: Int) -> (year: Int, month: Int) {
        let base = anchor ?? (store.state.year, store.state.month)
        let total = base.year * 12 + (base.month - 1) + (index - pageCenter)
        return (total / 12, total % 12 + 1)
    }

    private func syncPage() {
        guard let base = anchor else { return }
        let diff = (store.state.year - base.year) * 12 + (store.state.month - base.month)
        let target = pageCenter + diff
        if target != page, (0..<pageCount).contains(target) {
            withAnimation { page = target }
        }
    }

    // MARK: 화면 안 — 그날 사진들

    /**
     고른 날은 **왼쪽에 빨간 선**이 섭니다 — 기준 디자인과 같은 방식입니다.
     칸을 통째로 칠하면 사진들이 상자에 갇힌 것처럼 보입니다.
     */
    private func daySection(_ group: DayGroup) -> some View {
        HStack(alignment: .top, spacing: MemorySpace.s) {
            Rectangle()
                .fill(store.state.selected == group.date ? PlasticColor.red : Color.clear)
                .frame(width: 3)

            VStack(alignment: .leading, spacing: MemorySpace.xs) {
                HStack(alignment: .lastTextBaseline, spacing: MemorySpace.s) {
                    Text("\(group.date.month)월 \(group.date.day)일")
                        .font(MemoryFont.font(15, .bold))
                        .foregroundStyle(PlasticColor.onPlate)
                    Text([group.placeName, "\(group.photos.count)장"]
                        .compactMap { $0 }.joined(separator: " · "))
                        .font(MemoryFont.font(11, .semibold))
                        .foregroundStyle(PlasticColor.onPlateDim)
                }

                PlasticFlowPhotos(photos: group.photos, coverId: group.coverId) { id in
                    Task { await store.setCover(id, on: group.date) }
                }
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(.bottom, MemorySpace.m)
    }

    private var emptyPlate: some View {
        VStack(alignment: .leading, spacing: 0) {
            PhotoFramesScene()
                .aspectRatio(PhotoFramesScene.ratio, contentMode: .fit)
                .frame(maxWidth: 130)
            Text("이 달엔 아직 사진이 없어요")
                .font(MemoryFont.font(17, .bold))
                .foregroundStyle(PlasticColor.onPlate)
                .padding(.top, MemorySpace.m)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(.horizontal, MemorySpace.l)
    }

    // MARK: 몸통 위 — 조작부

    /**
     왼쪽 접기(고무 알약), 오른쪽 사진 올리기(빨간 A 버튼).

     지도의 조작부와 **오른쪽이 같습니다.** 두 탭에서 빨간 버튼이 같은 자리에서
     같은 일을 해야 손이 기억합니다.
     */
    private var bottom: some View {
        HStack(spacing: MemorySpace.m) {
            Button { store.toggleCollapse() } label: {
                Text(store.state.collapsed ? "달력 펴기" : "달력 접기")
                    .font(MemoryFont.font(15, .bold))
                    .foregroundStyle(PlasticColor.onRubber)
                    .frame(maxWidth: .infinity)
                    .frame(height: PlasticSize.button)
                    .background(Capsule().fill(PlasticColor.rubber))
            }
            .buttonStyle(.plasticPress)
            .padding(PlasticSize.buttonInset)
            .raisedPlastic()

            Button(action: onAddPhoto) {
                Text("＋")
                    .font(MemoryFont.font(24, .bold))
                    .foregroundStyle(PlasticColor.onRed)
                    .frame(width: PlasticSize.button, height: PlasticSize.button)
                    .background(Circle().fill(PlasticColor.red))
            }
            .buttonStyle(.plasticPress)
            .accessibilityLabel("사진 올리기")
            .padding(PlasticSize.buttonInset)
            .raisedPlastic()
        }
        .padding(.horizontal, MemorySpace.xs)
        .padding(.vertical, MemorySpace.m)
    }
}

/**
 달력 한 칸.

 **테두리를 두르지 않습니다.** 42칸에 선을 두르면 격자가 아니라 그물이 됩니다.
 대신 세 가지로 가릅니다:
 - 사진 있는 날 → 사진이 칸을 채움
 - 오늘 → 숫자만 빨강
 - 고른 날 → 칸 바닥이 밝아짐 (눌러서 들어간 자리처럼)
 */
private struct PlasticDayCell: View {
    let item: DayCellUi
    let date: CalendarDate
    let isSelected: Bool

    var body: some View {
        ZStack(alignment: .topLeading) {
            photo
            Text("\(date.day)")
                .font(MemoryFont.font(12.5, .bold))
                .foregroundStyle(numberColor)
                .padding(.horizontal, 4)
                .padding(.vertical, 2)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(isSelected ? PlasticColor.plateHi : PlasticColor.plateLo)
        .clipShape(RoundedRectangle(cornerRadius: PlasticRadius.chip, style: .continuous))
        .padding(1)
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private var photo: some View {
        if let cover = item.coverURL, let url = URL(string: cover) {
            RemotePhoto(url: url) { Color.clear }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private var numberColor: Color {
        if item.isToday { return PlasticColor.redHi }
        if item.isSunday { return PlasticColor.redLo }
        if item.coverURL != nil { return PlasticColor.onRed }
        return PlasticColor.onPlateDim
    }
}

/// 한 줄에 담기는 만큼 담고 넘치면 다음 줄로. 기준 화면의 `FlowPhotos` 와 같고
/// 칸 크기만 이 스타일의 것입니다 — 격자와 목록이 한 화면에 같이 있어 더 작습니다.
private struct PlasticFlowPhotos: View {
    let photos: [Photo]
    let coverId: PhotoId?
    let onPick: (PhotoId) -> Void

    var body: some View {
        PlasticFlowStack(gap: MemorySpace.xs) {
            ForEach(photos) { photo in
                PhotoThumb(url: photo.downloadURL, isCover: photo.id == coverId)
                    .frame(width: PlasticSize.calendarPhoto, height: PlasticSize.calendarPhoto)
                    .onTapGesture { onPick(photo.id) }
            }
        }
    }
}

/// 자식들을 왼쪽부터 놓고, 안 들어가면 다음 줄로 내리는 레이아웃.
/// 기준 화면의 `FlowStack` 과 같은 코드입니다 — 그쪽이 `private` 이라 여기 한 벌 둡니다.
/// 채택되면 둘을 합쳐 DesignSystem 으로 올리는 것이 맞습니다.
private struct PlasticFlowStack: Layout {
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

/// 페이지처럼 넘기는 `TabView` 스타일은 **iOS 에만** 있습니다.
/// 패키지가 맥에서도 빌드돼야 `swift test` 가 돌기 때문에 여기서 갈라 둡니다.
private struct PlasticPageStyle: ViewModifier {
    func body(content: Content) -> some View {
        #if os(iOS)
        content.tabViewStyle(.page(indexDisplayMode: .never))
        #else
        content
        #endif
    }
}
