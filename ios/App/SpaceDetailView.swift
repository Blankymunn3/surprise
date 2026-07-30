import CoreModel
import DesignSystem
import FeatureCalendar
import SwiftUI

/// 공간 안. 위쪽 가운데의 `지도 | 달력` 으로 오갑니다 (`docs/app/SCREENS.md`).
struct SpaceDetailView: View {
    let spaceId: SpaceId

    @Environment(\.dismiss) private var dismiss
    @State private var tab: Tab = .map
    @State private var calendar: CalendarStore

    enum Tab: String, CaseIterable { case map = "지도", calendar = "달력" }

    init(spaceId: SpaceId) {
        self.spaceId = spaceId
        _calendar = State(initialValue: AppContainer.shared.calendarStore(spaceId))
    }

    var body: some View {
        ZStack(alignment: .top) {
            MemoryColor.paper.ignoresSafeArea()

            Group {
                switch tab {
                case .map: MapTabPlaceholder()
                case .calendar: CalendarView(store: calendar)
                }
            }
            .padding(.top, 52)

            segmented
        }
        .navigationBarBackButtonHidden()
        .toolbar(.hidden, for: .navigationBar)
        .safeAreaInset(edge: .top, spacing: 0) { Color.clear.frame(height: 0) }
        .overlay(alignment: .topLeading) { backButton }
    }

    private var segmented: some View {
        HStack(spacing: 2) {
            ForEach(Tab.allCases, id: \.self) { item in
                Text(item.rawValue)
                    .memoryHeadline()
                    .foregroundStyle(tab == item ? MemoryColor.ink : MemoryColor.ink2)
                    .padding(.horizontal, MemorySpace.xl)
                    .padding(.vertical, MemorySpace.s)
                    .background {
                        if tab == item {
                            Capsule().fill(MemoryColor.surface)
                                .shadow(color: .black.opacity(0.06), radius: 6, y: 2)
                        }
                    }
                    .contentShape(Capsule())
                    .onTapGesture { tab = item }
            }
        }
        .padding(3)
        .background(Capsule().fill(MemoryColor.fill))
        .padding(.top, MemorySpace.s)
    }

    private var backButton: some View {
        Button { dismiss() } label: {
            Image(systemName: "arrow.left")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(MemoryColor.ink)
                .frame(width: 40, height: 40)
                .background(Circle().fill(MemoryColor.surface))
                .shadow(color: .black.opacity(0.06), radius: 6, y: 2)
        }
        .padding(.leading, MemorySpace.l)
        .padding(.top, MemorySpace.s)
    }
}

/// 지도는 아직 **그리지 않습니다.**
///
/// 안드로이드는 MapLibre 로 기본 지도를 띄우는데, iOS 쪽은 지도 SDK 를 아직 붙이지
/// 않았습니다. 없는 것을 있는 척하는 화면보다, 무엇이 없는지 적어 두는 편이 낫습니다.
/// 진행 상황은 `docs/app/STATUS.md`.
private struct MapTabPlaceholder: View {
    var body: some View {
        VStack(spacing: MemorySpace.m) {
            Spacer()
            Image(systemName: "map")
                .font(.system(size: 40, weight: .light))
                .foregroundStyle(MemoryColor.ink3)
            Text("지도는 아직 준비 중이에요").memoryHeadline()
            Text("달력 탭에서 사진을 볼 수 있어요")
                .memoryBody()
                .foregroundStyle(MemoryColor.ink2)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}
