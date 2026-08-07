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

    /**
     옆으로 넘겨 달을 바꿉니다. 안드로이드의 `HorizontalPager` 와 같은 방식입니다.

     페이지 수를 아주 크게 잡고 가운데에서 시작합니다 — 무한히 넘기는 것처럼 보이게 하는
     흔한 방법입니다. **보이는 달만** 사진을 채워 그립니다. 옆 페이지는 상태에 없는 달이라
     사진을 모르는데, 빈 격자를 잠깐 보여 주는 편이 엉뚱한 달의 사진을 보여 주는 것보다 낫습니다.

     높이를 미리 정하는 이유: 달마다 줄 수가 달라 그때그때 재면 넘길 때 화면이 출렁입니다.
    @ViewBuilder
    /// 페이지 번호 → 달. 기준 달로부터 몇 칸 떨어졌는지로 셈합니다.
    private func month(at index: Int) -> (year: Int, month: Int) {
        let base = anchor ?? (store.state.year, store.state.month)
        let total = base.year * 12 + (base.month - 1) + (index - Self.pageCenter)
        return (total / 12, total % 12 + 1)
    }

    /**
     날짜 하나와 그날 사진들.

     고른 날은 **왼쪽에 레드 선**이 섭니다. 칸을 통째로 칠하거나 테두리를 두르면
     사진들이 상자에 갇힌 것처럼 보이는데, 선 하나면 "여기" 만 짚어 줍니다.
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
